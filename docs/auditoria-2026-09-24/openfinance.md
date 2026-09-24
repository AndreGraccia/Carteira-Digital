# Wall.ia × Open Finance Brasil: relatório de pesquisa (set/2026)

Escopo: orientar a evolução do sandbox caseiro do Wall.ia (`backend/src/main/java/com/github/yildizmy/service/openfinance`) em direção ao padrão oficial. Nenhum dinheiro real.

Método e limites:
- Swaggers oficiais baixados do repositório `OpenBanking-Brasil/openapi` (último commit em 19/12/2025). As versões estáveis mais novas desse repositório são as listadas na seção 1. O Confluence da OFB e as páginas do bcb.gov.br são SPA e não renderizaram via fetch, então algumas afirmações vêm de busca e estão marcadas.
- As séries do BCB foram consultadas **ao vivo** em 24/09/2026.
- Onde não confirmei, está escrito **não confirmado**.

---

## 1. Arquitetura do Open Finance Brasil

### Papéis
| Papel | O que faz |
| --- | --- |
| **Transmissora** (detentora da conta) | Expõe as APIs de dados e valida o consentimento. |
| **Receptora** (TPP de dados) | Cria o consentimento e consome os dados. |
| **ITP**, Iniciadora de Transação de Pagamento | Inicia Pix na conta do cliente em outra instituição. Precisa de autorização específica do BCB como instituição de pagamento na modalidade iniciadora. |

As fases históricas foram quatro: 1) dados abertos de produtos e canais; 2) dados cadastrais e transacionais; 3) iniciação de pagamento; 4) investimentos, seguros, câmbio e previdência.

Base normativa:
- Resolução Conjunta nº 1/2020 (escopo, participantes, consentimento). PDF consolidado v7: https://normativos.bcb.gov.br/Lists/Normativos/Attachments/51028/Res_Conj_0001_v7_L.pdf
- Resolução BCB nº 32/2020 (requisitos técnicos e procedimentos operacionais).
- Lista oficial de atos normativos: https://openfinancebrasil.org.br/atos-normativos/

### Jornada de consentimento (API Consents v3.3.1)
Fonte: https://raw.githubusercontent.com/OpenBanking-Brasil/openapi/main/swagger-apis/consents/3.3.1.yml

**Endpoints**
- `POST /open-banking/consents/v3/consents`
- `GET /consents/{consentId}`
- `DELETE /consents/{consentId}`
- `POST /consents/{consentId}/extends` (renovação)
- `GET /consents/{consentId}/extensions`

**`status`**
- `AWAITING_AUTHORISATION`: criado. Ainda não há recursos vinculados.
- `AUTHORISED`: aprovado pelo cliente. Os recursos já foram selecionados.
- `REJECTED`: estado final, cobre recusa **e também revogação**.

Não existe status `REVOKED`. A revogação vira `REJECTED` com o objeto `rejection` preenchido:
- `rejectedBy`: `USER`, `ASPSP` ou `TPP`
- `reason.code`: `CUSTOMER_MANUALLY_REJECTED`, `CUSTOMER_MANUALLY_REVOKED`, `CONSENT_EXPIRED`, `CONSENT_MAX_DATE_REACHED`, `CONSENT_TECHNICAL_ISSUE` ou `INTERNAL_SECURITY_REASON`

Um `DELETE` em consentimento já `REJECTED` retorna 422.

**`expirationDateTime`**
- Para prazo **indeterminado**, a receptora **não envia** o campo.
- Para prazo determinado, o campo é obrigatório.
- O limite antigo de 12 meses caiu com a mudança normativa de 2023. Fontes: https://agenciagov.ebc.com.br/noticias/202310/bc-simplifica-renovacao-de-consentimentos-no-open-finance-e-amplia-prazo-de-validade-do-compartilhamento e https://finsidersbrasil.com.br/regulamentacao/bc-acaba-com-limite-de-12-meses-para-compartilhamento-de-dados-no-open-finance/
- A renovação só se aplica a consentimentos `AUTHORISED` e só altera a validade e a finalidade.

**`permissions`** (agrupamentos obrigatórios, cada um exige também `RESOURCES_READ`)
- Contas: `ACCOUNTS_READ`, `ACCOUNTS_BALANCES_READ`, `ACCOUNTS_TRANSACTIONS_READ`, `ACCOUNTS_OVERDRAFT_LIMITS_READ`
- Cartão: `CREDIT_CARDS_ACCOUNTS_READ`, `..._BILLS_READ`, `..._BILLS_TRANSACTIONS_READ`, `..._LIMITS_READ`, `..._TRANSACTIONS_READ`
- Operações de crédito: `LOANS_*`, `FINANCINGS_*`, `INVOICE_FINANCINGS_*`, `UNARRANGED_ACCOUNTS_OVERDRAFT_*`. É um grupo "agrupado": todas as permissões do grupo precisam ser mantidas juntas.
- Regras de resposta:
  - A transmissora devolve só o subconjunto que suporta.
  - Se sobrar uma permissão que não existe nos agrupamentos, retorna 400.
  - Se não sobrar nenhuma permissão funcional, retorna 422 (`SEM_PERMISSOES_FUNCIONAIS_RESTANTES`).
  - Combinação errada de permissões retorna `COMBINACAO_PERMISSOES_INCORRETA`.

### Versões estáveis mais novas no repositório oficial (dez/2025)
| API | Versão | Base path |
| --- | --- | --- |
| consents | 3.3.1 | `/open-banking/consents/v3` |
| resources | 3.1.0 | `/open-banking/resources/v3` |
| accounts | 2.4.2 (2.5.0-beta.2 em desenvolvimento) | `/open-banking/accounts/v2` |
| credit-cards-accounts | 2.3.1 (2.4.0-beta.2 em desenvolvimento) | `/open-banking/credit-cards-accounts/v2` |
| loans | 2.5.0 (2.6.0-beta.1 em desenvolvimento) | `/open-banking/loans/v2` |
| financings | 2.4.0 | não verificado |
| payments (Pix) | 4.0.0 | não verificado |
| automatic-payments | 2.0.0 | não verificado |
| enrollments (JSR) | 2.1.0 | não verificado |

Não verifiquei se versões mais novas saíram depois de dez/2025. O catálogo vigente fica em https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/17367659/Especifica+es+de+APIs

### Campos principais (confirmados nos YAML)

**Accounts 2.4.2**
Endpoints: `/accounts`, `/accounts/{accountId}`, `/balances`, `/transactions`, `/transactions-current` (últimos 7 dias, até D-6) e `/overdraft-limits`.
- **Conta:** `brandName`, `companyCnpj`, `type` (`CONTA_DEPOSITO_A_VISTA`, `CONTA_POUPANCA`, `CONTA_PAGAMENTO_PRE_PAGA`), `compeCode`, `branchCode`, `number`, `checkDigit`, `accountId`, `subtype` (`INDIVIDUAL`, `CONJUNTA_SIMPLES`, `CONJUNTA_SOLIDARIA`).
- **Saldo:** `availableAmount`, `blockedAmount`, `automaticallyInvestedAmount` (cada um no formato `{amount: "string", currency}`) e `updateDateTime`.
- **Transação:** `transactionId`, `completedAuthorisedPaymentType` (`TRANSACAO_EFETIVADA`, `LANCAMENTO_FUTURO`, `TRANSACAO_PROCESSANDO`), `creditDebitType` (`CREDITO`, `DEBITO`), `transactionName`, `type`, `transactionAmount`, `transactionDateTime`, além dos dados da contraparte (`partie*`).
- **Valores de `type`:** `TED`, `DOC`, `PIX`, `TRANSFERENCIA_MESMA_INSTITUICAO`, `BOLETO`, `CONVENIO_ARRECADACAO`, `PACOTE_TARIFA_SERVICOS`, `TARIFA_SERVICOS_AVULSOS`, `FOLHA_PAGAMENTO`, `DEPOSITO`, `SAQUE`, `CARTAO`, `ENCARGOS_JUROS_CHEQUE_ESPECIAL`, `RENDIMENTO_APLIC_FINANCEIRA`, `PORTABILIDADE_SALARIO`, `RESGATE_APLIC_FINANCEIRA`, `OPERACAO_CREDITO`, `OUTROS`.

**Paginação**
- **Listas** (`/accounts`, `/contracts`): parâmetros `page` e `page-size` (padrão 25, máximo 1000). A resposta traz `links` (`self`, `first`, `prev`, `next`, `last`) e `meta` (`totalRecords`, `totalPages`, `requestDateTime`).
- **Transações** (conta e cartão): paginação por **`pagination-key`**. A resposta traz `links` (`self`, `first`, `prev`, `next`) e `meta` com **apenas `requestDateTime`**, sem `totalPages`. Para continuar, siga `links.next` até ele não vir mais.

**Credit Cards 2.3.1**
Endpoints: `/accounts`, `/accounts/{id}`, `/bills`, `/bills/{billId}/transactions`, `/limits`, `/transactions` e `/transactions-current`.
- **Conta:** `creditCardAccountId`, `brandName`, `companyCnpj`, `name`, `productType` (`GOLD`, `PLATINUM`, `BLACK`…), `creditCardNetwork`.
- **Fatura:** `billId`, `dueDate`, `billTotalAmount`, `billMinimumAmount`, `isInstalment`, `financeCharges`, `payments`.
- **Transação:** `transactionId`, `identificationNumber`, `transactionName`, `billId`, `creditDebitType`, `transactionType` (`PAGAMENTO`, `TARIFA`, `OPERACOES_CREDITO_CONTRATADAS_CARTAO`, `ESTORNO`, `CASHBACK`, `OUTROS`), `paymentType`, `feeType`, `chargeIdentificator` e `chargeNumber` (parcelamento), `brazilianAmount`, `amount`, `transactionDateTime`, `billPostDate`, `payeeMCC`.

**Loans 2.5.0**
Endpoints: `/contracts`, `/contracts/{id}`, `/warranties`, `/scheduled-instalments` e `/payments`.
Campos: `contractNumber`, `ipocCode`, `productType`, `productSubType`, `contractDate`, `contractAmount`, `CET`, `interestRates[]` (`taxType`, `interestRateType`, `taxPeriodicity`, `calculation`, `preFixedRate`, `postFixedRate`, `referentialRateIndexerType`), `contractedFees`, `contractedFinanceCharges`, `totalNumberOfInstalments`, `paidInstalments`, `dueInstalments`, `pastDueInstalments`, `contractOutstandingBalance`, `nextInstalmentAmount`, `balloonPayments`.

**Resources 3.1.0**
`GET /resources` devolve `type` (`ACCOUNT`, `CREDIT_CARD_ACCOUNT`, `LOAN`, `FINANCING`…) e `status` (`AVAILABLE`, `UNAVAILABLE`, `TEMPORARILY_UNAVAILABLE`, `PENDING_AUTHORISATION`).

---

## 2. Participar de verdade × alternativas acadêmicas

### Participar de verdade
1. Ser instituição autorizada pelo BCB, ou firmar parceria contratual com uma.
2. Entrar no **Diretório de Participantes**.
3. Ter certificados **ICP-Brasil** (transporte/mTLS e assinatura).
4. Implementar o perfil de segurança **FAPI-BR**: OAuth2 + OIDC, PAR, JARM, `private_key_jwt`, DCR/DCM. https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/240649123
5. Passar nas duas certificações de conformidade (segurança via OpenID Foundation e funcional via **Motor de Conformidade**). Uma API só entra no Diretório de produção se estiver certificada nas duas.
6. Referências: https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/155910145 e https://openfinancebrasil.org.br/certificado-de-conformidade/

O sandbox do Diretório exige usuário **vinculado a uma organização participante**, então não serve a um estudante isolado.

### Alternativas realistas para projeto acadêmico
- **`OpenBanking-Brasil/mock-api`** (GitHub oficial): workflow que gera imagem Docker de mock a partir dos Swaggers oficiais. Último push em abr/2024, então verificar se cobre as versões atuais. Serve para testar o `ParticipantSandboxOpenFinanceGateway` contra o contrato oficial. https://github.com/OpenBanking-Brasil/mock-api
- **Validação por contrato**: rodar os YAML oficiais em Prism ou openapi-diff contra o sandbox caseiro, ou gerar DTOs a partir deles. Custo zero.
- **Pluggy**: sandbox gratuito, sem limite de tempo e sem cartão. Produção de dados a partir de R$ 2.500/mês e Pix a partir de R$ 500/mês, com trial de 15 dias. SDK Java disponível. https://www.pluggy.ai/pricing e https://www.pluggy.ai/desenvolvedores
- **Meu Pluggy**: grátis, mas só para **as próprias contas**, com até 5 conexões e atualização a cada 24 h. Proibido para produto com terceiros. https://www.pluggy.ai/meu-pluggy
- **Belvo**: sandbox gratuito, só para teste. Produção paga. O valor de ~R$ 6 mil/mês vem de relato de dev e é **não confirmado** oficialmente. https://developers.belvo.com/pt-br/developer_resources/resources-sandbox e https://belvo.com/plans-and-pricing/

Recomendação: manter o sandbox próprio, mas **fiel ao contrato oficial**, e opcionalmente adicionar um terceiro gateway que use o sandbox da Pluggy.

---

## 3. Pix, Pix Agendado e Pix Automático

### Iniciar pagamento pelo app
Só é possível como ITP autorizada ou via parceiro (agregador ou banco).
- Fluxo: consentimento de pagamento, redirecionamento ou JSR, e depois `POST /pix/payments` (API payments v4.0.0).
- Consentimento de pagamento autorizado expira em "agora + 60 min" e depois vira `CONSUMED`. Fonte: máquina de estados de pagamentos na OFB (https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/347078805) e o mesmo swagger de consents.

### Pix Automático
- Lançado em **16/06/2025**. Fonte: https://agenciabrasil.ebc.com.br/economia/noticia/2025-06/banco-central-anuncia-o-pix-automatico
- Autorização prévia única do pagador, gratuita para ele.
- O PSP recebedor envia a instrução **2 a 10 dias antes** do vencimento.
- Novas tentativas em até **7 dias**, só se a autorização previr.
- Norma: Resolução BCB 402/2024 (https://www.legisweb.com.br/legislacao/?id=462331).
- Guias do BCB: https://www.bcb.gov.br/content/estabilidadefinanceira/pix/automatico/guia_pix_automatico.pdf e https://www.bcb.gov.br/content/estabilidadefinanceira/pix/pix-automatico-FAQ-participantes.pdf
- Via Open Finance: API `automatic-payments` v2.0.0.

### JSR e Pix por aproximação
- Regulados pelas Resoluções BCB 406 e 407/2024. Disponíveis ao público desde **28/02/2025**.
- Usam vínculo de dispositivo (API `enrollments` v2.1.0) e FIDO2.
- Referências: https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/1449754670 e https://finsidersbrasil.com.br/pagamentos/comeca-piloto-da-jornada-que-vai-permitir-o-pix-por-aproximacao/
- **Não confirmado:** uma busca citou a "IN BCB nº 746/2026", que tiraria o teto único do Pix por aproximação a partir de 01/10/2026. Não achei a fonte primária.

### Simular corretamente
- Modelar o Pix sugerido como um **consentimento de pagamento** com o mesmo formato da API oficial: `AWAITING_AUTHORISATION`, depois `AUTHORISED`, depois `CONSUMED` (ou `REJECTED`). O pagamento em si passa por `RCVD`, `ACCP`, `ACSC` e `RJCT` (ou `SCHD`, se agendado). Os códigos de status do pagamento v4 foram **não confirmados** no YAML nesta pesquisa.
- Deixar explícito na UI que "nenhum dinheiro sai".
- Para o Pix Automático: a simulação deve mostrar a autorização com valor máximo e periodicidade, o aviso de 2 a 10 dias antes do débito e a opção de cancelar.

---

## 4. Dados públicos do BCB (consultados ao vivo em 24/09/2026)

**SGS**
- Endpoint: `https://api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados?formato=json&dataInicial=dd/MM/aaaa&dataFinal=dd/MM/aaaa`
- Última observação: `.../dados/ultimos/N?formato=json`
- A série diária aceita no máximo 10 anos por consulta. Esse limite é **não confirmado** nesta pesquisa.

| Código | Série | Última leitura |
| --- | --- | --- |
| 432 | Meta Selic (% a.a.) | 13,75 (a data vem projetada até a próxima reunião do Copom) |
| 11 | Selic diária (% a.d.) | 0,050788 em 24/09/2026 |
| 12 | CDI diário (% a.d.) | 0,050788 |
| 4389 | CDI anualizado base 252 | 13,65 |
| 433 | IPCA mensal (%) | −0,32 em ago/2026 |
| 13522 | IPCA acumulado em 12 meses | 4,22 (código de uso comum, nome não validado nos metadados) |
| 1 | Dólar comercial (venda) | 5,1795 |
| 20714 | Taxa média de juros (% a.a.) | 32,08 em jul/2026. A modalidade exata é **não confirmada**. |
| 20716 | Taxa média de juros (% a.a.) | 37,41 em jul/2026. A modalidade exata é **não confirmada**. |

Para as séries 20714 e 20716, validar o nome em https://www3.bcb.gov.br/sgspub antes de usar.

**Focus (Olinda)**
- Endpoint: `https://olinda.bcb.gov.br/olinda/servico/Expectativas/versao/v1/odata/ExpectativasMercadoAnuais?$filter=Indicador eq 'IPCA'&$orderby=Data desc&$format=json`
- Leitura de 18/09/2026: mediana do IPCA de 2026 em 4,92%, de 2027 em 4,30% e de 2028 em 3,80%.

**Taxas de juros por instituição (Olinda)**
- Endpoint: `https://olinda.bcb.gov.br/olinda/servico/taxaJuros/versao/v2/odata/TaxasJurosMensalPorMes?$format=json`
- Campos: `Mes`, `Modalidade`, `Posicao`, `InstituicaoFinanceira`, `TaxaJurosAoMes`, `TaxaJurosAoAno`, `cnpj8`, `anoMes`. Os dados vão até ago/2026.
- Serve para dizer "seu empréstimo a X% a.a. está acima ou abaixo da média do seu banco e do mercado".

**PTAX**
- Serviço Olinda `PTAX/versao/v1` (por exemplo, `CotacaoDolarDia(dataCotacao=@d)`). O caminho exato é **não testado** nesta pesquisa.

Portal de dados abertos: https://dadosabertos.bcb.gov.br

---

## 5. Regras que afetam as mensagens do app

**Cheque especial**
- Teto de **8% a.m.** (~151,8% a.a.), pela Resolução CMN 4.765/2019, em vigor desde 06/01/2020.
- A mesma norma permitia tarifa de até 0,25% sobre o limite acima de R$ 500. A vigência atual dessa tarifa é **não confirmada**.
- Fontes: https://normativos.bcb.gov.br/Lists/Normativos/Attachments/50875/Res_4765_v2_P.pdf e https://www.gov.br/pt-br/noticias/financas-impostos-e-gestao-publica/2020/01/ja-esta-valendo-a-limitacao-de-juros-para-cheque-especial

**Rotativo e parcelamento de fatura**
- Juros e encargos somados não podem passar de **100% do valor original** da dívida. Lei 14.690/2023 e Resolução CMN 5.112/2023, com vigência desde 03/01/2024.
- Fonte: https://finsidersbrasil.com.br/regulamentacao/juro-do-rotativo-do-cartao-de-credito-sera-limitado-a-100/

**IOF sobre crédito para pessoa física**
- **0,0082% ao dia**, limitado a 365 dias (cerca de 3% a.a.), **mais 0,38% fixo**. Máximo de aproximadamente 3,38%.
- Os Decretos 12.466 e 12.499/2025 foram restabelecidos pelo STF em jul/2025.
- Fontes: https://www2.camara.leg.br/legin/fed/decret/2025/decreto-12499-11-junho-2025-797588-publicacaooriginal-175631-pe.html e https://www.infomoney.com.br/guias/iof/
- A leitura de que o percentual para PF não mudou vem de fontes secundárias e está **parcialmente confirmada**.

**CET**
- Vem pronto no campo `CET` da API Loans. O app deve exibi-lo em vez de recalculá-lo.
- A obrigação de informar o CET vem da Res. CMN 3.517/2007, **não confirmada nesta pesquisa**.

**Registrato / SCR**
- É o canal oficial e gratuito para o cidadão ver as próprias dívidas no SCR. O app pode apontar para ele, mas não tem API pública.
- A URL atual é **não confirmada**.

**Regime de caixa e competência**
- A projeção de saldo deve usar **caixa**: data de débito, `dueDate` da fatura e vencimento da parcela.
- O relatório de gastos pode usar **competência**: data da compra, `transactionDateTime`, com a parcela distribuída pelos meses.
- COSIF não se aplica a pessoa física. Para categorizar, basta `type`, `transactionType`, `payeeMCC` (cartão) e contraparte.

---

## 6. Checklist de conformidade e LGPD

- [ ] Base legal de **consentimento** específica por finalidade (LGPD, art. 7º, I, e art. 8º), com finalidade escrita na tela e no dado persistido.
- [ ] Permissões **mínimas** para a finalidade, sem pedir `LOANS_*` se o app não usa empréstimo.
- [ ] Revogação a qualquer momento, com o **mesmo esforço** do consentimento: um clique em "Desconectar".
- [ ] Ao revogar, apagar ou anonimizar os dados importados daquela conexão, com prazo definido e registrado.
- [ ] Mostrar a validade (`expirationDateTime`, ou "indeterminado") e avisar antes de expirar.
- [ ] Log de auditoria do consentimento: criado, autorizado, rejeitado e revogado, com `rejectedBy` e `reason`.
- [ ] Segredos, token, `consent-id` e keystore mTLS só via variável de ambiente. Nunca em log.
- [ ] Dados financeiros criptografados em repouso, retenção mínima e direito de acesso e exclusão (LGPD, art. 18).
- [ ] Rótulo claro: "ambiente acadêmico / dados simulados / nenhum dinheiro movimentado", sem imitar marca de banco real.
- [ ] Mensagens de crédito com números normativos e fonte (seção 5), sem soar como recomendação de investimento.

---

## 7. Gaps do sandbox caseiro frente ao padrão (grep no código)

Arquivos analisados, todos em `backend/src/main/java/com/github/yildizmy/`:
- `service/openfinance/ParticipantSandboxOpenFinanceGateway.java`
- `service/openfinance/ContractSandboxOpenFinanceGateway.java`
- `service/openfinance/OpenFinanceSnapshot.java`
- `service/OpenFinanceService.java`
- `domain/enums/OpenFinanceConnectionStatus.java`

1. **Status de consentimento.** O enum tem `AUTHORISED` e `REVOKED`. O padrão é `AWAITING_AUTHORISATION`, `AUTHORISED` e `REJECTED`, com `rejection.rejectedBy` e `rejection.reason.code` (`CUSTOMER_MANUALLY_REVOKED`, `CONSENT_EXPIRED`…). `REVOKED` não existe no padrão.
2. **Bug provável de paginação** (`ParticipantSandbox…:113-130`). O código usa `page` e `page-size` e para em `meta.totalPages`. Na Accounts v2.4.2, `/transactions` pagina por `pagination-key` e `links.next`, e o `meta` traz só `requestDateTime`. Com uma transmissora aderente, o `asInt(1)` faz o loop **parar na primeira página**. Correção: seguir `links.next` até ele sumir.
3. **`type` fora do enum.** O `ContractSandbox` emite `DEBITO_AUTOMATICO` e `COMPRA_DEBITO`, que não existem no `EnumTransactionTypes`. Os equivalentes oficiais são `CONVENIO_ARRECADACAO` ou `OPERACAO_CREDITO` para débito automático, e `CARTAO` para compra no débito.
4. **Falta `completedAuthorisedPaymentType`** no `Entry`. `LANCAMENTO_FUTURO` é justamente o dado oficial para **projetar o saldo futuro**, e `TRANSACAO_PROCESSANDO` evita contar o mesmo lançamento duas vezes.
5. **Saldo incompleto.** Só `availableAmount` é lido. Faltam `blockedAmount`, `automaticallyInvestedAmount` e `updateDateTime`. A conta também não tem `compeCode` nem `subtype`.
6. **Permissões** (`OpenFinanceService:26-28`). A lista tem só `ACCOUNTS_*`: falta `RESOURCES_READ`, que é obrigatório. Mesmo assim, o snapshot importa cartões e empréstimos, que exigiriam `CREDIT_CARDS_ACCOUNTS_*` e o agrupamento completo de `LOANS_*`. O escopo do consentimento não cobre os dados usados.
7. **Cartão com nomes inventados.** O código usa `lastDigits`, `closingDay`, `dueDay`, `limitAmount`, `installments` e `firstInvoiceDue`. O padrão usa `bills[]` (`billId`, `dueDate`, `billTotalAmount`, **`billMinimumAmount`**, `isInstalment`, `financeCharges`), `/limits` e `chargeIdentificator` e `chargeNumber` para as parcelas. Sem `billMinimumAmount`, não dá para emitir o alerta de rotativo e do teto de 100% com base em dado real.
8. **Empréstimo com nomes divergentes.** O código usa `outstandingBalance`, `installmentAmount`, `totalInstallments`, `paidInstallments` e `annualRate`. O padrão usa `contractOutstandingBalance`, `nextInstalmentAmount`, `totalNumberOfInstalments`, `paidInstalments`, `dueInstalments`, `pastDueInstalments` e `interestRates[]`, com `taxType`, periodicidade e pré/pós. Faltam também **`CET`**, `contractDate`, `ipocCode` e `/scheduled-instalments`. Detalhe de grafia: o padrão escreve "instalment", com um "l" só.
9. **Consentimento nunca consultado.** O gateway de participante não faz `GET /consents/{id}`. Por isso `expiresAt` volta `null` e o status não é conferido. Também não chama `/resources` para descobrir contas `UNAVAILABLE` ou `TEMPORARILY_UNAVAILABLE`.
10. **Janela de transações.** O código pede 12 meses em `/transactions`. Para os últimos 7 dias, o padrão tem `/transactions-current`, e a sincronização diária (`OpenFinanceDailySync`) deveria usar esse endpoint.
11. **Pontos que já estão corretos:**
    - cabeçalhos `x-fapi-interaction-id`, `x-fapi-auth-date` e `x-customer-user-agent`;
    - mTLS via keystore PKCS12;
    - nomes de conta `brandName`, `companyCnpj`, `branchCode`, `number`, `checkDigit` e o enum `type`;
    - `creditDebitType` com `CREDITO` e `DEBITO`;
    - `transactionAmount.amount` lido como string;
    - `fromBookingDate` e `toBookingDate`.
12. **Pix simulado.** Hoje ele é só telemetria (`FutureController /pix-interest`, com passos `PREPARE` e `CONFIRM`). Não existe um objeto de consentimento ou pagamento com os estados oficiais (seção 3).
