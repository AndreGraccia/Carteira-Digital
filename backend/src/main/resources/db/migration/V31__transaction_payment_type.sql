-- Tipo do lançamento no padrão Open Finance (PIX, TED, BOLETO, TRANSFERENCIA_MESMA_INSTITUICAO...).
-- Serve para reconhecer transferência entre contas do próprio usuário sem confundir com salário ou boleto.
-- Nulo em lançamentos locais e nos importados antes desta versão.
ALTER TABLE transaction ADD COLUMN payment_type VARCHAR(40);
