-- Descrições de lançamentos e compras no cartão passam a ser gravadas criptografadas (ADR 0014).
-- O texto cifrado (prefixo, IV, tag e Base64) é mais longo que o original, então as colunas viram TEXT.
-- No PostgreSQL, VARCHAR para TEXT não reescreve a tabela. Linhas antigas continuam em texto puro e são lidas assim.
ALTER TABLE transaction ALTER COLUMN description TYPE TEXT;
ALTER TABLE card_charge ALTER COLUMN description TYPE TEXT;
