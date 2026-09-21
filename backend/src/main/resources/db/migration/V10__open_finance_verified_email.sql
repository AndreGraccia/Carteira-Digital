ALTER TABLE open_finance_email_challenge
    ADD COLUMN verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN verified_email VARCHAR(50);

-- Preserve existing imports while isolating the provider transaction key per user.
UPDATE transaction t
SET external_id = w.user_id::text || ':' || t.external_id
FROM wallet w
WHERE t.from_wallet_id = w.id AND t.source = 'OPEN_FINANCE' AND t.external_id IS NOT NULL;
