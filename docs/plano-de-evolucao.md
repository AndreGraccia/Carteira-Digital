# Plano de evolução — Wall.ia

Auditoria de 24/09/2026 sobre a branch `feat/wallia` (`2228112`). Fontes, com evidência arquivo:linha, em [`auditoria-2026-09-24/`](auditoria-2026-09-24/):

| Relatório | O que cobre |
| --- | --- |
| [`contextos.md`](auditoria-2026-09-24/contextos.md) | As 4 sessões anteriores: o que foi feito, 42 pendências, promessas não cumpridas |
| [`chatgpt.md`](auditoria-2026-09-24/chatgpt.md) | Conversa "Comparar carteiras digitais": bugs apontados e plano de fases 0–10 |
| [`frontend.md`](auditoria-2026-09-24/frontend.md) | Layout, i18n, rotas, integração, exibição de análise e predição |
| [`backend.md`](auditoria-2026-09-24/backend.md) | Motor Futuro, insights, Open Finance, integrações, rotas, banco |
| [`openfinance.md`](auditoria-2026-09-24/openfinance.md) | Padrão Open Finance Brasil, BCB, Pix, regras de crédito, LGPD, contabilidade |

## Andamento (revisado em 25/09/2026)

Revisão item a item contra o código em [`analise-2026-09-25.md`](analise-2026-09-25.md), que também traz os próximos passos (Sprint 9) e o que depende de rede.

| Sprint | Estado |
| --- | --- |
| 0 Higiene | Feita. O push forçado sem coautoria foi feito em 25/09. **O CI no GitHub nunca foi conferido.** |
| 1 Predição e análise | Feita. |
| 2 Segurança | Feita. |
| 3 E-mail no cadastro | Feita (V24, ADR 0008). |
| 4 Tradução e layout | Parcial. Verificação pelo DOM (Playwright headless, sem navegador real) em 375 e 1280 px, PT e EN, em 9 telas. Nomenclatura padronizada em 25/09. **A tela Futuro no celular continua com cerca de 4.700 px.** |
| 5 Aderência OFB | Parcial (V25, V26, ADR 0009). **Faltam `GET /consents/{id}` e o sync por `/transactions-current`.** |
| 6 API e banco | Parcial. Existe teste de integração ponta a ponta. **Faltam `@WebMvcTest` por controller e o E2E no CI.** |
| 7 BCB | Custo do negativo, rotativo, CET, comparação com o mercado e CDI/IPCA/Selic feitos (V29, ADR 0010). **Faltam o Pix Automático simulado, Metas com CDI/IPCA e a explicação de caixa e competência na tela.** |
| 8 Entrega | README, CHANGELOG e `.env.example` atualizados. **Faltam LGPD, guia do desenvolvedor, porta padrão, imagens Docker e o PR para a `main`.** |

Detalhe de cada entrega em [`CHANGELOG.md`](../CHANGELOG.md). Numeração real dos ADRs: 0007 dia útil, 0008 e-mail no cadastro e consentimento, 0009 aderência OFB, 0010 dados do BCB. Onde o texto abaixo cita outro número, vale esta lista.

## Estado medido em 24/09

- Backend: `./mvnw test` → 220 testes, 0 falhas; migrações V1–V22 aplicam em banco vazio (Postgres 14).
- Frontend: build, ESLint e 19 testes passam sem warning; chaves i18n PT/EN batem; 36 chamadas HTTP batem com os controllers; nenhum link quebrado.
- Ou seja: **o que quebra não é compilação, é regra**. Os defeitos abaixo passam em todos os testes atuais.

## Como ler as sprints

- Sprint = 1 semana. Ordem por risco: primeiro o que mostra número errado ou vaza dado, depois aderência ao padrão, depois acabamento e diferencial.
- Cada item tem ID do relatório de origem (F = backend/Futuro, A/L/I/R/H = frontend, O = Open Finance).
- **Pronto** = teste que falhava antes e passa depois + verificação na tela (navegador real, 375 px e 1280 px) quando houver UI. Sem isso o item não fecha.

---

## Sprint 0 — Higiene (1–2 dias)

| # | Tarefa | Estado |
| --- | --- | --- |
| 0.1 | Desligar coautoria automática do Claude nos commits (`attribution` em `~/.claude/settings.json` + hook `commit-msg` local) | Feito |
| 0.2 | Branch `feat/wallia-sem-coautor`: mesmos 18 commits, mesmo código (árvore idêntica), sem o trailer | Feito; push forçado em 25/09 com `--force-with-lease` |
| 0.3 | Decidir a alteração não commitada em `i18n.js` (chave `openFinance.MuiTypography`, EN = `'.'`, ninguém lê) → descartar; a correção real é o item I-1 da Sprint 4 | Feito (descartada; I-1 em `15fc192`) |
| 0.4 | Conferir o CI no GitHub (nunca foi visto verde por nós) | Pendente |
| 0.5 | Conferir se ninguém mais tem `feat/wallia` baixada antes do push forçado (repositório é do André) | Feito (remoto conferido em `2228112` antes do push) |

## Sprint 1 — Predição e análise mostram número errado

O produto promete "Java calcula" com exatidão. Estes são os casos em que ele calcula errado.

| ID | Defeito | Onde | Teste de aceite |
| --- | --- | --- | --- |
| F1 | Recorrência que atrasa para o mês seguinte (salário do dia 30 caindo dia 1) some da projeção e gera janela negativa e alerta falsos | `CashEventPlanner.java:105` | salário 30/08, 01/10 → projeção contém 30/10 |
| — | Empréstimo com vencimento 28/02 vira "todo dia 28" | `LoanPlan.java:30` | vencimento dia 31 → 28/02, 31/03 |
| — | "Juros que faltam" errado no SAC (1000,00 onde é 550,00) | `LoanCost.java:11` | caso SAC de referência |
| — | Com renda amanhã, limite diário = saldo inteiro | `SpendingLimit.java:62` | renda em D+1 → limite ≤ saldo − compromissos |
| — | Saída do buraco ignora envelopes e marca "resolve" sem reprojetar | `ExitPlan.java:54-58` | com envelope, sugestão reprojetada sem dia negativo |
| — | Limite da visão geral ignora gasto no cartão confirmado | backend.md | igual ao da tela Futuro |
| O5 | Empréstimo quitado continua gerando parcelas | `OpenFinanceImportService.java:170-190` | empréstimo ausente no import → sem parcelas futuras |
| — | Insights: transferência entre contas próprias conta como receita e despesa | `InsightsService.java:119-124` | transferência própria → 0 no consolidado |
| — | Insights fecha mês em UTC, Futuro usa Brasília | `InsightsService.java:91,114` | lançamento 30/09 23h BRT → setembro |
| A-1 | Tela Análises mostra o mês anterior ("2026-09" → "ago.") | `Insights.js:94-98` → reusar `monthLabel` (`utils/wallet.js:136`) | teste com `TZ=America/Sao_Paulo` |
| A-3/A-4 | Ask e Dashboard mostram limite sem renda confirmada; "falta" com dois valores diferentes | `Ask.js:49-55`, `Dashboard.js:36-80`, `future-ui/index.js:314` | uma função única de exibição |
| — | Alerta repete todo dia (compara início exato da janela) | `FutureAlertService.java:151` | mesma janela deslocada 1 dia → 1 alerta |
| — | Fim de semana/feriado sem ajuste de vencimento | motor | decisão em ADR (adiantar/postergar por tipo) + teste |

Saída: suíte de datas de borda (dia 29/30/31, fevereiro, virada de ano, fuso) no motor.

## Sprint 2 — Segurança e integridade dos dados

| ID | Defeito | Onde |
| --- | --- | --- |
| **O1 (crítico)** | Modo `participant-sandbox` usa token/consent global: todo usuário importa a mesma conta | `ParticipantSandboxOpenFinanceGateway.java:70` — token por usuário ou bloquear o modo para 1 usuário dev |
| O3 | Consentimento nunca expira (cada sync renova `expiresAt`) | `ContractSandbox…:96`, `OpenFinanceImportService.java:73-74` |
| O4 | Revogar não tira dados importados do saldo/projeção e trava exclusão da carteira | `OpenFinanceService.java:131`, `WalletService.java:247` |
| I1 | Chamada ao Gemini (até 20 s) dentro de transação: 10 relatórios lentos travam o pool | `InsightsService.java:64` |
| — | Enumeração no cadastro (erro distinto para usuário e e-mail existentes) — prometido e nunca feito | `AuthService.signup` |
| R-1 | Sem interceptor de 401: sessão expirada em /future, /insights, /ask fica em loop de "Tentar novamente" | `services/axios.js` |
| — | Jobs agendados sem lock (2 réplicas = 2 execuções) | sync diário, alertas — ShedLock ou `pg_advisory_lock` |
| — | JWT sem `iss`/`aud`, sem revogação no logout/troca de senha | backend.md |

## Sprint 3 — Verificação de e-mail sai do Open Finance e vai para o cadastro

Plano da conversa com o ChatGPT (fases 0–4), conferido contra o código local:

1. `User.emailVerifiedAt` + migração **V23** (renomear `open_finance_email_challenge` → `email_verification_challenge`).
2. `POST /api/v1/auth/email/verify` e `/email/resend`; login recusa conta não verificada. Contas ativas antes da V23 entram como verificadas.
3. Open Finance sem OTP: saem `OpenFinanceEmailVerificationService`, `OpenFinanceCodeRequest`, `/connection/challenge`, `/connection/confirm`, `PENDING_EMAIL_VERIFICATION` e o modal de `OpenFinance.js`.
4. Alertas por e-mail só com `emailVerifiedAt != null` (hoje dependem do Open Finance).
5. Corrigir o NPE pendente em `OpenFinanceEmailVerificationServiceTest:74` (ou apagar com a classe).

Risco: quebra login de contas de teste. Reverter = migração de volta + flag. ADR 0007.

## Sprint 4 — Tradução e layout

| ID | Defeito | Correção |
| --- | --- | --- |
| I-1 | Aviso do sandbox aparece em PT com a tela em EN (`status.notice` cru) | chaves `openFinance.notice.contract` / `.participant` por `status.simulated` |
| I-2 | Backend manda rótulos prontos em PT ("Compra simulada", "Meta"…) | backend manda código; front traduz |
| — | "Pergunte ao/à Wall.ia" inconsistente; README ainda diz "Plano de saída do buraco" | padronizar |
| — | ~60 chaves órfãs; `INVALID_PERIOD`, `INITIAL_BALANCE_NOT_ALLOWED` sem `errorKey` | limpar e mapear |
| H-2 | `Promise.all` em `Future.js:106-116`: falha em `/recurrences` derruba a tela | `Promise.allSettled` |
| A-2 | Simulador mostra resposta de outra aba | `Simulator.js:135-138` |
| L-1..L-4 | Carregamento solto, retry sem feedback, estado vazio piscando, Open Finance afirmando estado durante loading | skeleton + estados explícitos |
| L-5 | Painel de conta estoura largura no tablet e não fecha com Esc | `DashboardLayout.js:159-183` |
| — | Grid dentro de Stack voltou em `admin/Metrics.js`; Futuro no celular tem 4.530 px | revisão de hierarquia mobile |
| — | Gráfico: largura fixa 640, legenda com cor errada, tooltip só no mouse | acessibilidade |
| — | **Verificação em navegador real** — nunca feita em nenhuma sessão | checklist por tela, 375/1280 px, teclado |

## Sprint 5 — Aderência ao padrão Open Finance Brasil

Referência: YAMLs oficiais `OpenBanking-Brasil/openapi` (consents 3.3.1, resources 3.1.0, accounts 2.4.2, credit-cards-accounts 2.3.1, loans 2.5.0).

| # | Hoje | Padrão |
| --- | --- | --- |
| 1 | Status `AUTHORISED`/`REVOKED` | `AWAITING_AUTHORISATION`/`AUTHORISED`/`REJECTED` + `rejection.rejectedBy` + `reason.code` (`CUSTOMER_MANUALLY_REVOKED`, `CONSENT_EXPIRED`) |
| 2 | Pede só `ACCOUNTS_*` | `RESOURCES_READ` + `CREDIT_CARDS_ACCOUNTS_*` + `LOANS_*` (o app lê cartão e empréstimo) |
| 3 | Pagina `/transactions` por `page`/`meta.totalPages` — **lê só a 1ª página** numa transmissora real | `pagination-key` + `links.next` |
| 4 | `DEBITO_AUTOMATICO`, `COMPRA_DEBITO` | enum oficial (`CONVENIO_ARRECADACAO`, `CARTAO`…) |
| 5 | Sem `completedAuthorisedPaymentType` | `LANCAMENTO_FUTURO` é o dado oficial para projetar — usar no motor |
| 6 | Cartão com nomes inventados | `bills[]` com `billTotalAmount`, `billMinimumAmount`, `billForecastDate`; parcelas em `chargeIdentificator`/`chargeNumber` |
| 7 | Empréstimo com nomes inventados, sem CET | `contractOutstandingBalance`, `nextInstalmentAmount`, `interestRates[]`, `CET`; `preFixedRate` é fração |
| 8 | Não consulta `GET /consents/{id}` nem `/resources` | consultar; `expiresAt` vem daí |
| 9 | Sync diário pede 12 meses | `/transactions-current` (7 dias) |
| 10 | Nada valida o sandbox contra o padrão | teste de contrato contra os YAMLs oficiais (ou `OpenBanking-Brasil/mock-api` em Docker) |

Performance junto: dedupe do import com 1 SELECT por lançamento (`:98`) → lote; chamadas HTTP fora do lock da linha do usuário.

Opcional, via ADR: adaptador **Pluggy** (sandbox grátis e sem prazo) atrás da mesma interface de gateway, para demonstrar dado de instituição real sem ser participante. Posicionamento regulatório fica o mesmo: PFM em sandbox, nunca participante do Open Finance nem banco.

## Sprint 6 — API, banco e testes

- `GET /transactions/users/{id}` sem paginação real (`PageImpl` da lista toda) → paginação no banco + filtro de data; o Futuro deixa de ler o histórico inteiro.
- Índices nas FKs de `transaction` (EXPLAIN mostrou seq scan).
- Status HTTP corretos: NOT_FOUND → 404, conflito → 409, regra → 422; hoje tudo vira 400 (`GlobalExceptionHandler.java:68`) e saldo insuficiente vira 412.
- `WalliaApplicationTests` sem `@Test` (contexto nunca sobe) e **zero teste de controller**: rota, autorização e HTTP não são testados → `@WebMvcTest` por controller + teste de acesso de outro usuário.
- Gemini: retry com backoff, log do status, timeout menor; grounding desligado.
- Bundle de 250 kB gzip único → `React.lazy` por rota.
- E2E (Playwright) dos 3 fluxos que já falharam na mão do dono: cadastro → login → conectar Open Finance → Futuro.

## Sprint 7 — Dados do BCB e diferenciais

Consumo das APIs públicas (sem chave):

| Uso | Fonte |
| --- | --- |
| Custo real do negativo em vez do teto de 8% a.m. | Olinda `taxaJuros/v2` — taxa média por instituição e modalidade (dados até ago/2026) |
| Rotativo: aviso de que encargos têm teto de 100% da dívida | Lei 14.690/2023, Res. CMN 5.112/2023 |
| IOF no custo do negativo/empréstimo | PF: 0,0082% a.d. + 0,38% (Decretos 12.466 e 12.499/2025) |
| Metas: rendimento e inflação | SGS 432 (Selic meta), 4389 (CDI), 433/13522 (IPCA), Focus |
| Pix Automático simulado com os estados oficiais | Res. BCB 402/2024 — aviso 2–10 dias antes, retentativa só se autorizada |

Cache diário das séries no banco (fonte e data de coleta visíveis na tela — "de onde veio este número").

Ainda na fila da estratégia: SCR/Registrato, tarifas, quitação do cartão, divisão da renda, primeira leitura real do placar de precisão (01/10).

**Contabilidade:** COSIF não se aplica a pessoa física. O que se aplica: Futuro trabalha em **regime de caixa** (data em que o dinheiro sai); Análises pode trabalhar em **competência** (data da compra, parcela distribuída nos meses). Deixar isso explícito na tela e no ADR evita os dois números "diferentes" que hoje confundem. Categorização por `type`, `transactionType`, `payeeMCC` e contraparte.

## Sprint 8 — LGPD, documentação e entrega

- Checklist LGPD ([`openfinance.md` §6](auditoria-2026-09-24/openfinance.md)): finalidade escrita na tela e no dado, exclusão de conta e dados (art. 18), criptografia em repouso dos dados importados, retenção mínima, tela de "privacidade visível" (prometida, não existe).
- Imagens Docker regeneradas; `.env.example` completo; porta padrão sem colisão.
- README e guia do desenvolvedor atualizados com o que mudou; ADRs 0007 (e-mail no cadastro), 0008 (aderência OFB), 0009 (dados BCB).
- PR `feat/wallia` → `main`, com CI verde e changelog.

---

## Fora do plano (decidido, não reabrir)

Pix real (exige credenciamento), IA movendo dinheiro, admin movimentando saldo, virar participante do Open Finance. Visão do produto: pergunta antes de supor, estimado separado do confirmado, sandbox explícito.

## Pontos a confirmar na fonte antes de implementar

- Números de normas citados só pelo ChatGPT (IN BCB 759/720, Res. 526/541, "Manual de Experiência 9.0", IN 746/2026).
- Séries SGS 20714 e 20716 (modalidade), caminho da PTAX no Olinda, códigos de status da payments v4.
- Se há versão das APIs OFB posterior a 19/12/2025 (último commit consultado).
