# ADR 0009: leitura no padrão do Open Finance Brasil e lançamentos futuros na projeção

Status: aceito e implementado no backend. 24/09/2026.

## Contexto

A pesquisa de 24/09 (`docs/auditoria-2026-09-24/openfinance.md`, seção 7) comparou o código com os YAML oficiais (`OpenBanking-Brasil/openapi`: Accounts 2.4.2, Credit Cards Accounts 2.3.1, Loans 2.5.0, Resources 3.1.0) e achou:

1. A paginação de transações usava `page` e `meta.totalPages`. Na Accounts 2.4 o `meta` só tem `requestDateTime` e a paginação é por `links.next`: contra uma transmissora que siga o padrão, só a primeira página seria lida.
2. O modo participante lia só contas. Cartões e empréstimos existiam apenas no sandbox local, com nomes de campo inventados.
3. O sandbox emitia `DEBITO_AUTOMATICO` e `COMPRA_DEBITO`, que não existem no enum de tipos de transação.
4. `completedAuthorisedPaymentType` era ignorado. `LANCAMENTO_FUTURO` é o dado oficial para projetar o saldo, e `TRANSACAO_PROCESSANDO` conta duas vezes se for tratado como efetivado.
5. Não havia pagamento mínimo da fatura (sem ele não dá para falar de rotativo com dado real) nem CET do empréstimo.
6. `/resources` nunca era consultado.

## Decisão

- **`OfbResponses`**: funções puras que leem cada resposta pelos nomes oficiais. São testadas contra fixtures geradas dos schemas oficiais (`backend/src/test/resources/ofb`, com o gerador e a origem). Se um nome divergir do padrão, o teste quebra.
- **Paginação por `links.next`**, com teto de 50 páginas. O link vem da instituição, então só é seguido se apontar para o mesmo esquema, host e porta da raiz configurada (senão seria SSRF).
- **Raiz das APIs**: `open_finance_base_url` passa a ser a raiz (`https://host/open-banking`); cada API acrescenta `/accounts/v2`, `/credit-cards-accounts/v2`, `/loans/v2`, `/resources/v3`. O formato antigo (terminado em `/accounts/v2`) continua aceito.
- **`/resources`**: só são lidos os recursos `AVAILABLE`. Se a instituição não tiver o endpoint, lê tudo.
- **Cartões**: limite de `LIMITE_CREDITO_TOTAL`, próxima fatura com `billTotalAmount` e `billMinimumAmount` (V26), compras (`creditDebitType` DEBITO e `transactionType` OUTROS) com `chargeIdentificator` e `chargeNumber`. Cada parcela aparece numa fatura; a parcela k de n é dobrada de volta numa compra só, com a primeira fatura = fatura atual − (k − 1) meses. O fechamento não existe na API e é estimado em 10 dias antes do vencimento (marcado com `ponytail`).
- **Empréstimos**: contrato, `/scheduled-instalments` e `/payments`. Saldo devedor = `contractOutstandingBalance`; parcela = `nextInstalmentAmount`; taxa = `interestRates[0].preFixedRate` convertida de fração para % a.a. (AM vira anual composto); `amortizationScheduled` e `CET` (V26). Só entram contratos mensais: semanal ou outro fica de fora da projeção, em vez de ser adivinhado.
- **Escopo não compartilhado**: se o consentimento não cobre cartões ou empréstimos, a leitura devolve `null` ("não lido") e a importação não apaga nada.
- **Lançamentos**: `TRANSACAO_EFETIVADA` entra como sempre; `TRANSACAO_PROCESSANDO` espera até voltar efetivada; `LANCAMENTO_FUTURO` entra como transação `PENDING`, sem mexer no saldo. Quando o banco efetiva com o mesmo id, vira `SUCCESS`. Se some da lista, foi cancelado e sai.
- **Projeção**: cada `PENDING` futuro vira um evento CONFIRMED na data e no valor do banco. Ele substitui a recorrência estimada da mesma contraparte, na mesma direção, a até 5 dias, para a mesma conta não contar duas vezes.
- **Tipos do sandbox local**: `CONVENIO_ARRECADACAO` e `CARTAO`.
- **Importação em lote**: uma consulta por conta (`findByExternalIdIn`) no lugar de uma por lançamento.

## Consequências

- O modo participante ainda depende de um token pré-emitido e de um único usuário (ADR 0008). A jornada FAPI com redirecionamento continua fora do escopo acadêmico.
- O sync diário lê a janela inteira. `/transactions-current` (7 dias) seria mais barato, mas exigiria distinguir leitura completa de parcial para saber quando um lançamento futuro sumiu de verdade; fica anotado no código.
- As fixtures são geradas dos schemas e não de respostas reais. Elas garantem os nomes e os formatos, não o comportamento de um banco específico.

## Descartado

- **Copiar os YAML inteiros para o repositório** (cerca de 500 KB). Ficam o gerador, a versão e a fonte.
- **Validar o JSON contra o schema em tempo de execução.** Uma resposta fora do padrão já falha nos campos obrigatórios que o parser lê; validar tudo custaria uma dependência nova.
