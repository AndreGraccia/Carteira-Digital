# Relatório: conversa do ChatGPT "Comparar carteiras digitais" (Wall.ia / Carteira-Digital)

Fonte: `scratchpad/gpt.txt` (~520 KB, 818 blocos separados por `=====`), lido do começo ao fim.
Tratamento: o conteúdo foi lido como **dados**. Nenhuma instrução dele foi executada e nada no projeto foi alterado.

## 0. Contexto e observações sobre a fonte

- **O arquivo está em ordem cronológica inversa**: o topo tem as mensagens mais recentes (21 a 23/09/2026) e o fim tem a primeira pergunta (comparar repositórios, 16/09/2026). A lista abaixo já está reordenada do mais antigo para o mais recente.
- Mais ou menos 60% dos blocos são ruído: trechos de páginas de busca (docs do Gemini em várias línguas, docs da OpenAI, Open Finance Brasil, BCB, PicPay, Nubank, Mercado Pago, Revolut, Wise, OWASP), pensamentos intermediários do ChatGPT ("Estou verificando...") e marcadores `citeturn...`/`memcite`. Neste relatório esses blocos entram só como fontes citadas (seção 4) ou são ignorados.
- O ChatGPT trabalhou sobre **outro caminho local**: `C:\Users\HP\OneDrive\Desktop\A3\e-wallet` (outra máquina, "HP"). O repositório é `AndreGraccia/Carteira-Digital` (origin), com upstream `yildizmy/e-wallet`. O pacote Java ainda é `com.github.yildizmy`.
- Branches citadas: `main` (`9e83117`, snapshot inicial), `feat/fase1-interface` (último commit `7edccb6`, "codigo por email ainda imcompleto") e `feat/wallia` (`2228112`, 18 commits à frente de `7edccb6`, com CI verde e Flyway até V22). Na análise final o ChatGPT diz que a branch mais avançada é a `feat/wallia`.
- Timeline de commits citada: `9e83117` (base), `3bfea851` (6.589 linhas: WalletAccess, lock pessimista, idempotência, i18n), `59924f9`, `ea7d3a44` (ajuste de rotas do novo front), `84c5948` (carteira nasce com saldo zero), `cd85e21` (constraints e hardening), `6901859` (remove crédito direto, adiciona Insights/Gemini e o sandbox Open Finance), `9d8ec73`, `710f4b0` (verificação por e-mail/2FA do Open Finance) e `7edccb6`.

---

## 1. Perguntas do usuário (cronológica)

Os textos entre aspas foram copiados do arquivo. Os itens marcados *(inferido)* não têm a mensagem do usuário no arquivo; a pergunta foi deduzida pela resposta do ChatGPT.

1. "compare essa wallet [github.com/yildizmy/e-wallet] com essa atualização [github.com/AndreGraccia/Carteira-Digital]"
2. *(inferido)* Avisa que existe um segundo commit (na branch `feat/fase1-interface`, não na `main`) e pede para refazer a comparação.
3. *(inferido)* Pede para começar a "Parte 1" (vazamento de dados de carteira em `TransactionResponse`).
4. *(inferido)* Manda um print do front com "Não foi possível conectar ao servidor". Depois manda outro print com erros de i18next, Docker e Maven rodados dentro de `frontend/`.
5. "análise o novo commite veja erros de segurança de regra de negócio com base em carteiras digitais e me mostre oque pode deixar esse projeto com cara de comercial no lugar de projeto acadêmico"
6. *(inferido)* Pede para começar a primeira correção: impedir saldo inicial arbitrário. Seguem prints com o erro de `final Wallet wallet` duplicado, ESLint (`import/newline-after-import`, `jest is not defined`), testes do `WalletServiceTest` quebrados e 3 testes falhando no `BasicTabs.test.js`. Pergunta também como commitar e se o app funcionar mas o teste falhar está ok.
7. *(inferido)* Pergunta o que pode ser excluído do repositório (arquivos mortos) e depois como excluir pelo CMD.
8. "de uma olhada em todos os commites ate agora e veja oque ainda precisa srr melhorado e como conectar o gemini api em passos"
9. *(inferido)* Pede ajuda para criar a chave Gemini pelo navegador. O ChatGPT abre um chat em "Work mode".
10. "faça uma nova analise agora vamos utilizar isso como aprendizado você vai me ensinar a codar [...] veja outras carteira digitais o banco central [...] muito profissional como se fosse para venda mas com dados simulados de teste open finance criação de agente pode ser gemini ou open ai [...] contas admin que realmente são admins [...] regras de negocio e soluções"
11. *(inferido)* Pede uma ideia diferenciadora de produto. A resposta foi "Gêmeo Financeiro" + "Modo Futuro".
12. "como podemos fazer isso sem que o usuario precise nos passar muitas informações pelo open finance coletarmos suas fontes de renda quanto ele recebe por mes quanto gasta pra onde vai o dinheiro oque é essencial [...] seria legal se pudessemos ver o que vem na fatura do mes que vem"
13. *(inferido)* Pede um roadmap completo, tratado como produto profissional e com ensino passo a passo.
14. "quero que tudo isso seja muito simples para o usuario nada dificil apenas perguntas sobre oque ele conhece exemplos simples dicas faceis de entender, dividas bem explicas projeções faceis de entender"
15. "sim e mostrar esse gasto de 39 da netflix é fixo ou esse gasto de 1500 com aluguel sempre exclarecer de onde vem e pra onde foi em toda informaçao salario por exemplo esse receita de 2000 da concentrix s.a pode ser considerado salario não da pa definir algo sabando somente o valor [...]"
16. "beleza vamos para o passo 0, e aprender sobre tudo em passo a passo aprendendo e codificando tudo ao longo do tempo"
17. Manda as saídas de `git status`, `java -version` (Java 21.0.10), `mvn -version` e `mvn -f backend/pom.xml test` (69 testes, 11 erros de Mockito/SecureRandom). Depois manda "Tests run: 0", depois o erro de compilação em `setUp:61` ("Select-String [...] retornou coisas") e depois o NPE na linha 74 (10 de 11 testes passando).
18. *(inferido)* Pede um resumo textual da ideia do produto. A resposta foi o texto "Nossa ideia é transformar a carteira digital [...]".
19. *(inferido)* Pergunta se o projeto pode virar app Android/iOS. Depois pergunta como começar o mobile e se o Codex consegue gerar o mobile a partir da versão web.
20. *(inferido)* Pergunta qual é a conta de teste documentada no projeto. A resposta cita `otpana2026` e `otpbruno2026` com o Mailpit (`localhost:8025`); a senha não foi reproduzida aqui.
21. *(inferido)* Pede para reanalisar o GitHub remoto. É quando o ChatGPT descobre a `feat/wallia`.
22. "faça em fases as alterações necessárias e quero separar remover a verificação de email apenas deixala pra cadastro monte um passo com a criacao de api key gemini projeto e ai studios gratis ja conectado e revise os erros e faca sujestao de melhoria de segurança desempenho e validações"
23. As três últimas "perguntas" (revisão de segurança/desempenho, passo a passo da chave Gemini com `gemini-3.8-flash`, remoção da verificação de e-mail do Open Finance) são **sugestões automáticas de interface** do ChatGPT. Não há evidência de que o usuário as enviou.

---

## 2. Erros e bugs apontados, com localização

### 2.1 Integração, rotas e ambiente
| Bug | Local | Status na conversa |
|---|---|---|
| O login original salvava `response.data.data`, mas o backend retorna o `JwtResponse` direto | `frontend/src/services/AuthService.js`; `AuthController`/`JwtResponse` | já corrigido na Carteira-Digital |
| `.env.properties` com segredo JWT/DB versionado no original | raiz | corrigido (`.env.example.properties` + `.gitignore`) |
| A migration V6 reusava os IDs 2 e 3 da tabela `type` (já eram Payment/Shopping): duplicate key, Flyway falha, Spring não sobe, o front mostra "Não foi possível conectar ao servidor" | `backend/src/main/resources/db/migration/V6__wallet_operation_types.sql` | corrigido para 4=Deposit, 5=Withdrawal, 6=Initial balance |
| Comandos rodados dentro de `frontend/` (`docker compose --env-file .env.properties`, `mvn -f backend/pom.xml`) não encontram os arquivos | terminal | orientação: rodar da raiz |
| `i18next`/`react-i18next` não instalados | `frontend/package.json` | `npm.cmd install` |
| O front aponta para `http://localhost:8080/api/v1` | config do front | diagnóstico |
| `ALLOWED_ORIGIN = "http://localhost:3000"` fixo no código | `Constants` | mover para configuração por ambiente |
| Tipo `Deposit` mudou de 2 para 4, mas o teste ainda esperava `2L` | `WalletServiceTest.deposit_setsAuthoritativeTypeAndSameWallet` | corrigido |
| Bloco `final Wallet wallet = walletRequestMapper.toWallet(request)` duplicado em `create()` | `WalletService.java` (linhas ~138 e ~142) | corrigido |
| ESLint: `import/newline-after-import` | `frontend/src/pages/auth/Signup.js`, `frontend/src/pages/wallet/NewWallet.js` (linha 12) | corrigido |
| ESLint: `jest/test/expect/beforeEach is not defined`. Adicionar `react-app/jest` no `package.json` não resolve porque `frontend/.eslintrc` tem `"root": true` | `frontend/.eslintrc` (precisa de `overrides` com `env.jest` para `**/*.test.js(x)`) | a própria IA corrigiu a orientação anterior |
| 3 testes falhando em `BasicTabs.test.js:26`: o `afterEach` chama `root.unmount()` com `root` indefinido e esconde o erro real | `frontend/src/pages/transfer/BasicTabs.test.js` | `afterEach` defensivo, `global` → `globalThis` |
| `WalletTransactionRequestMapper` sem uso depois da remoção do saldo inicial | `WalletService`, `WalletServiceTest`, `dto/mapper/WalletTransactionRequestMapper.java` | remover |
| `.mvn/wrapper/maven-wrapper.properties` da raiz removido | raiz | a IA primeiro mandou restaurar e depois se corrigiu (o wrapper certo está em `backend/.mvn/`) |
| Mockito não consegue mockar `java.security.SecureRandom` no Java 21 (Byte Buddy não acessa `jdk.internal.util.random`): 11 erros | `OpenFinanceEmailVerificationServiceTest.setUp:51` | resolvido com `VerificationCodeGenerator` + `SecureVerificationCodeGenerator`, removendo o bean `SecureRandom` do `ClockConfig` |
| `-Dtest=...` sem aspas no PowerShell gera "Tests run: 0 / No tests were executed" | comando mvn | usar `"-Dtest=OpenFinanceEmailVerificationServiceTest"` |
| "Unresolved compilation problem" em `setUp:61`: o construtor ainda recebia `secureRandom` | `OpenFinanceEmailVerificationServiceTest.java:61` | trocado por `codeGenerator` |
| NPE em `requestStoresOnlyHashAndSendsCodeToRegisteredEmail:74` (10 de 11 testes passando) | mesmo teste | **não resolvido na conversa**, ficou esperando as linhas 68 a 82 |
| Conflito de abordagem: a `feat/wallia` resolveu o mesmo problema com `FixedRandom extends SecureRandom`, então o `VerificationCodeGenerator` local vai conflitar | Git | orientação: não fazer reset nem trocar de branch sem preservar o trabalho local |
| Encoding quebrado ("Conex?o", "c?digo") | README do commit `7edccb6` | padronizar em UTF-8 |

### 2.2 Segurança
- **Criar dinheiro (P0):** `WalletService.create()` aceitava `balance` do cliente (`WalletRequest.balance`) e `POST /wallets/addFunds` fazia `balance += amount`. A primeira parte foi corrigida (`INITIAL_BALANCE_NOT_ALLOWED`). O `/addFunds` foi removido depois no `6901859` ("remove crédito direto").
- **Stack trace exposto (P0):** `application.yml` tinha `include-stacktrace: on_param`, `include-message: always`, `exception.trace: true`, e o `GlobalExceptionHandler` honra `?trace=true`.
- **Vazamento de dados da contraparte:** `TransactionResponse` embute `WalletResponse` (e dentro dele `UserResponse` com email, username e roles). O `WalletAccess.transaction()` deixa remetente e destinatário lerem a transação. A correção proposta é `TransactionWalletResponse {id, name}`.
- **DTOs de entrada com campos que deveriam ser do servidor:** `TransactionRequest` (`id`, `createdAt`, `referenceNumber`, `status`, `typeId`), `WalletRequest` (`id`, `iban`, `balance`, `userId`), `SignupRequest` (`id`, `roles`). O `AuthView.js` ainda envia `roles: ['ROLE_USER']`.
- **`userId` vindo do front** (`/wallets/users/{userId}`, `/transactions/users/...`). Está protegido por `@PreAuthorize("@walletAccess.user(#request.userId)")`, mas a recomendação é `/me`.
- **Senha passa por `.trim()`** em `AuthService.login()`.
- **JWT:** primeiro em `localStorage`, depois em `sessionStorage`. Continua exposto a XSS.
- **Idempotência opcional:** `requestId` pode ser `null` (`if (request.getRequestId() == null) return null;`).
- **`CascadeType.ALL`** de `Wallet` para as transações (`mappedBy = "fromWallet"`/`"toWallet"`) permite apagar histórico financeiro.
- **Logs com IBAN, saldo e valores** nos services.
- **Swagger público** (`/swagger-ui/**`, `/v3/api-docs/**` liberados no `SecurityConfig`).
- **Rate limit em memória** (`AuthenticationRateLimiter` e `InsightRateLimiter` com `ConcurrentHashMap`).
- **Migration V4** transforma os usuários 1 e 2 em ADMIN. `ROLE_ADMIN` tem `findAll()` de wallets e transações, um privilégio amplo demais. (A `feat/wallia` depois desativou os demos e tirou o ADMIN deles.)
- **Rate limiter do Gemini** consome a cota individual antes de saber se o orçamento global permite a chamada.
- **Migration V6 editada depois de criada** (risco de checksum no Flyway).
- **Dependências desalinhadas:** Spring Boot `3.0.4` com `spring-boot-starter-validation 3.2.5`, e `jjwt 0.9.1` antigo.
- **Identidade herdada:** `com.github.yildizmy`, `package.json` com `@minimal/material-kit-react` e `minimals.cc`.
- **Resposta de erro inconsistente:** `ErrorResponse` vs `Map.of("code", ...)`.
- **Constraints `NOT VALID`** na V6 (`wallet_nonnegative_balance`, `transaction_nonnegative_amount`) precisam de `VALIDATE CONSTRAINT` depois.
- **Mais de 40 KB e sem gate de cobertura:** CI sem gate de JaCoCo nem de segurança.

### 2.3 Regra de negócio, análise e predição
- **`LoanCost` (o erro que mais pesa):** calcula `installment × remaining − outstanding` e o comentário diz "Exact for ... Price or SAC". Isso não vale para SAC, onde as parcelas variam. Proposta: `AmortizationSystem {PRICE, SAC, OTHER, UNKNOWN}` com cronograma, ou a mensagem "não há dados suficientes".
- **Saque (`withdrawFunds()`):** o dinheiro "some" (transação da carteira com ela mesma, sem destino).
- **Depósito:** representado como `fromWallet = toWallet`, o que é semanticamente artificial.
- **Tipos misturados:** `Transfer/Payment/Shopping/Deposit/Withdrawal/Initial balance` misturam operação com categoria.
- **Sem moeda no domínio:** `Wallet` não tem `currency`, mas o front mostra R$.
- **`Status`:** o mapper sempre grava `SUCCESS`, então `PENDING`/`ERROR` não são usados.
- **Paginação falsa:** `TransactionRepository.findAllByUserId()` devolve uma `List`, que depois vira `new PageImpl<>(...)`. Os relatórios varrem todo o histórico e só depois filtram as datas. O `InsightsService` agrega em Java.
- **Faltam índices** `from_wallet_id + created_at` e `to_wallet_id + created_at`.
- **Arquivos grandes demais:** `FutureService.java` tem ~43 KB e `frontend/src/i18n.js` tem ~93 KB.
- **Uso de `double`** em cálculos financeiros críticos (recomendação genérica de evitar).
- **Validação de nome:** mínimo de 3 caracteres barra nomes legítimos de 2 letras.
- **IBAN gerado (`BR` + checksum)** parece uma conta bancária real. Deixar claro que é fictício.

### 2.4 E-mail e Open Finance (conceitual)
- **O código por e-mail foi tratado como "autenticação do Open Finance".** No OF real, a autenticação é feita na instituição transmissora (redirect).
- **`OpenFinanceEmailChallenge` faz dois papéis:** OTP de 10 minutos e prova de verificação por 90 dias. A conexão só fica ativa se o OTP continuar "verificado".
- **`FutureAlertService` depende de `OpenFinanceEmailVerificationService`** para decidir se pode mandar e-mail. É um acoplamento errado: o e-mail pertence à identidade do usuário.
- **`OpenFinanceConnectionStatus`** só tem `AUTHORISED` e `REVOKED`.
- **Falta teste de jornada** challenge → verify → consent → import → sync → revoke.

### 2.5 Gemini
- **`gemini_search_grounding=true` por padrão.** O ChatGPT diz que o free tier do `gemini-3.8-flash` não inclui Search grounding, e que o grounding aumenta custo e latência. Recomenda `false`.
- **Endpoint `/v1beta/interactions` em `application.yml`.** No fim a IA recomenda trocar para `/v1/interactions`, estável (ver seção 6).
- **Diagnóstico:** `provider: "LOCAL"` na resposta de `POST /api/v1/insights/reports` indica fallback (chave ausente, erro, timeout ou JSON inválido).

### 2.6 Layout e tradução (frontend)
- **Dois sistemas de layout:** o `DashboardLayout.js` novo convive com os legados `layouts/dashboard/header/`, `nav/` e `index.js`.
- **Resíduos do template:** `components/logo`, `nav-section`, `scrollbar`, `svg-color`, `hooks/useResponsive.js`, `_mock/account.js`, `sections/@dashboard/app/AppTasks.js` (com `console.log('EDIT'/'DELETE'/'SHARE')`), `AppWidgetSummary.js`, `TransactionListHead.js`, `WalletListHead.js`, barrels `index.js` e os assets `public/assets/images/{products,covers,avatars}` e `icons/navbar`.
- **Páginas mortas:** `pages/transfer/{AddFunds,WalletToWallet,WithdrawFunds}.js`, `pages/wallet/AddFunds.js`, `pages/auth/{LoginForm,SignupForm}.js`.
- **npm e Yarn ao mesmo tempo:** `package-lock.json` + `yarn.lock`, e scripts `re-start`/`re-build`/`clear-all` usando yarn e `rm -rf`.
- **Scripts temporários na raiz:** `.wallet-*.py/.cjs` e `.translation-runtime.cjs`. O `frontend/scripts/check-wallet.cjs` deve ser mantido.
- **i18n:** `i18n.js` monolítico. Proposta de dividir em `pt/future.js`, `pt/auth.js`, `pt/openFinance.js`, `en/...`.
- **Carteiras sem separação visual:** "Minhas carteiras" (local) e "Contas conectadas" (OF) aparecem juntas.

---

## 3. Recomendações e planos propostos

### 3.1 Plano final (última resposta, sobre a `feat/wallia`): fases 0 a 10
- **Fase 0, base Git:** verificar `git status` sem destruir nada local. Branch `refactor/signup-email-verification` a partir de `feat/wallia`. PR `feat/wallia → main`, proteger `main` exigindo CI.
- **Fase 1, e-mail no cadastro:**
  - `User.emailVerifiedAt`.
  - `SignupRequest` sem `id` e sem `roles`.
  - O signup cria o usuário com `emailVerifiedAt = null`, envia código de 8 dígitos e retorna `verificationId`, `maskedEmail`, `expiresAt`, `resendAvailableAt`.
  - Endpoints `POST /api/v1/auth/signup`, `POST /api/v1/auth/email/verify` e `POST /api/v1/auth/email/resend`.
  - Mantém hash, expiração, cooldown, limite de tentativas e limite diário. O código é consumido uma única vez.
  - Sem JWT para usuário não confirmado. `UserDetailsImpl.isEnabled()` passa a refletir a confirmação.
  - No front, a tela "Enviamos um código para a***@email.com".
- **Fase 2, migration:**
  - Não alterar V9, V10 nem V12. Criar `V23__signup_email_verification.sql`.
  - Renomear `open_finance_email_challenge` → `email_verification_challenge` e apagar os desafios antigos.
  - Adicionar `email_verified_at` ao usuário. Contas ativas preexistentes ficam marcadas como verificadas.
- **Fase 3, remover o OTP do Open Finance:**
  - Remover `OpenFinanceEmailVerificationService`, `OpenFinanceCodeRequest`, `/connection/challenge`, `/connection/confirm`, o estado `PENDING_EMAIL_VERIFICATION`, o modal de código em `OpenFinance.js` e as traduções relacionadas.
  - Fluxo que fica: `GET /api/v1/open-finance/status`, `POST /api/v1/open-finance/connection` (consentimento simulado + primeira importação), `POST /api/v1/open-finance/sync`, `DELETE /api/v1/open-finance/connection`.
  - `OpenFinanceConnectionStatus` = `PENDING/AUTHORISED/EXPIRED/REVOKED/ERROR`.
- **Fase 4, alertas:** tirar `OpenFinanceEmailVerificationService` do `FutureAlertService`. A regra passa a ser `user.emailVerifiedAt != null`. O Mailpit continua para testar cadastro e alertas.
- **Fase 5, Gemini:**
  - Trocar `/v1beta/interactions` por `/v1/interactions`.
  - Criar a chave em aistudio.google.com/apikey num projeto `wallia-dev` (ou `e-wallet-gemini-dev`). Usar as "auth keys" novas, porque as chaves antigas seriam rejeitadas.
  - `.env.properties` com `gemini_api_key=...`, `gemini_model=gemini-3.8-flash`, `gemini_search_grounding=false`.
  - Nunca colocar a chave no React (`REACT_APP_*`) nem no Git; conferir com `git check-ignore .env.properties`.
  - Teste direto por `Invoke-RestMethod` com o header `x-goog-api-key`.
  - Manter `store=false`.
  - O free tier pode usar os dados para melhorar produtos, então nada de dados reais de cliente.
- **Fase 6, segurança:**
  - Tirar `userId`, `balance` e `iban` do `WalletRequest`, e `createdAt`, `referenceNumber`, `status` e `typeId` da transferência. Identidade vem do JWT.
  - Migrar para `/api/v1/me/...`.
  - Idempotência obrigatória.
  - Remover `CascadeType.ALL` e trocar deleção por estados `ACTIVE/FROZEN/CLOSED`.
  - Rate limit em Redis (quando houver várias instâncias).
  - Access token curto + refresh token protegido.
  - Resposta genérica no reenvio de código, contra enumeração de contas.
- **Fase 7, desempenho:**
  - Consultas filtradas no banco e paginação real.
  - Índices `from_wallet_id+created_at` e `to_wallet_id+created_at`.
  - Dividir o `FutureService` em `ProjectionService`, `RecurrenceService`, `GoalService`, `EnvelopeService`, `ScenarioService`, `LoanAnalysisService` (e `CommitmentService`).
- **Fase 8, validações financeiras:** corrigir `LoanCost` (SAC). `requestId` obrigatório. Limites validados também no banco. DTOs separados para criação e alteração. Nome com `@NotBlank` + máximo, sem mínimo de 3.
- **Fase 9, IA endurecida:**
  - Allowlist de modelo e limite de saída.
  - Métricas de latência e falha.
  - Retry só para 429/5xx com backoff, e circuit breaker.
  - `interface AiProvider { AgentResponse execute(AgentRequest) }` com `GeminiAiProvider` e `OpenAiProvider` (`ai_provider=gemini|openai`).
  - Rate limiter com "aquisição" que só consome cota quando a chamada vai mesmo acontecer.
- **Fase 10, testes:**
  - Cadastro → código → confirmação → login. Código expirado, código reutilizado, limite de envio, troca de e-mail, usuário não confirmado.
  - OF sem e-mail, sync e revogação. Alerta só para e-mail confirmado.
  - Gemini ligado e desligado, fallback, 429, JSON inválido.
  - Idempotência concorrente e Flyway do zero.
- **Prioridades declaradas:** tirar o e-mail do OF e desligar o grounding. Próximo checkpoint: apenas a Fase 0 e o começo da Fase 1.

### 3.2 Plano "Baseline 1.0" (análise da `feat/wallia`)
- **Checklist do Baseline:** Git organizado, main atualizado, CI obrigatório, cálculos validados, consentimento OF separado, idempotência obrigatória, histórico não apagável, APIs `/me`, DTOs seguros, testes verdes.
- **Ordem depois do Baseline:** base confiável → Modo Futuro → agente → mobile.
- **Entidade `OpenFinanceConsent`:** `id`, `userId`, `institutionId`, `institutionName`, `status`, `grantedAt`, `expiresAt`, `revokedAt`, `purpose`, `permissions` (`ACCOUNTS_READ`, `BALANCES_READ`, `TRANSACTIONS_READ`), `externalConsentId`, `lastSyncedAt`.
- **Entidade `EmailVerificationChallenge`:** `id`, `userId`, `codeHash`, `createdAt`, `expiresAt`, `attemptCount`, `maxAttempts`, `resendAvailableAt`, `consumedAt`.
- **DTOs:** `CreateWalletRequest{name}`, `RenameWalletRequest{name}`, `TransferRequest{fromWalletId, toWalletId, amount, requestId, description}`. Nas versões anteriores apareciam também `DepositRequest` e `WithdrawRequest`, e o header `Idempotency-Key`.
- **Contrato:** OpenAPI, cliente compartilhado e TypeScript.

### 3.3 Roadmap longo (fases 0 a 15, "engenharia de produto")
0. Estabilização (0.1 Git, 0.2 backend, 0.3 frontend, 0.4 Flyway, 0.5 limpeza, 0.6 npm, 0.7 encoding, 0.8 deps, 0.9 CI, 0.10 baseline).
1. Consentimento OF.
2. `/me` + DTOs.
3. Idempotência.
4. Ledger de dupla entrada: `LedgerTransaction` e `LedgerEntry`, soma das entradas = 0, `REVERSAL` em vez de delete, `balance` só como cache, conta de liquidação/settlement para depósito e saque, estados `PENDING/AUTHORIZED/PROCESSING/SETTLED/FAILED/CANCELED/REVERSED`.
5. Modelo OF normalizado: `FinancialAccount`, `CreditCard`, `CreditCardBill`, `CreditCardTransaction`, `CreditAgreement`, `Installment`, todos com `institution/source/currency/externalId/lastSyncedAt`. Sandbox com as instituições fictícias "Banco Aurora" e "Banco Horizonte".
6. `RecurringPatternService`: `RECURRING_INCOME`/`RECURRING_EXPENSE`, `confidence`, `evidenceCount`, feito em Java e sem IA.
7. Fatura futura projetada.
8. `FinancialProjectionService`: 30/90/365 dias e menor saldo do período.
9. Radar Financeiro.
10. `FinancialGoal`.
11. Modo Futuro: gasto único, renda extra, gasto mensal. Depois `Scenario`/`ScenarioEvent`.
12. Agente Gemini com ferramentas `getCashFlow`, `getUpcomingBills`, `getIncomeSummary`, `getRecurringExpenses`, `getGoals`, `simulateScenario`, `getConsentStatus`.
13. Provider OpenAI.
14. Admin + auditoria: papéis `USER/SUPPORT/OPERATIONS/AUDITOR/ADMIN/SYSTEM`, "admin não movimenta dinheiro", console `/admin`, `AuditEvent{actor, actorRole, action, targetType, targetId, ip, userAgent, createdAt, metadata}` append-only, acesso sensível com motivo e prazo.
15. Segurança/Redis/Testcontainers/Playwright E2E/observabilidade (`requestId`, `correlationId`, logs estruturados sem OTP nem token).

Outros itens deste roadmap:
- **Documentação** em `docs/` (architecture, financial-model, ledger, security, ...) e ADRs: ADR-001 ledger, ADR-002 IA não movimenta saldo, ADR-003 OF read-only, ADR-004 Gemini atrás de `AiProvider`.
- **Critério de pronto:** regra, backend, validação, autorização, testes, front, erro, i18n, docs, build e commit limpo.
- **Commits pequenos** com prefixos `feat:`, `test:`, `docs:`.

### 3.4 Modelagem de proveniência ("Explainable Finance Engine")
- `NormalizedTransaction{amount, direction, occurredAt, sourceInstitution, sourceAccount, counterpartyName, transactionName, merchantName, merchantCategoryCode, source, externalId}`.
- `TransactionClassification{transactionId, type, confidence, source(SYSTEM_INFERENCE/USER_CONFIRMED), userConfirmed}`. A confirmação do usuário vence a inferência.
- `DetectedFinancialPattern{type, estimatedAmount, recurrence, confidence, evidenceCount, firstSeenAt, lastSeenAt, userConfirmed}`. Confiança alta entra na projeção automaticamente, média gera uma pergunta, baixa só entra depois de confirmada.
- Recorrente ≠ fixo ≠ essencial, e só o usuário decide o que é essencial. Transferência entre contas próprias (`INTERNAL_TRANSFER`) não conta como renda nem como gasto.
- MCC classificado por tabela determinística. A IA só explica.

### 3.5 Recomendações já executadas ou em andamento na conversa
- Carteira nasce com saldo 0 e sem transação "Initial balance" (commit sugerido: `fix: impede saldo inicial arbitrario em carteiras`).
- `TransactionWalletResponse{id, name}` + `TransactionResponseMapper.toTransactionWalletResponse(Wallet)`. No front, `wallet-statement/index.js` compara por `id` em vez de IBAN. Teste `TransactionResponseMapperTest.shouldNotExposeSensitiveWalletData`. Commit: `fix: limita dados de carteiras em respostas de transacao`.
- `VerificationCodeGenerator` (interface) + `SecureVerificationCodeGenerator` (`@Component`, faixa 10.000.000 a 99.999.999).
- Limpeza de arquivos em lotes: lixo evidente primeiro, depois layout legado, depois assets. Usar `git rm` e manter as migrations antigas.
- Profiles `application-dev.yml`/`application-prod.yml` com stacktrace desligado em produção.
- Renomear o pacote para `com.github.andregraccia.carteiradigital` ou `dev.andregraccia.wallet`, mantendo os créditos MIT.
- Refatoração em `WalletOperationService`, `IdempotencyService` e `WalletAccessService`.
- Formato de erro único `{status, code, message, fieldErrors}`.
- TOTP/reauth para transferências acima de R$ 1.000.
- Mobile: React Native + Expo em `mobile/`, eventualmente monorepo `apps/web`, `apps/mobile`, `packages/api-client|types|validation`. `AGENTS.md` com regras para o Codex. Tokens em Keychain/Keystore. Só depois do backend estável.

---

## 4. Referências regulatórias e documentação citadas

### 4.1 Banco Central (normas e FAQs)
- Resolução BCB 51/2020 (autorização de funcionamento): https://www.bcb.gov.br/estabilidadefinanceira/exibenormativo?numero=51&tipo=Resolu%C3%A7%C3%A3o+BCB
- Resolução BCB 526, de 3/12/2025 (altera a Circular 4.015, escopo de dados e serviços): https://www.bcb.gov.br/estabilidadefinanceira/exibenormativo?numero=526&tipo=Resolu%C3%A7%C3%A3o+BCB
- Resolução BCB 541, de 18/12/2025 (altera a Res. BCB 406/2024, compartilhamento do serviço de iniciação): https://www.bcb.gov.br/estabilidadefinanceira/exibenormativo?numero=541&tipo=Resolu%C3%A7%C3%A3o+BCB
- Resolução Conjunta nº 5 (interoperabilidade no OF): https://www.bcb.gov.br/estabilidadefinanceira/exibenormativo?numero=5&tipo=Resolu%C3%A7%C3%A3o+Conjunta
- IN BCB 720, de 2/4/2026 (Manual de Segurança do OF v5.0): https://www.bcb.gov.br/estabilidadefinanceira/exibenormativo?numero=720&tipo=Instru%C3%A7%C3%A3o+Normativa+BCB
- IN BCB 759, de 9/7/2026 (Manual de Escopo de Dados e Serviços v8.0): https://www.bcb.gov.br/estabilidadefinanceira/exibenormativo?numero=759&tipo=Instru%C3%A7%C3%A3o+Normativa+BCB
- IN BCB 760 (citada, sem conteúdo): https://www.bcb.gov.br/estabilidadefinanceira/exibenormativo?numero=760&tipo=Instru%C3%A7%C3%A3o+Normativa+BCB
- IN BCB 37 (manual de segurança do Open Banking), 178 (manual de experiência do cliente), 184 (dados mínimos da iniciação de pagamento), 371, 463 (lista a Res. Conjunta 1/2020, Circ. 4.015/2020, Res. BCB 32/2020 e LGPD 13.709/2018), 587 (dados opcionais), 725 (funcionalidades da API e jornadas). Mesmo formato de URL: `https://www.bcb.gov.br/estabilidadefinanceira/exibenormativo?numero=<N>&tipo=Instru%C3%A7%C3%A3o+Normativa+BCB`
- Busca de normas "open banking": https://www.bcb.gov.br/estabilidadefinanceira/buscanormas?conteudo=open+banking&tipoDocumento=Todos
- "Manual de Experiência do Cliente 9.0" (ambiente de gestão de consentimentos): citado sem URL. Verificar.
- FAQs do BC:
  - Tipos de dados compartilháveis: https://www.bcb.gov.br/meubc/faqs/p/tipos-de-dados-e-servicos-que-podem-ser-compartilhados-com-o-open-finance
  - Instituições participantes: https://www.bcb.gov.br/meubc/faqs/p/os-bancos-e-instituicoes-que-participam-do-open-finance
  - Benefícios: https://www.bcb.gov.br/meubc/faqs/p/beneficios-do-open-finance-que-podem-ajudar-a-sua-vida-financeira
  - IPs que precisam de autorização: https://www.bcb.gov.br/meubc/faqs/p/quais-instituicoes-de-pagamento-devem-ser-autorizadas-a-funcionar-pelo-banco-central
  - Contas de pagamento: https://www.bcb.gov.br/meubc/faqs/p/as-contas-de-pagamento-sao-mantidas-somente-pelas-instituicoes-de-pagamento
  - DDC do cartão: https://www.bcb.gov.br/meubc/faqs/p/documento-descritivo-do-credito-ddc-para-as-operacoes-com-cartao-de-credito
  - CCS: https://www.bcb.gov.br/meubc/faqs/p/tipos-de-instituicoes-obrigadas-a-registrar-as-informacoes-no-relatorio1
- **Conclusão regulatória usada pelo ChatGPT:** só instituições autorizadas pelo BC participam do OF, e conta de pagamento é serviço regulado. Por isso o produto deve se apresentar como simulador/sandbox/PFM, nunca como participante nem como banco.

### 4.2 Open Finance Brasil (openfinancebrasil.atlassian.net)
- **Contas:**
  - Informações Gerais v2.0.0: https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/17372398/Informa%2Bes%2BGerais%2B-%2BContas%2B-%2Bv2.0.0
  - v2.4.0: .../pages/512720918/...DC%2BContas%2B-%2Bv2.4.0
  - v2.4.0-beta.2: .../pages/458654038/...
  - v2.5.0-beta.2: .../pages/1323335697/...
  - PRD Contas v2.5.0: .../pages/1738965008/...
  - Changelogs: v2.4.1 (.../578584760/), v2.4.2 (.../979566781/), v2.1.0-rc.1 (.../157450418/), v2.0.0 (.../17379430/)
  - Regras de obrigatoriedade additionalInfo: .../1623195649/
- **Cartão de Crédito:**
  - v2.0.0: .../pages/17371584/
  - v2.1.0-rc.2: .../163513705/
  - v2.1.0-rc.3: .../193691666/
  - v2.3.1: .../450625557/
  - v2.4.0-beta.1: .../1130333059/
  - v2.4.0-beta.2: .../1323696145/
  - Changelogs v2.2.1 (.../310608068/) e v2.4.0-beta.2 (.../1323696361/)
  - Swagger v2.4.0: .../1739292701/
  - PRD v2.4.0: .../1739325456/
  - Documentação Negocial: .../1739325441/ (e o Draft .../DraftOF/pages/1736900609/)
  - Orientações: .../1739292685/ e Draft .../150077467/
  - [DA] Cartão v1.1.0: .../1719173325/
  - Pontos usados: `transactionName`, MCC, `billForecastDate` e "bills = faturas fechadas".
- **Empréstimos:** v2.6.0 (.../1672052753/), v2.4.0 (.../784728086/), v2.0.1 (.../17372685/), v1.0.4 (.../17373009/). Endpoints citados: `/contracts`, `/contracts/{id}`, `/payments`, `/scheduled-instalments`. Regras additionalInfo de crédito: .../1620181433/
- **Financiamento:** v2.4.0 (.../1267990659/, .../1267990643/), changelog (.../1267990804/), beta.1 (.../1101922322/), v1.0.4 e v2.0.0 (.../17373895/, .../17374072/). Direitos Creditórios v2.4.0: .../1268252691/
- **Consentimento / segurança:**
  - PRD API de Consentimento v3.4.0-rc1: .../2122743837/
  - Regras additionalInfo: .../1622474756/
  - Orientações Draft: .../DraftOF/pages/178061313/
  - FAPI-BR v2.1.0 (EN): .../1334149240/
  - FAPI-BR v2.2.0: .../1675395073/
  - FAPI-BR v2.2.1: .../1957625885/
  - Jornada CIBA v1.0.0-beta1 (citada sem URL)
- **Pagamentos automáticos:** Pix Automático (.../2065305489/), Liquidação longa duração (.../2065305764/), Retry (.../2065305788/), Regras de validação (.../1622933517/)
- Todas as URLs do OF acima têm o prefixo `https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/`.

### 4.3 Dados abertos BCB e contabilidade
- Datasets do BC:
  - https://dadosabertos.bcb.gov.br/dataset/26688-saldo-de-credito-por-origem-dos-recursos-e-modalidade-de-credito---mei---recursos-direcionado
  - https://dadosabertos.bcb.gov.br/dataset/27017-saldo-de-credito-do-mei-pessoa-fisica-por-modalidade-de-credito---antecipacao-de-fatura-de-ca
  - .../dataset/groups/25228-valor-total-das-transacoes-com-cartoes-de-debito
  - .../dataset/groups/25229-valor-total--das-transacoes-com-cartoes-de-credito
  - Ouvidoria
  - Organização "sistema-financeiro-nacional"
- Contabilidade e regulação citadas **só como trecho de busca, sem URL**:
  - COSIF 8.1.9.19.00.00-4, "(-) Despesas com serviços associados a transações de pagamento", base normativa IN499.
  - Circular 3.251/2004 (documento 5818).
  - CPC 05 (R1), Partes Relacionadas.
  - LC 105/2001 (sigilo bancário).
  - SCR.
- Não há recomendação contábil concreta além do "ledger de dupla entrada" e do "fechamento contábil" mencionado no roadmap.
- OWASP:
  - API Security Top 10 2023 (API1 BOLA, API3 BOPLA): https://api-security.owasp.org/editions/2023/en/0xa3-broken-object-property-level-authorization/
  - Cheat Sheets:
    - Logging: https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html
    - Transaction Authorization: https://cheatsheetseries.owasp.org/cheatsheets/Transaction_Authorization_Cheat_Sheet.html
    - Bot Management e Security Questions: aparecem só como ruído de busca.

### 4.4 Gemini / Google (as afirmações estão na seção 6)
- https://aistudio.google.com/apikey
- https://ai.google.dev/gemini-api/docs/generate-content/api-key?hl=pt-BR
- https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash
- https://console.cloud.google.com/cloud-resource-manager
- https://docs.cloud.google.com/resource-manager/docs/creating-managing-projects?hl=pt-br
- Endpoint: `https://generativelanguage.googleapis.com/v1beta/interactions`

---

## 5. Concorrentes e ideias de produto

### 5.1 Concorrentes citados
- **PicPay:** agrega contas via OF, gerencia autorizações (renovar/encerrar), adiciona dinheiro e paga via OF, tem seguro de carteira. URLs em meajuda.picpay.com e ajudaempresas.picpay.com (por exemplo https://meajuda.picpay.com/hc/pt-br/articles/4407855752083-O-que-%C3%A9-o-Open-Finance-e-como-ele-funciona-no-PicPay).
- **Mercado Pago:** OF, recursos de gestão e assistente (https://www.mercadopago.com.br/blog/open-finance-saiba-como-aproveita-lo-em-sua-conta-mercado-pago).
- **Nubank:** agrega via OF com recomendações personalizadas (2 milhões de autorizações) e a comunidade pede um "Minhas Finanças". URLs em blog.nubank.com.br, international.nubank.com.br e comunidade.nubank.com.br.
- **Revolut:** cartões virtuais, limites, bloqueios, alertas de segurança e contas em várias moedas (help.revolut.com).
- **Wise:** vários saldos e moedas.
- **Visa:** "AI Financial Assistant" para bancos, 14/07/2026 (https://investor.visa.com/news/news-details/2026/Visa-Introduces-AI-Financial-Assistant-Helping-Banks-Guide-Customers-from-Insight-to-Action/default.aspx).
- Citados só em resultados de busca: C6 Assistant, Atlas/"Elena", Kudos AI, MoneyWiz, SOMI, Organizze, Mobills, Minhas Economias, bots de WhatsApp.
- **Tabela do que copiar e do que não copiar:**
  - Copiar: agregador (OF), gestão de consentimento, histórico pesquisável, assistente, segurança configurável, centro de atividades, auditoria de dispositivos.
  - Deixar para depois: cartões virtuais, crédito, investimentos reais, câmbio (este só depois de modelar moeda).

### 5.2 Ideias de produto propostas
- **Posicionamento:** PFM white-label / sandbox / "laboratório financeiro", não "banco digital". Frases sugeridas: "uma carteira que permite experimentar o futuro antes de mexer no dinheiro", "Seu dinheiro tem um modo simulação".
- **Gêmeo Financeiro + Modo Futuro ("E se..."):**
  - Simulação contrafactual determinística e comparação de cenários (à vista vs 10x, perder renda, aluguel +15%).
  - Linha do tempo financeira com eventos hipotéticos.
- **Números úteis:**
  - "Saldo livre projetado" ou "Disponível planejado", com reserva protegida.
  - "Menor saldo do período", porque o timing importa: o mês pode fechar positivo e mesmo assim ficar negativo entre os dias 3 e 5.
  - "Quanto do próximo salário já está comprometido".
- **Radar de Compromissos** e **fatura futura projetada**, separando confirmado / detectado / projetado (`billForecastDate`, parcelas).
- **Onboarding quase sem formulário:** o sistema descobre renda, recorrências, faturas e dívidas; o usuário só confirma ("Tudo certo" / "Quero revisar").
- **UX simples:**
  - Perguntas em linguagem comum ("Esse valor é seu salário?").
  - Sem "score financeiro" opaco, com checklist no lugar.
  - Dois níveis de leitura, Resumo e Detalhes.
  - Botão "Por que identificamos isso?".
  - Regras: "Perguntar apenas o que não sabemos. Explicar apenas o que importa." e "Nenhum número importante aparece sozinho".
- **Privacidade visível:** tela "o assistente pode ver / não pode ver" e permissões do assistente. Minimização primeiro, provider depois.
- **Agente:** read-only por padrão, com fluxo Agent → Proposal (`TransferProposal`) → confirmação humana → backend revalida → ledger → audit. Regra "Java calcula, IA explica, usuário decide".
- **Decision Ledger:** histórico das simulações e decisões, e depois comparação do planejado com o realizado.
- **B2B:** "Financial Twin API" (`POST /simulations`).
- **Tela "Conexões e consentimentos"** por instituição (dados compartilhados, datas, expiração, revogar).
- **Notificações mobile**, por exemplo "fatura fecha em 3 dias" e "renovar conexão".

---

## 6. Afirmações duvidosas: verificar

1. **Modelos `gemini-3.8-flash` (GA/estável), `gemini-3.7-flash`, `gemini-3.6-flash`, `gemini-3.5-flash(-lite)` e `gemini-3.1-pro`: verificar.** Os trechos colados no arquivo dizem que existem (datas de junho e julho de 2026), mas os nomes e o status GA precisam ser conferidos no catálogo oficial antes de fixar o modelo no código. O mesmo vale para a desativação de `gemini-2.0-flash` em 1/6/2026 e para o acesso restrito ao 2.5.
2. **Interactions API: verificar.** A IA afirma que ela é "GA e recomendada desde jun/2026" e que existe um endpoint estável `/v1/interactions`. Numa resposta anterior ela disse que o endpoint correto era `/v1beta/interactions`, então há contradição interna. Conferir antes de trocar o endpoint.
3. **Auth keys: verificar.** A IA diz que chaves novas viram "auth keys" por padrão desde 28/05/2026 e que as chaves padrão antigas seriam rejeitadas a partir de setembro de 2026.
4. **"O free tier do Gemini 3.8 Flash não inclui Google Search grounding": verificar** na página de preços.
5. **"Interactions são armazenadas por padrão; `store=false` desativa": verificar.**
6. **Normas BCB: verificar** números, datas e versões de IN 759/2026 (Manual de Escopo v8.0), IN 720/2026 (Manual de Segurança v5.0), Res. BCB 526/2025, Res. BCB 541/2025 e "Manual de Experiência do Cliente 9.0". O arquivo traz trechos das páginas, mas não o texto inteiro.
7. **`billForecastDate`: verificar** em qual versão da API de Cartão ele foi adicionado (a IA cita a v2.4.x). O mesmo para "o endpoint `bills` retorna só faturas fechadas", que ela atribui a uma "documentação anterior".
8. **Lixo de busca (não são afirmações da IA), verificar se forem aproveitados:** nomes de modelos OpenAI ("GPT-5.6 Sol/Terra/Lu...") e "OpenAI Agents API, 10/09/2026". A afirmação "o Agents SDK é focado em Python e TypeScript" é plausível, mas também convém verificar.
9. **Explicação do Mockito vs `SecureRandom` no Java 21: verificar.** A causa (Byte Buddy antigo e módulo `jdk.internal.util.random` não exportado) é plausível. A solução com interface é boa, mas a própria IA reconheceu depois que a `feat/wallia` resolveu de outro jeito (`FixedRandom`).
10. **Orientações que a própria IA corrigiu depois:**
    - Restaurar `.mvn/wrapper` na raiz: errado, o wrapper certo fica em `backend/`.
    - `"react-app/jest"` no `package.json`: ineficaz por causa do `root: true` no `.eslintrc`.
    - "99% igual ao original": só valia para a `main`.
11. **`LoanCost` "exato para Price e SAC":** a IA está certa ao marcar como errado para SAC. Verificar se o código atual ainda tem esse comentário.
12. **Paths e nomes de outra máquina** (`C:\Users\HP\OneDrive\Desktop\A3\e-wallet`) e números de migration (V22, a próxima V23): verificar contra o repositório local atual (`Downloads\Wall_ia\Carteira-Digital`) antes de aplicar qualquer passo.
13. **Credenciais de teste** (`otpana2026`/`otpbruno2026`) aparecem no texto com senha. São contas de dev, mas convém confirmar que não estão em seed de produção.
