-- Daily cap on verification e-mails per account. Without it, an account registered with someone
-- else's address could send that person a code every 60 seconds (about 1,440 e-mails a day).
ALTER TABLE open_finance_email_challenge
    ADD COLUMN send_count             SMALLINT                 NOT NULL DEFAULT 0,
    ADD COLUMN send_window_started_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE open_finance_email_challenge
    ADD CONSTRAINT open_finance_email_challenge_send_count_check CHECK (send_count >= 0);

COMMENT ON COLUMN open_finance_email_challenge.send_count IS
    'Codigos enviados na janela de 24 horas iniciada em send_window_started_at.';
