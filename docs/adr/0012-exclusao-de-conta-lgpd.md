# ADR 0012: excluir a conta anonimizando, sem apagar o extrato de terceiros

Status: aceito e implementado. 25/09/2026.

## Contexto

A LGPD dá ao titular o direito de pedir a eliminação dos dados (art. 18, VI). A Wall.ia não tinha como excluir a conta, e a "privacidade visível" prometida na visão do produto não existia.

O obstáculo é uma transferência entre Ana e Bruno: a linha em `transaction` aponta para as duas carteiras. Apagar a carteira da Ana ou quebra a chave estrangeira ou, com cascade, apaga o extrato do Bruno. O cascade de `Wallet` para `Transaction` foi retirado em 25/09 justamente para que isso não aconteça por acidente.

## Decisão

`POST /api/v1/account/delete`, com a senha no corpo e sempre para o usuário do token (`AccountDeletionService`):

1. A senha é pedida de novo, porque uma sessão roubada não pode apagar a conta. As tentativas contam no mesmo limite do login.
2. É apagado o que é só da pessoa: os dados do Open Finance (o mesmo `purge` da revogação) e a conexão, as respostas às recorrências, as metas, os envelopes, os alertas, os snapshots do placar, as inscrições de push, os códigos de e-mail, os papéis e as carteiras locais sem nenhuma movimentação.
3. A linha do usuário fica, anonimizada:
   - usuário `excluido-<id>` e e-mail `excluido-<id>@invalid`;
   - nome "Conta excluida";
   - senha aleatória;
   - `email_verified_at` nulo, então o login é recusado;
   - `tokens_valid_after = agora`, então todo token já emitido cai.
4. As carteiras locais com movimentação ficam, com o nome "Conta encerrada", porque são também o histórico de outra pessoa. A LGPD permite guardar para cumprir obrigação ou proteger direito de terceiro (art. 16).
5. A tela "Privacidade e dados" diz o que é guardado e para quê, o que vai para a IA (só totais por mês, e só com chave do Gemini) e oferece o botão de exclusão.

## Consequências

- O extrato de terceiros continua íntegro e sem o nome de quem saiu.
- `admin_audit` continua apontando para o id. A trilha de auditoria de ações administrativas não é apagada.
- O usuário e o e-mail antigos voltam a ficar livres para um novo cadastro.
- As instruções SQL só são provadas contra PostgreSQL no `WalliaApplicationTests`, que precisa de Docker. O teste de unidade garante a ordem, a senha e que nenhuma instrução apaga `transaction` ou a linha do usuário.
- Continua de fora: criptografia em repouso dos dados importados e um prazo de retenção automático.
