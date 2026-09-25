# Changelog

Mudanças da branch `feat/wallia` em relação à `main`, agrupadas por tema. Datas no fuso de Brasília.

## 25/09/2026 (noite): Sprint 11, homologação possível

Backlog novo e fluxo de entrega em [`docs/backlog.md`](docs/backlog.md).

### Corrigido
- **CI vermelho desde a Sprint 9.** O `prettier --check` falhava em 3 arquivos do frontend e os testes do frontend e o build nunca rodavam no GitHub. Formatados, e o `reportWebVitals.js` (chamado sem função, não media nada) removido.
- **Parcela no cartão perdia centavos (B10).** R$ 100,00 em 3 aparecia como 3 x R$ 33,33 nas faturas projetadas. Agora a 1ª parcela leva o resto (R$ 33,34), a mesma divisão do simulador, e a soma fecha com o valor da compra.
- **Receita e despesa escondidas como transferência (B11).** Um boleto pago numa conta e um Pix recebido em outra, de mesmo valor e no mesmo dia, sumiam dos totais de Análises. Agora só se pareiam lançamentos importados, de contas diferentes, com tipo de transferência do Open Finance (Pix, TED, DOC, TEF, mesma instituição). O tipo passa a ser gravado (V31). Lançamentos digitados à mão nunca se pareiam.

### Novo
- **Health check (B31).** `/actuator/health/liveness` e `/actuator/health/readiness`, sem detalhes. A readiness fica `DOWN` quando o banco cai; a liveness não, para a API não reiniciar em ciclo. Só `health` é exposto, e o nginx não repassa `/actuator`.
- **Compose de homologação e produção (B30).** Banco com `pg_isready`, backend com health check, e cada serviço só sobe depois do anterior saudável. `mail_host` passou a ser obrigatório: produção nunca cai no Mailpit por omissão. Roteiro de homologação no guia (seção 13.1).

### Melhorado
- GitHub Actions `checkout`, `setup-java` e `setup-node` na v5 (runtime Node 24; a v4 avisava Node 20 depreciado).

### Verificado
- Backend: 311 testes, 0 falhas, **0 pulados**: a suíte rodou contra PostgreSQL 14.6 no Docker, com a V31.
- Imagem do backend construída e rodada: health check `healthy`; com o banco parado, readiness `DOWN` e liveness `UP`.
- Frontend: lint, Prettier, i18n, 63 testes e build.
- Não verificado: o CI do GitHub depois do push (sem `gh`) e o `npm audit` (registry bloqueado na rede corporativa).

## 25/09/2026 (noite): Sprint 10, integridade do consentimento e diferenciais

### Corrigido
- **Consentimento vencido nunca ficava gravado.** O `sync` marcava o consentimento como vencido e respondia 428 dentro da mesma transação, e o rollback desfazia a marcação. O consentimento ficava `AUTHORISED` para sempre, o sync diário tentava todo dia e os dados importados nunca saíam. O mesmo acontecia com o pedido de consentimento vencido depois de 60 minutos. Agora a expiração é gravada numa transação própria e apaga os dados importados.

- **Revisão de segurança independente**, com dois achados corrigidos e testados:
  - `?extra=1e-40000000` passava na validação de faixa e custava 14 s de CPU por requisição. Agora escala e precisão são conferidas antes de qualquer conta.
  - O nome anonimizado da conta excluída era previsível e aceito pelo cadastro. Registrá-lo antes travava a exclusão de outra pessoa. Agora usa `#`, que o cadastro recusa.

### Novo
- **Revogação feita no app do banco.** Antes de ler, o gateway de participante consulta `GET /consents/v3/consents/{id}`. Se o banco diz `REJECTED`, a conexão é encerrada com o motivo que o banco informou, e os dados são apagados.
- **Quitar mais rápido** (Compromissos). Compara maior juro primeiro e menor saldo primeiro com só pagar as parcelas: meses, juros e quanto cada ordem economiza. Nenhum crédito é oferecido (ADR 0013, `GET /api/v1/future/debt-payoff`).
- **Criptografia em repouso.** Descrições de lançamentos e de compras no cartão ficam gravadas com AES-256-GCM. A chave é obrigatória em produção (ADR 0014, V30).

### Melhorado
- Metas passaram a usar a mesma conta de aportes do simulador de Análises, em vez de uma cópia.
- Conferido no repositório oficial da OFB que não saiu versão de API depois de 19/12/2025.

### Verificado
- Backend: 307 testes, 0 falhas, 14 pulados (precisam de PostgreSQL).
- Frontend: 63 testes, lint, paridade PT/EN (784 chaves) e build.

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
