# Auditoria do backend Wall.ia (Carteira-Digital/backend)

Data: 24/09/2026. Somente leitura: nenhum arquivo do projeto foi alterado.
Caminhos relativos a `backend/src/main/java/com/github/yildizmy/`, salvo indicação.

## 0. O que foi executado

| Verificação | Resultado |
| --- | --- |
| `./mvnw -q test` (suíte inteira) | EXIT=0. 34 classes, **220 testes, 0 falhas, 0 erros, 4 pulados** (os 4 de `FlywayMigrationsTest`, que exigem `WALLIA_IT_DB_URL`) |
| `FlywayMigrationsTest` contra Postgres 14.6 descartável (container temporário na porta 55432, já removido) | **4/4 passaram**; V1..V22 aplicadas sem falha, numeração contínua |
| `WalliaApplicationTests` | Classe **sem nenhum @Test**: o contexto Spring nunca sobe na suíte. Não existe nenhum teste de controller/MockMvc, então rota, autorização e mapeamento de status HTTP não têm teste |
| Sondagem do backend rodando (127.0.0.1:8082) | Rotas protegidas respondem 401 sem token; preflight CORS de origem estranha, 403; PATCH fora da lista de métodos do CORS, 403; JSON malformado, 400 em ProblemDetail. Não há `/actuator/health` (401) |
| Sonda jshell sobre `target/classes` (scratchpad `probe.jsh`) | Reproduziu os achados F1, F3, F4 e F5 abaixo, com a saída colada em cada um |
| Modelo Gemini | `gemini-3.8-flash` **existe** (listado como estável em ai.google.dev/gemini-api/docs/models). O formato `response_format{type,mime_type,schema}` e a leitura de `output_text` batem com a doc atual da Interactions API |

Não fiz chamadas autenticadas ao servidor ao vivo: para isso seria preciso criar usuário no banco de dev. Os achados do motor foram provados por execução direta das classes.

Severidade: **Crítica** (vaza dado ou dá prejuízo direto) · **Alta** (número errado para o usuário ou quebra em produção) · **Média** (heurística frágil ou inconsistência visível) · **Baixa** (higiene).

---

## 1. Motor Futuro / predição

### F1 [Alta] Salário pago antes ou depois da virada do mês some ou aparece duas vezes na projeção
- **Arquivo:** `service/future/CashEventPlanner.java:105`, com `service/future/RecurrenceDetector.java:88`
- **Problema:** um mês é marcado como "já aconteceu" quando `YearMonth(date) <= YearMonth(lastSeen)`. Salário de dia 30 que caiu em 01/09 (fim de semana ou feriado) faz setembro contar como visto, e o salário de 30/09 **desaparece**. O resultado é uma janela negativa falsa de um mês inteiro, com alerta por e-mail e push. `typicalDay` é a mediana crua do dia do mês, então uma série como 01, 30, 31, 31 vira dia 31.
- **Prova (jshell):** ocorrências 30/06, 30/07, 01/09 com hoje = 02/09 e horizonte até 31/10 produzem `events=[2026-10-30]`. O salário de 30/09 não aparece.
- **Correção:** decida pela distância até a última ocorrência (ex.: `date` a mais de 20 dias de `lastSeen`), não pelo mês-calendário. Calcule `typicalDay` de forma circular (dia 1 ≈ dia 31). Teste: salário no último dia útil com virada de mês.

### F2 [Média] Nenhum ajuste para dia útil, fim de semana ou feriado
- **Arquivo:** `CashEventPlanner.java:98-120` (recorrências), `CardPlan.java:30-32` (vencimento), `LoanPlan.java:27-36`
- **Problema:** vencimentos e créditos caem em sábado, domingo ou feriado. No Brasil a fatura ou boleto passa para o próximo dia útil, e o salário costuma ser antecipado (ou pago até o 5º dia útil, pela CLT). A janela negativa pode ser apontada com 1 a 3 dias de erro, justamente no dia crítico. Nenhum teste cobre isso: busquei `SATURDAY`, `weekend`, `feriado`, `02-28` e `-31` e não achei nada.
- **Correção:** função `ajustarDiaUtil(data, direção)` com feriados nacionais fixos e móveis (Páscoa). Débito vai para o próximo dia útil; crédito, para o anterior.

### F3 [Média] Empréstimo perde o dia de vencimento original depois de fevereiro
- **Arquivo:** `service/future/LoanPlan.java:30`
- **Problema:** `day = nextDueDate.getDayOfMonth()`. Se o banco informa o próximo vencimento como 28/02 (contrato no dia 31), todas as parcelas seguintes vão para o dia 28.
- **Prova:** `loan dues=[2027-02-28, 2027-03-28, 2027-04-28, 2027-05-28]`
- **Correção:** guarde o dia contratual (OFB: `dueDate` do cronograma em `/scheduled-instalments`) ou derive-o de `contractDate`.

### F4 [Média] "Juros que faltam" está errado para SAC, mas a documentação diz que está exato
- **Arquivo:** `service/future/LoanCost.java:11` (javadoc) e `:21-25`
- **Problema:** `parcela × restantes − saldo` só vale para Price. No SAC a parcela cai a cada mês, e usar a próxima parcela superestima os juros.
- **Prova:** SAC de 10.000 a 1% a.m., 10 parcelas restantes, próxima de 1.100. O código dá `1000.00`; o valor real é `550.00`.
- **Correção:** use o sistema de amortização (`amortizationScheduled` no OFB Loans) ou o cronograma de parcelas. Enquanto isso, devolva null quando não for Price.

### F5 [Média] O limite diário vira o saldo inteiro quando a renda é amanhã
- **Arquivo:** `service/future/SpendingLimit.java:39` e `:62`
- **Problema:** `spendingDays = 0`, o laço não produz candidato e `daily = openingBalance`. A tela mostra "pode gastar R$ 5.000/dia".
- **Prova:** `limit income tomorrow: daily=5000.00 days=0`
- **Correção:** com 0 dias, devolva `daily = balance` rotulado como "hoje", ou use o próximo ciclo (renda seguinte). Precisa de teste.

### F6 [Média] ExitPlan e visão geral calculam limites diferentes para a mesma pessoa
- **Arquivo:** `service/future/ExitPlan.java:54-58` vs `service/FutureService.java:118-119` e `:155`
- **Problema:** a visão geral usa `saldo − envelopes` e só compromissos fixos. O ExitPlan usa o saldo **cheio** (sem descontar envelopes) e inclui `CARD_SPENDING`. O "novo limite" sugerido em CUT_DAILY pode passar do limite exibido e consumir envelopes. Além disso, CUT_DAILY grava `resultingLowest = 0` e `solves = true` fixos, sem rodar a projeção de novo, o que contraria o javadoc ("each tested by running the projection again"). Uma janela negativa depois da próxima renda continua lá.
- **Correção:** passe `balance − reserved` e o mesmo conjunto de eventos. Reprojete com o novo ritmo antes de marcar `solves`.

### F7 [Média] O limite da visão geral ignora os gastos no cartão que o usuário confirmou
- **Arquivo:** `service/FutureService.java:118-119` (usa `planFixed`, que não traz `cardSpending`)
- **Problema:** as compras diárias no cartão confirmadas viram fatura antes da próxima renda, mas não entram no limite. O "posso gastar X/dia" sai otimista.
- **Correção:** inclua `CashEventPlanner.cardSpending(...)` nos eventos do limite.

### F8 [Baixa] Custo do cheque especial chamado de "teto" sem IOF
- **Arquivo:** `service/future/ProjectionEngine.java:25` e `:84`
- **Problema:** a taxa diária (1,08^(1/30) − 1 = 0,0025687) está correta para o teto da Res. CMN 4.765/2019. Falta o IOF (0,0082% ao dia + 0,38% fixo), então o valor chamado de teto no e-mail não é teto de verdade.
- **Correção:** some o IOF ou troque o texto para "juros (sem IOF)".

### F9 [Baixa] Compra "no cartão" cai no à vista sem avisar
- **Arquivo:** `service/future/ScenarioSimulator.java:34-39`; `service/FutureService.java:421`
- **Problema:** com `cardId` inexistente ou sem cartão, `card = null` e a simulação vira à vista, mas a resposta do Pergunte continua dizendo `method = "CARD"`.
- **Correção:** devolva 422 `CARD_NOT_FOUND`, ou devolva o método efetivamente usado.

### F10 [Baixa] Heurística de recorrência agrupa transferências diferentes
- **Arquivo:** `service/FutureService.java:572-581`
- **Problema:** toda transferência P2P vira a contraparte única "Transferência enviada/recebida". Duas pessoas diferentes pagas por mês viram uma "recorrência", e o `DailySpending` soma PIX grandes e únicos (IPVA, TV) sem aparar extremos (`DailySpending.java:69`).
- **Correção:** use o IBAN ou nome do destino como contraparte e descarte extremos (ex.: acima do p95) na média diária.

### F11 [Baixa] Referência instável e carga redundante
- **Arquivo:** `CashEventPlanner.java:80`: `reference = "card-spending:" + days` pode colidir entre faturas.
- **Arquivo:** `FutureService.java:419` + `:305`: `ask()` carrega o contexto duas vezes (overview + simulate), e cada carga lê **todo** o histórico de transações (ver D1).

---

## 2. Insights / análise

### I1 [Alta] Chamada ao Gemini (até 20 s) dentro de transação aberta
- **Arquivo:** `service/InsightsService.java:64` (`@Transactional(readOnly=true)`) envolve `:76`, que chama `GeminiInsightNarrativeGenerator.java:72,78` (timeout de 20 s)
- **Problema:** cada relatório segura uma conexão do Hikari (10 por padrão) durante a chamada HTTP. Dez relatórios simultâneos com o Gemini lento esgotam o pool, e a API inteira para.
- **Correção:** monte o snapshot numa transação curta e chame a IA fora dela.

### I2 [Média] Transferência entre contas próprias do Open Finance conta como receita e despesa
- **Arquivo:** `service/InsightsService.java:119-124`
- **Problema:** o OF importa cada lado como Deposit/Withdrawal em carteiras diferentes (`from == to`). A "Transferência para reserva" (300/mês no sandbox) infla entradas e saídas. O `FutureService` trata esse caso (`twoWay`); o Insights não.
- **Correção:** reutilize a mesma regra de par interno (mesma contraparte nas duas direções e mesmo valor na mesma data).

### I3 [Média] Mês fechado em UTC, enquanto o Futuro usa America/Sao_Paulo
- **Arquivo:** `service/InsightsService.java:90-91`, `:114` (`ZoneOffset.UTC`) e `:47` (`Clock.systemUTC()`)
- **Problema:** movimentação das 21h às 23h59 do último dia do mês cai no mês seguinte. Os números do relatório divergem dos da tela Futuro.
- **Correção:** use `FutureService.ZONE` e o `Clock` injetado.

### I4 [Baixa] Narrativa local genérica
- **Arquivo:** `InsightsService.java:179-183`
- **Problema:** "Revise o mês com mais saídas" não diz qual mês, e o texto fala em "carteiras simuladas" mesmo com dados do OF. Não há divisão por zero no módulo (conferido).

---

## 3. Open Finance (sandbox) comparado ao padrão Open Finance Brasil

### O1 [Crítica, no modo participant-sandbox] Todos os usuários importam a mesma conta bancária
- **Arquivo:** `service/openfinance/ParticipantSandboxOpenFinanceGateway.java:70` (`fetch(userId)` ignora `userId`) e `:43-44`, `:139` (um único `access-token` e um único `consent-id` globais)
- **Exploração:** com `open_finance_mode=participant-sandbox`, qualquer usuário que valide o código de e-mail recebe o saldo e o extrato da conta configurada.
- **Correção:** token e consentimento **por usuário** (tabela de consentimento com tokens cifrados), obtidos pelo fluxo de autorização. Até lá, bloqueie o modo para mais de um usuário.

### O2 [Alta] Não existe consentimento no sentido OFB
- **Arquivos:** `domain/enums/OpenFinanceConnectionStatus.java` (só `AUTHORISED` e `REVOKED`); `service/OpenFinanceService.java:25-29`
- **Distâncias do padrão:**
  - Não há `POST /consents` com `permissions` + `expirationDateTime` + `loggedUser`, nem ciclo `AWAITING_AUTHORISATION → AUTHORISED | REJECTED` com `rejection.reason` (ex.: `CUSTOMER_MANUALLY_REVOKED`, `CONSENT_MAX_DATE_REACHED`). `CONSUMED` pertence aos consentimentos de pagamento, não aos de dados.
  - As `PERMISSIONS` fixas só listam contas, mas o import também lê cartão e empréstimo. Faltariam `RESOURCES_READ` (obrigatória), `CREDIT_CARDS_ACCOUNTS_READ`, `CREDIT_CARDS_ACCOUNTS_BILLS_READ`, `CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ`, `CREDIT_CARDS_ACCOUNTS_LIMITS_READ`, `LOANS_READ`, `LOANS_SCHEDULED_INSTALMENTS_READ` e `LOANS_PAYMENTS_READ`.
  - O "consentimento" é um código por e-mail da própria Wall.ia. No OFB a autorização acontece na instituição transmissora (redirect/FAPI).
- **Correção:** entidade `Consent` com esses campos e estados, e permissões derivadas dos recursos que realmente são lidos.

### O3 [Alta] A expiração do consentimento se renova sozinha a cada sync
- **Arquivo:** `service/openfinance/ContractSandboxOpenFinanceGateway.java:96` (`expiresAt = now + 90d` a cada fetch); `service/OpenFinanceImportService.java:73-74` (sobrescreve `authorisedAt` e `expiresAt` em todo import)
- **Problema:** o sync diário (`OpenFinanceDailySync`) empurra a expiração para frente todo dia, e o consentimento nunca expira. No OFB, `expirationDateTime` é fixada na criação (máx. 12 meses) e não se estende com a leitura. `authorisedAt` também deveria ser imutável. No modo participante, `expiresAt = null` (`ParticipantSandbox...:104`) significa "nunca expira" (`OpenFinanceService.java:167`).
- **Correção:** grave os dois campos só na criação do consentimento e rejeite a leitura quando expirado.

### O4 [Alta] Revogar não para o uso dos dados, e a carteira OF fica presa para sempre
- **Arquivo:** `service/OpenFinanceService.java:131-139`; `service/WalletService.java:247` e `:253-256`
- **Problema:** a revogação só marca REVOKED. Não chama `DELETE /consents/{id}` e não desativa as carteiras, cartões e empréstimos importados, que continuam somando no saldo e na projeção do Futuro. O usuário também não consegue excluir a carteira (`assertLocal` lança `OPEN_FINANCE_READ_ONLY`). Pela LGPD, revogar deve cessar o tratamento.
- **Correção:** ao revogar, marque os dados de origem OF como inativos (ou apague) e tire-os de `FutureService.load` e do Insights.

### O5 [Alta] Empréstimo quitado continua sendo projetado
- **Arquivo:** `service/OpenFinanceImportService.java:170-190` (só faz upsert), além de contas e cartões que somem no upstream
- **Problema:** o cartão faz o espelhamento das compras (`removeIf`), mas o empréstimo que o banco deixa de informar fica com as parcelas restantes no futuro, e a projeção cobra parcelas que não existem mais.
- **Correção:** apague ou inative loans e cards cujo `externalId` não veio no snapshot da mesma instituição.

### O6 [Média] Tipos de transação e formato de valor fora do enum oficial
- **Arquivo:** `ContractSandboxOpenFinanceGateway.java:75-79` (`DEBITO_AUTOMATICO`) e `:83` (`COMPRA_DEBITO`)
- **Problema:** até onde conheço a Accounts API v2, esses valores não estão no enum de `type`. Débito automático de convênio é `CONVENIO_ARRECADACAO` e compra no débito é `CARTAO`. **Confirmar no swagger oficial.**
- **Outros pontos:**
  - `Entry` não tem `completedAuthorisedPaymentType` (`TRANSACAO_EFETIVADA` / `LANCAMENTO_FUTURO` / `TRANSACAO_PROCESSANDO`). Um lançamento futuro importado viraria movimento já liquidado.
  - `consentId` é um UUID determinístico; o OFB usa URN (`urn:<instituicao>:<id>`).
  - `CreditCardAccount.closingDay/dueDay` (`OpenFinanceSnapshot.java:56-57`) não existem na API de Cartões. O real vem de `/bills` (`dueDate`, `billTotalAmount`, `isInstalment`) e das transações (`chargeNumber`, `chargeIdentificator`, `billId`).
  - Em Loans, a taxa vem em `interestRates[].preFixedRate` como **fração** (0,2984). O modelo guarda percentual (`annualRate 29.84`), e o mapeamento real sairia 100× errado se não for convertido.
- **Correção:** alinhar enums e nomes ao swagger oficial e manter o sandbox gerado a partir dele.

### O7 [Média] O modo participante só lê contas, sem retry e com chamada HTTP dentro da transação
- **Arquivo:** `ParticipantSandboxOpenFinanceGateway.java:71-106`
- **Problema:** não lê cartões nem empréstimos, então a projeção no modo real não tem faturas nem parcelas. A paginação usa `meta.totalPages`; confirmar se o endpoint de transações da versão alvo usa `links.next` / `pagination-key`. `LocalDate.now(clock)` usa `Clock` UTC (`config/ClockConfig.java:77`), então entre 21h e 24h BRT o `toBookingDate` já é amanhã. Não há retry com backoff nem tratamento de 429.
- **Transação:** `OpenFinanceService.java:85-95` e `:141-142` executam `gateway.fetch` (N requisições de 12 s) com `SELECT ... FOR UPDATE` do usuário aberto.
- **Correção:** buscar fora da transação e importar numa transação curta.

### O8 [Média] A verificação de e-mail do OF acaba servindo de verificação de identidade do app todo
- **Arquivos:** `service/OpenFinanceService.java:164-168` (conexão ativa exige `isVerified`); `service/FutureAlertService.java:71`, `:83`, `:114` (alerta por e-mail depende da verificação do OF); `service/OpenFinanceEmailVerificationService.java:178` (validade de 90 dias fixa no código)
- **Efeitos:**
  1. A conexão OF cai 90 dias depois da verificação, qualquer que seja o consentimento.
  2. Pedir um novo código zera `verifiedAt` (`:106`) e derruba uma conexão ativa até a confirmação.
  3. Quem não usa OF não recebe alerta por e-mail.
  4. O SMTP roda dentro da transação com lock (`:111`).
- **Correção:** mover para `user.email_verified_at` no cadastro (o plano já citado nos docs), com um serviço próprio. O OF passa a exigir apenas "e-mail verificado".

### O9 [Baixa] N+1 no import
- **Arquivo:** `OpenFinanceImportService.java:98`: `existsByExternalId` roda uma vez por lançamento. Com 12 meses de extrato são centenas de SELECTs por sync, multiplicados pelo número de usuários no job diário.
- **Correção:** buscar de uma vez só os `externalId` existentes em lote (`findExternalIdsIn(...)`).

---

## 4. Integrações

### G1 [Média] Gemini sem retry nem backoff; resposta vazia volta para o fallback local sem aviso
- **Arquivo:** `service/insight/GeminiInsightNarrativeGenerator.java:78-88`
- **Problema:** o timeout existe (connect 8 s, request 20 s). 429 ou 503 vão direto para o fallback local, sem retry. O modelo `gemini-3.8-flash` existe e o contrato da Interactions API confere com a doc atual. Não há log do status HTTP (`InsightsService.java:82` registra só uma mensagem fixa), o que dificulta diagnosticar a queda.
- **Correção:** um retry com backoff e jitter para 429/5xx, e log de `status` + `model`.

### G2 [Média] Alerta por e-mail e push pode repetir todo dia
- **Arquivo:** `service/FutureAlertService.java:149-151`
- **Problema:** a deduplicação compara `window.start` exato. Como a projeção tem gasto diário estimado, o início da janela anda um dia para frente ou para trás conforme o usuário gasta, e cada mudança dispara um novo aviso.
- **Correção:** considerar a mesma janela se `|Δstart| <= 7 dias` ou se as janelas se sobrepõem.

### G3 [Média] Jobs agendados sem lock distribuído nem idempotência entre instâncias
- **Arquivos:** `service/OpenFinanceDailySync.java:33`; `service/FutureAlertService.java:98`
- **Problema:** com duas réplicas, os dois jobs rodam duas vezes (sync duplo, e-mail duplo). O sync é sequencial numa só thread (já sinalizado com `ponytail`). Não há métrica de execução além de um log.
- **Correção:** ShedLock ou `pg_try_advisory_lock` no início de cada job.

### G4 [Baixa] Rate limiters em memória
- **Arquivos:** `security/AuthenticationRateLimiter.java:31`, `service/insight/InsightRateLimiter.java:18`, `service/ask/AskUsage.java:28`
- **Problema:** zeram no restart e não são compartilhados entre réplicas. Aceitável para uma instância só; documentar isso.

### G5 [Baixa] Push
- **Arquivo:** `service/push/PushService.java:34-35`, `:163-169`: a allowlist de hosts contra SSRF está correta e a assinatura VAPID tem timeout de 8 s.
- **Observação:** `:85-96`: quem conhece o `endpoint` de outro navegador consegue "tomar" a inscrição. O risco é baixo, porque o endpoint é um segredo de fato.

---

## 5. Rotas e segurança

### S1 [Média] Status HTTP errado para erros de regra
- **Arquivo:** `exception/GlobalExceptionHandler.java:66-68`
- **Problema:** todo `WalletRuleException` vira **400**, inclusive `GOAL_NOT_FOUND` e `ENVELOPE_NOT_FOUND` (`FutureService.java:390,396`, que deveriam ser 404), `RECURRENCE_NOT_FOUND` (404), `GOAL_LIMIT`/`ENVELOPE_LIMIT`/`PUSH_LIMIT` (409 ou 422) e `OPEN_FINANCE_READ_ONLY` (403 ou 409). `InsufficientFundsException` vira **412** (`:149-153`), mas o certo seria 422.
- **Correção:** um código HTTP por código de erro (mapa no handler ou campo no exception).

### S2 [Média] Endpoint de lista sem paginação
- **Arquivo:** `controller/TransactionController.java:57-61`
- **Problema:** `new PageImpl<>(findAllByUserId(userId))` carrega todo o histórico e só embrulha num Page falso.
- **Correção:** receber `Pageable` e paginar na query.

### S3 [Baixa] JWT
- **Arquivo:** `security/JwtUtils.java:48-55`
- **Problema:** token de 1 h sem refresh, sem claim `type`, `iss` ou `aud`, e com `subject = username`. Não há revogação no logout nem na troca de senha. Ponto positivo: o segredo é validado no startup (`:37-39`).
- **Correção:** incluir `iss`, `aud` e `ver` (versão de credencial do usuário) e rejeitar tokens de versão antiga.

### S4 [Baixa] Outros achados de segurança
- `config/SecurityConfig.java:80`: CSRF desabilitado. Aceitável porque a autenticação é Bearer em header e o CORS tem origem explícita com `allowCredentials=false` (verificado ao vivo: origem estranha recebe 403).
- `application-dev.yml:4`: `org.springframework.security: debug` loga headers e o fluxo de autenticação em dev.
- `service/WalletService.java:142-149`, `:234`, `:250`: logam IBAN + saldo em INFO.
- **IDOR:** não achei nenhum. Toda rota com id usa `@PreAuthorize("@walletAccess...")` ou filtra por `userId` na query (`findByIdAndUserId`), e "não existe" e "não é seu" dão a mesma resposta (403).
- **Segredos:** `.env.properties` está no `.gitignore` e não é rastreado (`git ls-files` confirmou). Os compose leem de env, e as portas do banco e do mailpit ficam presas em 127.0.0.1. Não li os valores do `.env.properties`.

---

## 6. Banco e migrações

### D1 [Média] Sem índice nas FKs de `transaction`; a consulta principal faz seq scan
- **Arquivo:** `resources/db/migration/V1__db_init.sql:91-94` (FKs sem índice); `repository/TransactionRepository.java:22-27` (`JOIN wallet w ON w.id IN (from,to)`)
- **Prova:** `EXPLAIN` no Postgres descartável mostrou `Seq Scan on transaction t` com `Join Filter: ((w.id = t.from_wallet_id) OR (w.id = t.to_wallet_id))`.
- **Onde pesa:** a consulta roda em toda chamada de `FutureService.load` (visão geral, pergunta, simulação, alerta diário de cada usuário) e no Insights, **sem filtro de data**.
- **Correção:** migração V23 com `CREATE INDEX CONCURRENTLY` em `transaction(from_wallet_id, created_at)` e `(to_wallet_id, created_at)`; reescrever a query como `UNION` e limitar a janela (ex.: 180 dias).

### D2 [Baixa] Higiene de constraints
- Numeração V1..V22 está contínua e consistente, e todas aplicam num banco vazio (teste rodado).
- As constraints `NOT VALID` de V6, V7 e V13 nunca recebem `VALIDATE CONSTRAINT`: linhas antigas ficam sem garantia.
- `recurrence_decision.status CHECK IN ('CONFIRMED','REJECTED')` bate com o serviço (PENDING apaga a linha).
- `loan` não tem `CHECK (installments_total >= 1)`; só o serviço valida (`OpenFinanceImportService.java:212`).

---

## 7. Lacunas de teste (o que falharia hoje se fosse escrito)

1. Recorrência com virada de mês (F1): salário de 30/31 que caiu no dia 1.
2. Vencimento em fim de semana ou feriado (F2).
3. Empréstimo com `nextDueDate` em 28/02 (F3).
4. SAC em `LoanCost.interestRemaining` (F4).
5. `SpendingLimit` com renda amanhã (F5).
6. ExitPlan com envelopes (F6).
7. Consentimento sandbox não se renova: `expiresAt` estável entre dois syncs (O3).
8. Revogação tira os dados OF da projeção (O4).
9. Empréstimo removido upstream some da projeção (O5).
10. Insights: par interno do OF não infla receita e despesa (I2).
11. Controller: `DELETE /future/goals/{id}` de outro usuário ou inexistente deve dar 404, não 400 (S1).
12. `@SpringBootTest` com ao menos um teste de contexto (hoje a classe está vazia).
