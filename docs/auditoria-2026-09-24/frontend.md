# Auditoria do frontend: Carteira-Digital (Wall.ia)

Data: 2026-09-24. Escopo: `frontend/src` (React 18 + MUI 5 + i18next), cruzado com `backend/src/main/java/com/github/yildizmy/controller`.
Modo: somente leitura. Nenhum arquivo do projeto foi alterado. O build foi gerado no scratchpad (`BUILD_PATH`).

## 0. Ferramentas (item 6)

| Comando | Resultado |
| --- | --- |
| `react-scripts build` | **OK**, "Compiled successfully", sem warnings de ESLint/webpack. Bundle `main.js` com 249,77 kB gzip e um único chunk de 2,6 kB. Não há code splitting por rota. |
| `eslint --ext .js,.jsx ./src` (`npm run lint`) | **OK**, exit 0, nenhum warning |
| `CI=true react-scripts test --watchAll=false` | **OK**: 5 suites, 19 testes passando, sem warnings de `act()`/console |
| `npm run test:i18n` | PASS |
| `npm run test:wallet` | PASS ("723 PT/EN keys; 462 literal translation references") |
| Aviso de ambiente | `caniuse-lite is outdated` (Browserslist), além de `DEP0176 fs.F_OK` vindo do react-scripts. Nenhum dos dois é defeito do app. |

Script próprio de cruzamento de chaves: `scratchpad/i18n-audit.cjs`, com saída em `scratchpad/i18n-audit.txt`. Ele carrega `resources` do `i18n.js` via `vm`, achata PT e EN e compara: chaves só num idioma, valores vazios ou placeholder, interpolação `{{x}}` divergente, EN com cara de PT e vice-versa, `t('...')` estático sem chave, `t(\`...\`)` dinâmico e chaves nunca referenciadas.

Resultado do cruzamento: **PT e EN têm exatamente as mesmas chaves, e as interpolações batem.** Nenhum `t('...')` estático aponta para chave inexistente. Os prefixos dinâmicos (`future.categories.${...}`, `ask.verdict.${...}`, `openFinance.status.${...}` etc.) foram conferidos contra os enums do backend (`CashEvent`, `RecurrenceDetector`, `QuestionParser.Intent`, `ScenarioSimulator`, `OpenFinanceConnectionStatus`, `Status`) e todos têm tradução.

Os problemas reais de tradução estão na **chave fantasma** `MuiTypography` e nos **textos que vêm prontos em PT do backend**. Veja a seção 1.

---

## 1. i18n / tradução

### I-1 · ALTA · Chave `openFinance.MuiTypography` com EN = `'.'` (alteração não commitada)
- **Onde:** `frontend/src/i18n.js:258` (pt) e `frontend/src/i18n.js:1159` (en). É o `git diff` pendente.
- **Problema:** a chave recebeu o nome do componente MUI (`MuiTypography`), o que indica que alguém copiou a classe do DOM ao tentar traduzir o parágrafo. O valor PT é quase literalmente o `notice` que o backend devolve em `OpenFinanceService.java:172`. O valor EN é um placeholder de um ponto. **Nenhum código lê essa chave** (grep: só aparece em `i18n.js` e em `theme/overrides/Typography.js`, onde é o override do tema). O texto que o usuário vê continua vindo cru do backend, então a tela em inglês segue mostrando português (ver I-2). Se alguém ligar essa chave como está, a UI em inglês vai exibir só ".".
- **Correção:** descartar a alteração e criar as chaves semânticas `openFinance.notice.contract` e `openFinance.notice.participant` nos dois idiomas. Em EN: "Local contract data aligned with the official Accounts API; it does not represent access to the official Directory." / "Sandbox environment of the configured participant institution.". Em `OpenFinance.js:254`, trocar `status?.notice` por `t(status.simulated ? 'openFinance.notice.contract' : 'openFinance.notice.participant')`.

### I-2 · MÉDIA · Textos em PT gerados no backend aparecem crus na UI em inglês
- **Onde (exibição):** `pages/open-finance/OpenFinance.js:254` (`status.notice`), `components/future-ui/index.js:83,108` (`event.label`, `event.origin`), `pages/ask/Ask.js:91` (`nextIncome.label`), `components/future-ui/index.js` (ExitPlan `action.label`).
- **Origem:** `backend/.../service/OpenFinanceService.java:172-173`, `service/future/CashEventPlanner.java:53,75,92` ("Meta", "Compras do dia a dia no cartão", "Gastos do dia a dia"), `service/future/ScenarioSimulator.java:33,74` ("Compra simulada", "Guardar todo mês"), `service/FutureService.java:431,571,577,581,805` ("Pergunta", "Depósito", "Retirada", "Transferência enviada/recebida", "Média dos últimos N dias", "Valor informado"), `service/OpenFinanceImportService.java:252`.
- **Problema:** com o idioma em EN, linhas do "Futuro", o "Perguntar" e o Open Finance misturam português e inglês. `EventRow` já sobrescreve o label de `DAILY_SPENDING` e `CARD_SPENDING`, mas os outros passam direto.
- **Correção:** o backend passa a devolver um código (`labelCode: "SIMULATED_PURCHASE"`, `originCode: "DAILY_AVERAGE"` + `days`), e o front traduz com `t('future.labels.${code}', { defaultValue: label })`. Nomes de contraparte (dado real) continuam crus.

### I-3 · BAIXA · ~60 chaves órfãs no catálogo (nunca referenciadas)
- **Onde:** `i18n.js`, blocos `transfer.*` inteiros (tela "Movimentar" removida, que hoje redireciona para `/envelopes` em `App.js:49`), `transactions.columns.*`, `transactions.types.*` (duplicam `app.types.*`), `dashboard.heading|wallets|users|totalAmount|monthlyTransactions`, `wallet.create.*`, `wallet.statement.*` (parte), `wallet.stack.walletCount_*` (só usado no script `check-i18n.cjs`), `common.save|noRecords|searchPlaceholder|openMenu|home|profile|settings|more|clear|open`, `commitments.nextPaymentValue`. Lista completa em `scratchpad/i18n-audit.txt`, seção "nunca referenciadas".
- **Problema:** o catálogo tem 1.839 linhas com texto morto, e quem traduz perde tempo com ele. `scripts/check-i18n.cjs` testa `wallet.stack.walletCount`, que a UI não usa.
- **Correção:** remover os blocos mortos, primeiro confirmando com `grep` no repo inteiro. Adicionar ao `check-i18n.cjs` a checagem de "chave não referenciada" do meu script.

### I-4 · BAIXA · PT idêntico a EN em rótulos de navegação
- `navigation.openFinance`, `openFinance.heading` e `pageTitle` são "Open Finance" nos dois idiomas. É nome oficial do programa do BCB, então está aceitável. Não é defeito, só registro.

### I-5 · BAIXA · Mensagem de erro hardcoded e engolida
- **Onde:** `services/AuthService.js:12`, `throw new Error('O servidor não retornou um token válido.')`.
- **Problema:** o texto não passa pelo catálogo. Pior: `AuthView` roda `errorKey(failure)`, que, sem `response`, devolve `app.errors.network`. O usuário lê "erro de rede" quando o problema é o contrato do login.
- **Correção:** lançar um erro com `code: 'INVALID_TOKEN'`, mapear em `utils/wallet.js:errorKey` e criar `app.errors.INVALID_TOKEN`.

### I-6 · BAIXA · Códigos de erro do backend sem mapeamento no front
- **Onde:** `utils/wallet.js:79-127`, com a lista de `errorKey`.
- **Problema:** `INVALID_PERIOD` (`AdminMetricsController.java:33`, `FutureController.java:95`), `INITIAL_BALANCE_NOT_ALLOWED` (`WalletService.java:120`) e `INTERNAL_ERROR` caem no genérico por status HTTP ("dados inválidos"/"erro no servidor").
- **Correção:** incluir os códigos na lista e criar as chaves correspondentes em `app.errors`.

---

## 2. Rotas

Todos os `to=`/`navigate()`/`href` internos (46 ocorrências) apontam para rotas que existem em `App.js`. Não encontrei link quebrado. Os redirecionamentos legados (`/transfers` e `/wallets/addFunds`) funcionam.

### R-1 · MÉDIA · Sessão expirada fora do WalletContext não redireciona para login
- **Onde:** `services/axios.js` (sem interceptor), `context/WalletContext.js:27-30` (o único lugar que trata 401), `PrivateRoute.js:6` (checa `exp` só no render da rota).
- **Problema:** se o JWT expira com o usuário em `/future`, `/insights`, `/ask` ou `/open-finance`, a chamada volta 401 e a tela mostra `app.errors.session` com o botão "Tentar novamente". O botão faz a mesma chamada, que falha de novo, sem nunca levar ao login.
- **Correção:** adicionar um interceptor de response em `services/axios.js` que, em 401, chama `AuthService.logout()` e dispara um evento (ou `window.location.assign('/login')` guardando o `from`). O tratamento local do `WalletContext` pode ser removido depois.

### R-2 · BAIXA · Guarda de papel admin só vale no front; o menu usa `roles` do `sessionStorage`
- **Onde:** `layouts/dashboard/DashboardLayout.js:44`, `ProtectedRoute.js:7`.
- **Problema:** não é falha de segurança (o backend tem `@PreAuthorize("hasRole('ADMIN')")`), mas quem editar o `sessionStorage` vê o menu e a tela de métricas vazia, com erro 403 genérico.
- **Correção:** tratar 403 em `Metrics.js` com uma mensagem própria, ou derivar `roles` do payload do JWT em vez do objeto salvo.

### R-3 · BAIXA · Filtros não vivem na URL
- **Onde:** `pages/transaction/Transaction.js:16-17` (`query` e `page` em `useState`), `pages/future/Future.js:92` (`horizon`), `pages/insights/Insights.js:58` (`months`).
- **Problema:** recarregar ou compartilhar a página perde a busca, a página, o horizonte 60/90 e o período. O filtro `wallet` já usa `?wallet=` (`Transaction.js:14`), então o padrão existe e só precisa ser estendido.
- **Correção:** mover `q`, `page`, `horizon` e `months` para `useSearchParams`.

### R-4 · BAIXA · Payload morto no signup
- **Onde:** `pages/auth/AuthView.js:50`, que envia `roles: ['ROLE_USER']`.
- **Problema:** o backend ignora o campo e força `ROLE_USER` (`service/AuthService.java:77`). O envio dá a impressão de que o cliente controla o papel.
- **Correção:** remover o campo do payload.

---

## 3. Integração HTTP (front × controllers)

Conferi as 36 chamadas do front: todas batem em URL e método com um endpoint real. Os payloads batem com os DTOs (`FutureRequests.*`, `InsightReportRequest`, `WalletRequest`, `OpenFinanceCodeRequest`, `LoginRequest`/`SignupRequest`). Um script cruzou todos os campos lidos nas respostas (`overview.*`, `limit.*`, `risk.*`, `result.*`, `report.*`, `status.*`...) contra os nomes de campo dos records/DTOs do backend, e **nenhum campo lido pelo front deixa de existir no backend**. Endpoints do backend sem uso no front: `GET /admin/audit`, `POST /future/alerts/run`, `GET /future/ask/metrics`, `GET /transactions/{id}`, `/references/{ref}`, `/wallets/iban/{iban}`, `PUT/DELETE /wallets/{id}`, `POST /wallets/transfer|withdrawFunds`. São recursos de admin ou legados, então não é defeito.

### H-1 · MÉDIA · `/transactions/users/{id}` sem paginação real; o front carrega tudo e pagina em memória
- **Onde:** `backend/.../TransactionController.java:59-60` (`new PageImpl<>(lista inteira)`), `frontend/src/context/WalletContext.js:22`, `pages/transaction/Transaction.js:33,73`.
- **Problema:** com o Open Finance importando extrato, a lista cresce sem limite. O layout baixa tudo em toda navegação autenticada (o `WalletProvider` envolve o layout inteiro) e em todo `refresh()`.
- **Correção:** aceitar `Pageable` no endpoint e usar `?page=&size=` no front. O dashboard só precisa dos 6 últimos (`Dashboard.js:222`).

### H-2 · MÉDIA · `Promise.all` derruba a tela inteira do Futuro quando um endpoint secundário falha
- **Onde:** `pages/future/Future.js:106-116`.
- **Problema:** se `/future/recurrences` falha, `overview` é zerado (`overview: null`) e a tela toda vira um alerta de erro, mesmo com a projeção disponível.
- **Correção:** usar `Promise.allSettled`. Sem `recurrences`, a tela segue sem o bloco de perguntas e mostra um erro localizado no bloco.

### H-3 · BAIXA · Erros silenciosos (catch vazio)
- **Onde:** `pages/dashboard/Dashboard.js:31` (`.catch(() => setOverview(null))`, e o card some sem aviso), `pages/future/Accuracy.js:36` (idem), `components/future-ui/index.js:23` (`countPix ... .catch(() => {})`, aceitável para telemetria).
- **Correção:** no Dashboard, mostrar uma linha "Não foi possível carregar a previsão" com um botão de tentar de novo, em vez de sumir.

---

## 4. Layout, responsividade e estados

### L-1 · MÉDIA · Loading como texto ou spinner solto, sem skeleton, em todas as telas auditadas
- **Onde:** `pages/future/Future.js:178-182`, `pages/commitments/Commitments.js:281-285`, `pages/envelopes/Envelopes.js:122-125` (texto "Carregando..."), `components/wallet-ui/index.js:117-125` (`DataState`: `CircularProgress`, usado por Dashboard, Wallets e Transações), `pages/insights/Insights.js` (só o botão gira), `pages/open-finance/OpenFinance.js` (ver L-3).
- **Problema:** gera layout shift quando o conteúdo chega. Em `DataState`, todo `refresh()` do contexto (depois de criar carteira ou sincronizar o Open Finance) troca o dashboard inteiro por um spinner, porque `loading` volta a `true` (`WalletContext.js:17`).
- **Correção:** criar um `<PageSkeleton variant="stats|list|chart">` com `Skeleton` do MUI na forma dos cards. No `DataState`, mostrar o skeleton só quando ainda não há dados (`loading && !wallets.length`). Nas recargas, manter o conteúdo com `aria-busy`, como o Future já faz na linha 214.

### L-2 · MÉDIA · "Tentar novamente" sem feedback em Compromissos
- **Onde:** `pages/commitments/Commitments.js:259-266,290`.
- **Problema:** `load()` não volta `loading: true` nem limpa `error` ao reexecutar. O clique não muda nada até a resposta chegar e parece quebrado.
- **Correção:** chamar `setState((s) => ({ ...s, loading: true, error: null }))` no início de `load`, como `Envelopes.js:44` já faz.

### L-3 · MÉDIA · Open Finance: estados de carregamento e erro mentem
- **Onde:** `pages/open-finance/OpenFinance.js:248,251,254,221-225`.
- **Problema:** com `status === null` (carregando ou erro), o card afirma "Sandbox de participante" (248) e "Nenhuma instituição conectada" (251), e mostra "Carregando..." (254). No erro, o texto "Carregando..." fica para sempre ao lado do alerta, e o alerta de erro (221) não tem botão de tentar novamente. Se o `notice` vier vazio, também fica "Carregando..." para sempre.
- **Correção:** renderizar um skeleton enquanto `loading && !status`. No erro sem status, mostrar `Alert` com `action={<Button onClick={load}>}` e esconder o card. Remover o fallback `t('openFinance.loading')` de `notice` (resolvido por I-1).

### L-4 · MÉDIA · Falso "vazio" em Metas
- **Onde:** `pages/future/Goals.js:20,28-35,146`.
- **Problema:** `goals` começa como `[]`, sem estado de carregamento. O estado vazio ("nenhuma meta") pisca antes de a lista chegar.
- **Correção:** iniciar com `null`, mostrar um skeleton enquanto `goals === null` e o vazio só quando `goals.length === 0`.

### L-5 · MÉDIA · Painel de conta estoura a largura entre 600 e 1199 px
- **Onde:** `layouts/dashboard/DashboardLayout.js:159-183`.
- **Problema:** a partir de `sm` o painel vira `direction="row"` sem `flexWrap`, com nome, 4 links (Perguntar, Compromissos, Análises, Open Finance), "Sobre" e "Sair" na mesma linha. Em tablet (≈768 px) isso passa da largura do `Container` e causa scroll horizontal ou corte. O painel também não fecha com `Esc` nem com clique fora, e falta `aria-controls`.
- **Correção:** usar `flexWrap: 'wrap'` com `gap: 1`, ou `direction="column"` até `lg`. O ideal seria trocar por `Menu`/`Drawer` do MUI, que já traz `Esc`, trap de foco e fechamento por clique fora.

### L-6 · BAIXA · Mobile: "Perguntar", "Análises", "Compromissos" e "Open Finance" ficam escondidos no avatar
- **Onde:** `DashboardLayout.js:33-47`.
- **Problema:** as funções de análise e predição que o dono citou só aparecem em mobile se o usuário tocar no avatar, que não tem rótulo visível.
- **Correção:** trocar "Transações" por "Perguntar" no bottom nav (transações já estão no dashboard) ou adicionar um item "Mais".

### L-7 · BAIXA · Gráfico de projeção: largura inicial fixa e mínimo de 280 px
- **Onde:** `components/projection-chart/index.js:13,16`.
- **Problema:** o primeiro render usa `width=640` até o `ResizeObserver` disparar, o que em 375 px causa um frame com overflow e salto. O piso de 280 px estoura em telas de 320 px (container útil ≈ 256 px).
- **Correção:** usar `viewBox` com `width="100%"`, ou iniciar com `ref.current?.clientWidth` via `useLayoutEffect` e remover o piso.

### L-8 · BAIXA · Legenda do gráfico com margem anulada
- **Onde:** `components/projection-chart/index.js:74`, `sx={{ mb: 1, p: 0, m: 0 }}`.
- **Problema:** `m: 0` vem depois e zera o `mb: 1`, então a legenda cola no SVG.
- **Correção:** usar `sx={{ p: 0, m: 0, mb: 1 }}`.

### L-9 · BAIXA · Legenda de Análises não bate com as cores das barras
- **Onde:** `pages/insights/Insights.js:268-277`.
- **Problema:** o "●" de Entradas fica cinza (`text.secondary`) enquanto a barra é `success.main`. Só o de Saídas recebe cor via `::first-letter`.
- **Correção:** usar dois `Box` de 8×8 com `bgcolor` igual ao das barras.

### L-10 · BAIXA · Tooltip do gráfico só com mouse
- **Onde:** `projection-chart/index.js:107-108,206`.
- **Problema:** em toque ou teclado não há detalhe por dia. O `aria-label` resume o gráfico, então a acessibilidade mínima está coberta. A lista "ver dias" (`Future.js:363-377`) compensa.

### L-11 · BAIXA · Valores mágicos de estilo espalhados
- **Onde:** `Insights.js:153,304,370` (`borderRadius: '18px'`/`'20px'`), `OpenFinance.js:232-238` (220/-70/-90), `Questions.js:242` (`width: 180`), `Simulator.js:187,203,235` (`minWidth` 140/160/180), `DashboardLayout.js:64,114` (240 duplicado).
- **Correção:** levar raios e larguras de sidebar para `theme.shape`/tokens no `theme/`.

### L-12 · BAIXA · Bundle único de 250 kB gzip
- **Onde:** `App.js:12-23`, com todos os imports estáticos.
- **Correção:** usar `React.lazy` por rota (Insights, Metrics, OpenFinance e Future são pesadas e raras) com `Suspense` usando o skeleton de L-1.

---

## 5. Análise (Insights) e Predição (Futuro), números errados

### A-1 · ALTA · Análises mostra o **mês anterior** no fluxo mensal (fuso)
- **Onde:** `pages/insights/Insights.js:94-98`.
  ```js
  new Intl.DateTimeFormat(i18n.language, { month: 'short', year: '2-digit' })
    .format(new Date(Date.UTC(year, month - 1, 1)))
  ```
- **Problema:** meia-noite UTC do dia 1 ainda é o dia anterior em UTC−3, e o formatter usa o fuso local. Prova rodada com `TZ=America/Sao_Paulo`: `'2026-09'` sai como **"ago. de 26"**. Todas as linhas do "Fluxo mensal" ficam deslocadas um mês para trás para qualquer usuário no Brasil.
- **Correção:** reusar `monthLabel` de `utils/wallet.js:136`, que monta a data local (`localDate`), ou passar `timeZone: 'UTC'` no formatter. Adicionar um teste em `wallet.test.js`/Insights com `TZ` negativo.

### A-2 · MÉDIA · Simulador exibe o resultado da aba anterior (condição de corrida)
- **Onde:** `pages/future/Simulator.js:135-138` versus `98-113`.
- **Problema:** a troca de aba limpa `result`, mas **não incrementa `latest.current`**. Se o usuário simula uma compra e troca para "Guardar todo mês" antes da resposta, a resposta da compra chega com `mine === latest.current`, é gravada e aparece na aba nova, com `summaryLines` montado com `request.type = 'PURCHASE'`. O `set(field)` da linha 79 já faz o incremento certo, mas a aba não.
- **Correção:** adicionar `latest.current += 1;` no `onChange` das `Tabs`.

### A-3 · MÉDIA · "Perguntar" e Dashboard mostram limite e "livre" quando a tela Futuro esconde de propósito
- **Onde:** `pages/ask/Ask.js:49-55` (DAILY_LIMIT), `pages/dashboard/Dashboard.js:36-80` (FutureSummary) versus `pages/future/Future.js:131-139`.
- **Problema:** a tela Futuro só mostra o limite diário com `confirmedRecurrences > 0` e troca tudo por um estado vazio quando `noData`. O comentário da linha 131 diz: "zeros e linha reta pareceriam 'você tem R$ 0'". Já o Perguntar responde "Você pode gastar até R$ X por dia", e o Dashboard mostra "comprometido R$ 0 / livre = saldo", ambos sem nenhuma renda confirmada. É o mesmo usuário recebendo duas respostas diferentes.
- **Correção:** extrair um helper `forecastReady(overview)` em `utils` e aplicar nos três lugares. Com o helper falso, Ask e Dashboard devem apontar para "responda as perguntas no Futuro".

### A-4 · MÉDIA · "Falta" diferente entre Perguntar e o cartão do Futuro
- **Onde:** `pages/ask/Ask.js:52-53` usa `limit.shortfall`. `components/future-ui/index.js:311-328` usa `realShort = shortfall − reserved` e só cai em `shortfall` quando o problema são os envelopes.
- **Problema:** com envelopes, o Perguntar diz "faltam R$ 800" e o cartão diz "faltam R$ 300 mesmo sem gastar nada", para o mesmo dado. O comentário do próprio `DailyLimitCard` avisa que isso é o que ele evita.
- **Correção:** mover o cálculo de headline e falta do `DailyLimitCard` para uma função pura em `utils` e usá-la no Ask.

### A-5 · BAIXA · Sinal inconsistente do "menor saldo" na mesma tela
- **Onde:** `Future.js:303` (`Math.abs(lowestBalance)`, mostrado como "abaixo de zero em R$ 120"), `future-ui/index.js:422` (`laterRisk`: "até -R$ 120"), `Future.js:333` (aria-label com sinal), `Dashboard.js:76` (abs).
- **Correção:** escolher uma convenção, "abaixo de zero em R$ X" com abs, e aplicar em `laterRisk` e no `aria-label`.

### A-6 · BAIXA · Sugestão de aporte baseada em saída bruta
- **Onde:** `pages/insights/Insights.js:85-86`.
- **Problema:** calcula `outflow / periodMonths × 10%`. Com `outflow = 0` a sugestão vira "0.00", e o campo `type="number"` não aceita a vírgula que o resto do app aceita (`parseAmount`).
- **Correção:** usar `inputMode="decimal"` com `parseAmount` (padrão de `Envelopes`/`Goals`), e só sugerir quando `outflow > 0`.

### A-7 · BAIXA · Aritmética de dinheiro em float
- **Onde:** `pages/envelopes/Envelopes.js:112` (`Number(balance) - Number(reserved)`), `components/future-ui/index.js:314`, `Commitments.js:124-ish` (`installmentAmount - nextInstallmentInterest`).
- **Problema:** um resultado como `-1e-15` passa em `free < 0` e pinta "-R$ 0,00" de vermelho.
- **Correção:** fazer a conta em centavos (`Math.round(x*100)`), como `totalsFor` e `Dashboard.js:96` já fazem.

### A-8 · BAIXA · "Entradas/Saídas" do Dashboard somam o histórico inteiro sem dizer o período
- **Onde:** `pages/dashboard/Dashboard.js:95,163-171` (`totalsFor(transactions)` sobre todas as transações).
- **Correção:** limitar ao mês corrente e mudar o rótulo para "Entradas no mês", ou deixar o período explícito.

### A-9 · BAIXA · Locale cru do i18next em formatters
- **Onde:** `Insights.js:96,199` (`Intl.DateTimeFormat(i18n.language, ...)`).
- **Problema:** o resto do app normaliza com `localeFor()` (`utils/wallet.js:1`). Aqui `i18n.language` pode ser `'pt'`, que funciona por acaso, ou outro valor salvo. É inconsistente.
- **Correção:** usar `dateTime()`/`monthLabel()` de `utils/wallet.js`.

---

## 6. Outros (qualidade)

- **BAIXA** · `pages/future/Goals.js:40-44`: `useEffect` só para limpar `fit` quando o form muda. É estado derivado; o certo é limpar no próprio `onChange`.
- **BAIXA** · `pages/ask/Ask.js:28`: `max: 24` hardcoded, duplicando `@Max(24)` do backend. Se um dia mudar lá, a mensagem mente.
- **BAIXA** · `pages/ask/Ask.js:118`: `id: Date.now()` como key. Duas perguntas no mesmo ms (clique duplo em sugestão) colidem. O guard `busy` reduz o risco. Um contador `useRef` resolve.

---

## O que não foi feito

- **Verificação ao vivo no navegador (DOM, 375 px e 1280 px, teclado) não foi feita.** Subir backend, banco e front e logar ficou fora desta auditoria somente leitura. Os achados de layout (L-5, L-7) vêm da leitura do código e precisam ser confirmados no navegador.
- Não auditei a fundo `pages/admin/Metrics.js`, `pages/auth/*`, `About.js` nem o tema (`theme/*`), porque estavam fora das telas pedidas.
- Não validei a semântica dos cálculos do backend (motor de projeção), só como o front exibe os números.
