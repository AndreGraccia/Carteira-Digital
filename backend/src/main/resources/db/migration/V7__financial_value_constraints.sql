ALTER TABLE wallet
    ADD CONSTRAINT wallet_balance_scale
    CHECK (
        balance <= 9999999999.99
        AND balance = round(balance, 2)
    ) NOT VALID;

ALTER TABLE transaction
    ADD CONSTRAINT transaction_positive_amount
    CHECK (
        amount > 0
        AND amount <= 9999999999.99
        AND amount = round(amount, 2)
    ) NOT VALID;

COMMENT ON COLUMN wallet.iban IS
    'Identificador IBAN sintatico usado apenas nesta demonstracao academica; nao representa conta bancaria ou chave Pix.';

COMMENT ON COLUMN wallet.balance IS
    'Saldo simulado em BRL. Nao representa dinheiro custodiado.';

COMMENT ON COLUMN transaction.request_id IS
    'Chave UUID de idempotencia enviada pelo cliente para impedir operacoes duplicadas.';
