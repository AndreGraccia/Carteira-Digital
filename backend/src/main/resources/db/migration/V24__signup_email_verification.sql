-- A verificação de e-mail passa a ser da conta, no cadastro, e não mais da conexão Open Finance (ADR 0008).
-- Antes, o código por e-mail fazia papel de consentimento: expirava em 90 dias, derrubava a conexão ao pedir
-- um código novo e deixava sem alerta por e-mail quem não usava Open Finance.

ALTER TABLE public."user" ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;

-- Contas que já existiam não ficam presas fora do app: quem verificou pelo fluxo antigo mantém a data;
-- as demais são consideradas verificadas na migração (eram contas de desenvolvimento, sem e-mail real).
UPDATE public."user" u
SET email_verified_at = COALESCE(
        (SELECT c.verified_at FROM open_finance_email_challenge c
         WHERE c.user_id = u.id AND c.verified_email = u.email),
        now());

ALTER TABLE open_finance_email_challenge RENAME TO email_verification_challenge;
ALTER SEQUENCE open_finance_email_challenge_seq RENAME TO email_verification_challenge_seq;
ALTER TABLE email_verification_challenge
    RENAME CONSTRAINT pk_open_finance_email_challenge TO pk_email_verification_challenge;
ALTER TABLE email_verification_challenge
    RENAME CONSTRAINT fk_open_finance_email_challenge_user TO fk_email_verification_challenge_user;
ALTER TABLE email_verification_challenge
    RENAME CONSTRAINT open_finance_email_challenge_user_key TO email_verification_challenge_user_key;
ALTER TABLE email_verification_challenge
    RENAME CONSTRAINT open_finance_email_challenge_attempts_check TO email_verification_challenge_attempts_check;
ALTER TABLE email_verification_challenge
    RENAME CONSTRAINT open_finance_email_challenge_send_count_check TO email_verification_challenge_send_count_check;
