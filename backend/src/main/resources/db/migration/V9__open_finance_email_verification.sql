CREATE SEQUENCE IF NOT EXISTS open_finance_email_challenge_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE open_finance_email_challenge
(
    id                  BIGINT                   NOT NULL,
    user_id             BIGINT                   NOT NULL,
    code_hash           VARCHAR(100)             NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    resend_available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count       SMALLINT                 NOT NULL DEFAULT 0,
    max_attempts        SMALLINT                 NOT NULL,
    consumed_at         TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_open_finance_email_challenge PRIMARY KEY (id),
    CONSTRAINT fk_open_finance_email_challenge_user FOREIGN KEY (user_id) REFERENCES public."user" (id),
    CONSTRAINT open_finance_email_challenge_user_key UNIQUE (user_id),
    CONSTRAINT open_finance_email_challenge_attempts_check
        CHECK (attempt_count >= 0 AND max_attempts > 0 AND attempt_count <= max_attempts)
);

COMMENT ON COLUMN open_finance_email_challenge.code_hash IS
    'Hash BCrypt do codigo temporario; o codigo em texto puro nunca e persistido.';
