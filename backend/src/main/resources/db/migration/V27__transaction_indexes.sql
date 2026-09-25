-- As consultas do extrato, do Futuro e das Análises filtram transações pelas carteiras do usuário e por data.
-- Sem índice nas chaves estrangeiras, o EXPLAIN da auditoria de 24/09 mostrou varredura completa da tabela.
CREATE INDEX IF NOT EXISTS idx_transaction_from_wallet_created ON transaction (from_wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_to_wallet_created ON transaction (to_wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_to_wallet_status ON transaction (to_wallet_id, status);
