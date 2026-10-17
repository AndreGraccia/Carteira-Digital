-- Dados oficiais que faltavam para as mensagens de crédito (Open Finance Credit Cards v2.3 e Loans v2.5):
-- a próxima fatura com o pagamento mínimo (abaixo dele a fatura vai para o rotativo) e o CET do empréstimo.
ALTER TABLE credit_card
    ADD COLUMN next_bill_due_date DATE,
    ADD COLUMN next_bill_total    NUMERIC(14, 2),
    ADD COLUMN next_bill_minimum  NUMERIC(14, 2);

-- CET em fração ao ano, como na API (0.290000 = 29% a.a.).
ALTER TABLE loan ADD COLUMN cet NUMERIC(9, 6);
