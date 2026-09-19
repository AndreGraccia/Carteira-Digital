INSERT INTO public.type (id, name, description) VALUES
    (4, 'Deposit', 'Simulated deposit'),
    (5, 'Withdrawal', 'Simulated withdrawal'),
    (6, 'Initial balance', 'Initial simulated balance');

SELECT setval(
    'type_seq',
    (SELECT MAX(id) FROM public.type)
);

ALTER TABLE transaction
    ADD COLUMN request_id UUID;

ALTER TABLE transaction
    ADD CONSTRAINT uc_transaction_request_id
    UNIQUE (request_id);

UPDATE transaction
SET type_id = 6
WHERE from_wallet_id = to_wallet_id
  AND description = 'Initial balance';

ALTER TABLE wallet
    ADD CONSTRAINT wallet_nonnegative_balance
    CHECK (balance >= 0) NOT VALID;

ALTER TABLE transaction
    ADD CONSTRAINT transaction_nonnegative_amount
    CHECK (amount >= 0) NOT VALID;