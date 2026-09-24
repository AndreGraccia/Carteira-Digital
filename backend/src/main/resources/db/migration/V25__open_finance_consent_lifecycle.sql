-- Consentimento no ciclo do Open Finance Brasil (API Consents v3): AWAITING_AUTHORISATION -> AUTHORISED | REJECTED,
-- com quem rejeitou e o motivo. Revogar é REJECTED com CUSTOMER_MANUALLY_REVOKED; não existe status REVOKED.
-- A conexão passa a nascer antes da autorização, então as datas de autorização e sync ficam opcionais.

ALTER TABLE open_finance_connection
    ALTER COLUMN status TYPE VARCHAR(30),
    ALTER COLUMN authorised_at DROP NOT NULL,
    ALTER COLUMN last_synced_at DROP NOT NULL,
    ADD COLUMN created_at       TIMESTAMP WITH TIME ZONE,
    ADD COLUMN permissions      VARCHAR(600),
    ADD COLUMN rejected_by      VARCHAR(10),
    ADD COLUMN rejection_reason VARCHAR(40),
    ADD COLUMN rejected_at      TIMESTAMP WITH TIME ZONE;

UPDATE open_finance_connection
SET created_at  = COALESCE(authorised_at, last_synced_at, now()),
    permissions = 'ACCOUNTS_READ,ACCOUNTS_BALANCES_READ,ACCOUNTS_TRANSACTIONS_READ,RESOURCES_READ';

UPDATE open_finance_connection
SET status = 'REJECTED', rejected_by = 'USER', rejection_reason = 'CUSTOMER_MANUALLY_REVOKED', rejected_at = now()
WHERE status = 'REVOKED';

ALTER TABLE open_finance_connection
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN permissions SET NOT NULL,
    ADD CONSTRAINT open_finance_connection_status_check
        CHECK (status IN ('AWAITING_AUTHORISATION', 'AUTHORISED', 'REJECTED')),
    ADD CONSTRAINT open_finance_connection_rejection_check
        CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL));

COMMENT ON COLUMN open_finance_connection.permissions IS
    'Permissões pedidas no consentimento, separadas por vírgula: a finalidade fica gravada junto com o dado (LGPD art. 8º).';
