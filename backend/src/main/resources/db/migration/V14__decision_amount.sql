-- The user's own value for a decision. First use: everyday spending, stored as the decision with
-- pattern_key 'daily-spending'. NULL keeps the value the system estimated from the history.
ALTER TABLE recurrence_decision
    ADD COLUMN amount NUMERIC(12, 2);

ALTER TABLE recurrence_decision
    ADD CONSTRAINT recurrence_decision_amount_check
    CHECK (amount IS NULL OR (amount >= 0 AND amount <= 100000));

COMMENT ON COLUMN recurrence_decision.amount IS
    'Valor informado pelo usuario (ex.: gasto diario). NULL usa a estimativa calculada do historico.';
