CREATE SEQUENCE IF NOT EXISTS open_finance_connection_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE wallet
    ADD COLUMN source VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN institution_name VARCHAR(100),
    ADD COLUMN external_account_id VARCHAR(100),
    ADD COLUMN last_synced_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE wallet DROP CONSTRAINT IF EXISTS wallet_nonnegative_balance;

CREATE UNIQUE INDEX wallet_open_finance_account_key
    ON wallet (user_id, source, external_account_id)
    WHERE external_account_id IS NOT NULL;

ALTER TABLE transaction
    ALTER COLUMN description TYPE VARCHAR(200),
    ADD COLUMN source VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN external_id VARCHAR(255);

CREATE UNIQUE INDEX transaction_external_id_key
    ON transaction (external_id)
    WHERE external_id IS NOT NULL;

CREATE TABLE open_finance_connection
(
    id               BIGINT                   NOT NULL,
    user_id          BIGINT                   NOT NULL,
    institution_id   VARCHAR(40)              NOT NULL,
    institution_name VARCHAR(100)             NOT NULL,
    consent_id       VARCHAR(100)             NOT NULL,
    status           VARCHAR(20)              NOT NULL,
    authorised_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at       TIMESTAMP WITH TIME ZONE,
    last_synced_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_open_finance_connection PRIMARY KEY (id),
    CONSTRAINT fk_open_finance_connection_user FOREIGN KEY (user_id) REFERENCES public."user" (id),
    CONSTRAINT open_finance_user_institution_key UNIQUE (user_id, institution_id)
);

COMMENT ON COLUMN wallet.external_account_id IS
    'Identificador opaco de conta recebido da API de Contas do Open Finance; nunca deve ser exibido como credencial.';

COMMENT ON COLUMN transaction.external_id IS
    'Chave idempotente composta pela instituicao, conta e transactionId recebidos do Open Finance.';
