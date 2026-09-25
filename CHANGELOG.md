# Changelog

Mudanças da branch `feat/wallia` em relação à `main`, agrupadas por tema. Datas no fuso de Brasília.

## 25/09/2026 (tarde): Sprint 9, sem rede externa

### Novo
- Tela "Privacidade e dados": o que a Wall.ia guarda e para quê, o que vai para a IA (só totais por mês, e só com chave do Gemini) e o botão de excluir a conta.
- Excluir a conta (`POST /api/v1/account/delete`, com a senha). Apaga o que é só da pessoa e anonimiza o cadastro. As transferências continuam no extrato de quem recebeu, sem o nome de quem saiu (ADR 0012).
- Metas mostram quanto 12 meses viram a 100% do CDI, antes do IR, e quanto isso vale descontada a inflação, com a data do Banco Central.
- No celular, a tela Futuro recolhe as perguntas já respondidas, o Simulador e as Metas em uma linha cada. A resposta principal continua aberta.
- Análises diz na própria tela que conta pela data em que o dinheiro se moveu: a compra no cartão entra no dia em que a fatura é paga (ADR 0011).

### Corrigido
- Apagar uma carteira ou um tipo não leva mais junto as transações da contraparte (tirado o `CascadeType.ALL`).
- Páginas da API no formato estável do Spring (`page.totalPages`). O aviso "Serializing PageImpl instances as-is" sumiu.
- Portas padrão 8082 (API) e 3001 (tela), sem disputar 8080 e 3000 com outros projetos.

### Testes
- Um teste varre todos os controllers e falha se um endpoint com id na URL não conferir quem pede.
- `@WebMvcTest` do extrato com a cadeia de segurança real: 401 sem token, 403 para outro usuário, dono com teto de página.
- Exclusão de conta: senha errada não apaga nada; nenhuma instrução apaga `transaction` nem a linha do usuário.

### Documentação
- Guia do desenvolvedor e roteiro de teste com o cadastro por código, o consentimento Open Finance, o Banco Central e o teste de integração.
- Trilha de estudos: laboratórios cadastram e confirmam o e-mail pelo Mailpit.

### Verificado
- Backend: 290 testes, 0 falhas, 14 pulados (precisam de PostgreSQL; o Docker não estava disponível).
- Frontend: 61 testes, lint, paridade PT/EN (765 chaves) e build.

## 25/09/2026: dados do Banco Central e fechamento das sprints 4 a 7

### Novo
- Custo do negativo calculado com a taxa média do banco do usuário publicada pelo Banco Central, ou com a mediana dos bancos quando o banco não é encontrado. Os juros nunca passam do teto legal de 8% ao mês, e o IOF vem somado. A tela e o e-mail mostram de onde veio a taxa (ADR 0010, V29).
- `GET /api/v1/market/overview`: taxas de cheque especial, rotativo e crédito pessoal, mais CDI, Selic e IPCA. Os dados ficam em cache diário no banco e são buscados às 6h.
- Compromissos:
  - pagamento mínimo da fatura, com aviso sobre o rotativo e sobre o limite de encargos da Lei 14.690/2023;
  - CET do empréstimo;
  - taxa do contrato comparada com a mediana do mercado;
  - quadro com as referências do Banco Central.
- "Sair de todos os dispositivos".

### Corrigido
- Textos que o motor escreve (gastos do dia a dia, transferências, "média dos últimos N dias") saíam em português com a tela em inglês. Agora vêm por código (`labelKey`/`originKey`) e são traduzidos.
- O extrato só trazia os 100 lançamentos mais recentes. Agora lê todas as páginas, até 10 mil lançamentos.
- A tela do banco no consentimento Open Finance piscava vazia ao fechar.
- A linha de parcelas do empréstimo ficava espremida em 375 px.

### Verificado
- Backend: 280 testes, contando o de integração com PostgreSQL 14.6 e as migrações até a V29.
- Frontend: 55 testes, lint, i18n e build.
- Navegador (Playwright, pelo DOM): 9 telas em PT e EN, a 375 e 1280 px. Nenhum estouro horizontal, nenhuma chave i18n crua e nenhum erro no console.

## 24/09/2026: auditoria e sprints 0 a 6

Veja [`docs/plano-de-evolucao.md`](docs/plano-de-evolucao.md) e [`docs/auditoria-2026-09-24/`](docs/auditoria-2026-09-24/).

- Predição:
  - salário que vira o mês não some mais da projeção;
  - dia útil bancário (ADR 0007);
  - juros do SAC corrigidos;
  - dia de vencimento contratual (V23);
  - limite diário com a renda chegando amanhã.
- Análises:
  - mês calculado no fuso de Brasília;
  - transferência entre contas próprias não conta como receita;
  - a chamada à IA roda fora da transação.
- Segurança:
  - consentimento do sandbox de participante preso a um único usuário;
  - e-mail confirmado no cadastro (V24, ADR 0008);
  - JWT com emissor e audiência validados, e revogação de todos os tokens (V28);
  - sessão expirada volta ao login em qualquer tela.
- Open Finance no padrão oficial:
  - ciclo de consentimento `AWAITING_AUTHORISATION`/`AUTHORISED`/`REJECTED` (V25);
  - paginação por `links.next`;
  - cartões, empréstimos e `/resources`;
  - lançamentos futuros na projeção (V26, ADR 0009).
- API:
  - extrato paginado no banco;
  - índices (V27);
  - status HTTP por tipo de erro;
  - Gemini com retry;
  - jobs com advisory lock.
- Frontend:
  - code splitting por rota;
  - skeletons;
  - gráfico responsivo;
  - cabeçalho em 320 px;
  - valores em centavos.

## Antes

Motor Futuro, limite diário, perguntas, plano de saída, metas, envelopes, placar de precisão, Pix simulado, alertas por e-mail e Web Push, sync diário e auditoria admin. Veja os ADRs 0001 a 0006.
