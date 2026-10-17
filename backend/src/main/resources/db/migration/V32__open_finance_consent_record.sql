-- Consentimento como registro próprio (B42). A conexão guarda só o estado atual e era sobrescrita a cada novo
-- consentimento: a prova do anterior se perdia. Aqui cada consentimento tem a própria linha, as permissões ficam
-- numa tabela (não mais em texto separado por vírgula) e cada mudança de estado vira um evento que só é acrescentado.

CREATE TABLE open_finance_consent
(
    consent_id     VARCHAR(100)             NOT NULL,
    user_id        BIGINT                   NOT NULL,
    institution_id VARCHAR(40)              NOT NULL,
    purpose        VARCHAR(40)              NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE,
    -- Versão e idioma do texto que a pessoa leu ao autorizar; nulos até a autorização.
    terms_version  VARCHAR(20),
    terms_language VARCHAR(5),
    CONSTRAINT pk_open_finance_consent PRIMARY KEY (consent_id),
    CONSTRAINT fk_open_finance_consent_user FOREIGN KEY (user_id) REFERENCES public."user" (id)
);

CREATE INDEX idx_open_finance_consent_user ON open_finance_consent (user_id);

CREATE TABLE open_finance_consent_permission
(
    consent_id VARCHAR(100) NOT NULL,
    permission VARCHAR(60)  NOT NULL,
    CONSTRAINT pk_open_finance_consent_permission PRIMARY KEY (consent_id, permission),
    CONSTRAINT fk_open_finance_consent_permission FOREIGN KEY (consent_id)
        REFERENCES open_finance_consent (consent_id) ON DELETE CASCADE
);

CREATE TABLE open_finance_consent_event
(
    id          BIGSERIAL                NOT NULL,
    consent_id  VARCHAR(100)             NOT NULL,
    event       VARCHAR(20)              NOT NULL,
    actor       VARCHAR(10),
    reason      VARCHAR(40),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_open_finance_consent_event PRIMARY KEY (id),
    CONSTRAINT fk_open_finance_consent_event FOREIGN KEY (consent_id)
        REFERENCES open_finance_consent (consent_id) ON DELETE CASCADE,
    CONSTRAINT open_finance_consent_event_check CHECK (event IN ('CREATED', 'AUTHORISED', 'REJECTED')),
    CONSTRAINT open_finance_consent_event_reason_check CHECK ((event = 'REJECTED') = (reason IS NOT NULL))
);

CREATE INDEX idx_open_finance_consent_event_consent ON open_finance_consent_event (consent_id);

-- O que existe hoje: um consentimento por conexão (os anteriores já foram sobrescritos e não voltam).
INSERT INTO open_finance_consent (consent_id, user_id, institution_id, purpose, created_at, expires_at)
SELECT consent_id, user_id, institution_id, 'FUTURE_PROJECTION', created_at, expires_at
FROM open_finance_connection;

INSERT INTO open_finance_consent_permission (consent_id, permission)
SELECT DISTINCT consent_id, trim(permission)
FROM open_finance_connection, regexp_split_to_table(permissions, ',') AS permission
WHERE trim(permission) <> '';

INSERT INTO open_finance_consent_event (consent_id, event, occurred_at)
SELECT consent_id, 'CREATED', created_at FROM open_finance_connection;

INSERT INTO open_finance_consent_event (consent_id, event, occurred_at)
SELECT consent_id, 'AUTHORISED', authorised_at FROM open_finance_connection WHERE authorised_at IS NOT NULL;

INSERT INTO open_finance_consent_event (consent_id, event, actor, reason, occurred_at)
SELECT consent_id, 'REJECTED', rejected_by, rejection_reason, COALESCE(rejected_at, created_at)
FROM open_finance_connection WHERE status = 'REJECTED';

-- A coluna antiga deixa de ser escrita. Apagá-la é outra migração, depois da aprovação do dono.
ALTER TABLE open_finance_connection ALTER COLUMN permissions DROP NOT NULL;
COMMENT ON COLUMN open_finance_connection.permissions IS
    'Obsoleta desde a V32: as permissões ficam em open_finance_consent_permission.';

COMMENT ON TABLE open_finance_consent IS
    'Um registro por consentimento do Open Finance, nunca sobrescrito: finalidade, prazo e o texto que a pessoa leu (LGPD art. 8º).';
COMMENT ON TABLE open_finance_consent_event IS
    'Histórico do consentimento, só acrescentado: criado, autorizado e rejeitado (por quem e por quê).';
