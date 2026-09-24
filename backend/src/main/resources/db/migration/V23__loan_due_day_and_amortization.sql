-- Dia de vencimento contratual e sistema de amortização do empréstimo (Open Finance Loans:
-- dueDate do cronograma e amortizationScheduled). Sem o dia contratual, um vencimento informado
-- como 28/02 arrastava todas as parcelas seguintes para o dia 28.
ALTER TABLE loan
    ADD COLUMN due_day SMALLINT,
    ADD COLUMN amortization_system VARCHAR(30),
    ADD CONSTRAINT loan_due_day_check CHECK (due_day IS NULL OR due_day BETWEEN 1 AND 31);

UPDATE loan SET due_day = EXTRACT(DAY FROM next_due_date) WHERE next_due_date IS NOT NULL;
