# Relatório das sessões anteriores do Wall.ia (Contexto1 a Contexto4)

Fonte: `Carteira-Digital/Contexto1.txt` a `Contexto4.txt`. Li a parte conversacional completa das quatro transcrições (mensagens do usuário, do assistente, resumos finais e laudos dos revisores). Os diffs de código, só por amostragem. Citações no formato `C<n>:<linha>`. Uma checagem pontual no repositório está marcada como **[repo]**.

---

## 1. Linha do tempo

### Sessão 1 (Contexto1, 22/09): rebrand, Motor Futuro, tema novo
- Pedido inicial (C1:7): "analise o projeto inteiro e melhore ele por completo… ajuste o nome que mudamos para Wall.ia e faça um branding inteiramente novo… tudo deve ser baseado em números, projeções e visões". Repositório: github.com/AndreGraccia/Carteira-Digital.
- O usuário colou o documento de visão do produto (C1:128-209): gêmeo financeiro, "pergunta em vez de supor", Modo Futuro, "Java calcula, a IA explica e o usuário decide", agente só de leitura, Open Finance em sandbox, ledger de dupla entrada, papéis administrativos com auditoria. Depois pediu "Faça pesquisas no mercado também" (C1:216).
- Feito:
  - correção do teste do `SecureRandom` no Java 21 (69/69);
  - pesquisa de mercado por subagente, em `pesquisa-mercado.md`;
  - `.npmrc legacy-peer-deps`;
  - branch `feat/wallia`;
  - remoção dos scripts soltos (`.wallet-*.py`, `.translation-runtime.cjs`).
- Motor Futuro em `service/future/`: `CashEvent`, `ProjectionEngine`, `RecurrenceDetector`, `CardPlan`, `LoanPlan`, `CashEventPlanner`, `ScenarioSimulator`. Junto vieram a migração V11 (`V11__future_engine.sql`), o `FutureService`, o `FutureController` (`/api/v1/future`), os DTOs e a massa sandbox (`ContractSandboxOpenFinanceGateway`: Concentrix, Netflix, imobiliária, cartão parcelado, empréstimo), calibrada para mostrar cerca de -R$ 325 entre 3 e 5/10.
- Correções:
  - fail-fast do segredo JWT (`JwtUtils`);
  - 409 no resync do cartão (merge por `externalId`);
  - datas LocalDate em ISO (`AppConfig`, com `AppConfigTest`);
  - rename do artefato para `wallia`;
  - `docker-compose.prod.yml` deixou de passar o `.env.properties` para o frontend;
  - Maven Wrapper migrado para only-script.
- Frontend:
  - tema novo (`theme/palette.js`, contraste WCAG checado) e logo "W como gráfico de saldo" (`components/brand`, favicons, `scripts/build-icons.py`);
  - `projection-chart` e `future-ui`;
  - páginas `future/Questions.js`, `Simulator.js`, `Future.js` e `commitments/Commitments.js`;
  - navegação (barra inferior com 5 itens) e hero do login (`AuthView.js`);
  - card "Próximos 30 dias" no Dashboard, `index.html`/`manifest.json`, fontes self-hosted;
  - i18n com 466 chaves.
- Estado final: 96 testes no backend, build do front de 228 kB. Na primeira validação visual (puppeteer headless) apareceram **3 defeitos de layout**. A sessão foi interrompida e compactada antes de corrigi-los (C1:4037-4041).

### Sessão 2 (Contexto2, 23/09 de manhã): acabamento, docs e primeira tentativa de rodar
- Corrigido:
  - Grid deslocado em `Future.js`;
  - origem duplicada ("Banco Aurora Sandbox · Banco Aurora Sandbox • 8392") em `CashEventPlanner.origin` e `Questions.js`;
  - `useFlexGap` trocado por `gap`;
  - extrato mostrando "Retirada" em vez do nome;
  - "Outubro De 2026";
  - "Ver tudo" quebrando no mobile (`wallet-ui/index.js`);
  - título "e-wallet" na página Sobre.
- Docs: README reescrito; `docs/pesquisa-mercado.md`; **ADR 0001** (`docs/adr/0001-motor-futuro-deterministico.md`, "Java calcula, IA explica"); `docs/open-finance-email-tests.md` refeito (estava com mojibake "Conex?o").
- Erro do assistente: um `git checkout` apagou alterações não commitadas de `frontend/src/utils/wallet.js`. Foi recuperado pelos source maps do cache do jest (C2:257-269) e registrado no Jarbas como ERR-GIT-001.
- Usuário: "Segui o readme e não consegui rodar o projeto" (C2:896). Pediu um passo a passo detalhado e um plano de estudos do zero a CTO. Depois: "Tentei criar uma conta e não consegui" (C2:1005).
- Causas encontradas:
  - as portas 5432, 8080 e 3000 estavam ocupadas por `forja-db`, `forja-web` e `e-wallet-frontend`; a tela chamava a 8080, que é o forja-web;
  - o volume antigo do banco tinha outra senha;
  - o `jwt_secret` e a senha do banco ainda eram os textos de exemplo.
- As portas viraram configuráveis e o backend passou a subir na 8082. A sessão foi compactada no meio da validação do CORS (C2:1044).

### Sessão 3 (Contexto3, 23/09): guia, trilha, segurança, novas funcionalidades, Spring Boot
- Contexto: "estou em uma rede empresarial, então possivelmente não dê para rodar tudo" (C3:35).
- Docs e trilha: `docs/guia-do-desenvolvedor.md`, README atualizado, `Downloads\Wall_ia\PLANO_DE_ESTUDOS_FULLSTACK.md`. Depois o pedido "um md é pouco" (C3:1316) levou à pasta `trilha-fullstack/`, com cerca de 8.700 linhas, 11 fases, 19 laboratórios, projeto "Cofre" e modelos. Parte foi escrita por 3 subagentes.
- Revisão de segurança (C3:353-420), com 4 achados:
  - MÉDIO, rate limit do login: corrigido em `AuthenticationRateLimiter`;
  - MÉDIO, IBAN sintético previsível bloqueando o Open Finance de outro usuário: corrigido com semente aleatória;
  - BAIXO-MÉDIO, cadastro sem limite / spam de código / custo do Gemini: corrigido com a V12 (teto diário do código), limite de cadastro por IP e teto global do Gemini com fallback local;
  - BAIXO, enumeração de e-mail/usuário no cadastro: **não corrigido** (ver seção 2).
- Limpeza aprovada: 43 arquivos mortos, 98 assets, docs/Postman do template, 11 dependências npm, clone aninhado e `yarn.lock`. `Contexto*.txt` entrou no `.gitignore`.
- Usuário: "tentei criar um usuario e logar e não consegui" (C3:1440). Causa: o cadastro era case-insensitive e o login case-sensitive. Agora o login aceita e-mail e ignora maiúsculas; o `LoginRequest` limitava a 20 caracteres (ERR-JAVA-014).
- V13: o banco passa a recusar saldo negativo em carteira LOCAL (furo aberto desde a V8).
- Novo pedido: "precisamos que esse projeto se destaque de tudo já existente no mercado… não podemos fugir do nosso case e propósito" (C3:2092). Entregas:
  - `DailySpending` (gasto do dia a dia como pergunta);
  - `SpendingLimit` (limite diário seguro com shortfall);
  - custo do negativo (teto de 8% a.m. do cheque especial);
  - V14 (`decision_amount`);
  - `QuestionParser` + `POST /api/v1/future/ask` ("Pergunte ao Wall.ia", sem LLM);
  - tela `pages/ask/Ask.js`;
  - **ADR 0002** e `docs/estrategia-produto.md`.
- Revisor: 11 achados (C3:4308-4414). Os 10 primeiros foram corrigidos, com testes. O 11º (rate limit do /ask) foi feito na rodada seguinte.
- "De continuidade nos proximos passos e finalize tudo" (C3:5038). Entregas:
  - gasto à vista no cartão;
  - `ExitPlan` (plano de saída);
  - V15, métrica de intenção do /ask + `AskUsage` com rate limit;
  - V16, metas (`Goal`, `Goals.js`);
  - V17, alertas por e-mail (`FutureAlertService`, `AlertToggle.js`, `@EnableScheduling` às 8h);
  - **ADR 0003**.
- Spring Boot 3.0.4 para 3.5.16 (springdoc 2.8.17, flyway-database-postgresql). O upgrade revelou o `BigDecimal.TWO` (Java 19) num projeto Java 17.
- V18 (`AdminAuditInterceptor`, `AdminAuditController`) e **ADR 0004**: auditoria feita, ledger só proposto.
- Estado final: 173 testes, tudo commitado localmente e sem push.

### Sessão 4 (Contexto4, 24/09): revisões, Envelopes, placar, push, CI e publicação
- O usuário decidiu: Movimentar vira Envelopes; Pix só simulado (C4:35-38).
- Duas revisões (frontend e segurança) encontraram 16 defeitos de front e 2 médios + 3 baixos de segurança. Correções:
  - alerta só para e-mail verificado;
  - IP atrás do nginx;
  - username no formato de e-mail;
  - V19 (`identity_hardening`), que desativa os admins do template `johndoe`/`lindacalvin` com senha pública e cria índices únicos case-insensitive;
  - corrida no limite de 10 metas;
  - corrida ao trocar o horizonte;
  - estado "sem dados";
  - sugestão em inglês do Pergunte ("R$ 4,000 in 10x" virava "quanto?").
  - Commit `4603b50`.
- V20, Envelopes (`pages/envelopes/Envelopes.js`, no lugar de Movimentar, com `/transfers` redirecionando). V21, placar de precisão (`ForecastSnapshot`, `ForecastAccuracy`, `ForecastService`, `Accuracy.js`). Pix simulado no plano de saída. **ADR 0005**.
- Primeira validação real em navegador, feita por Chrome headless via CDP (a extensão nunca conectou): 7 telas em 1440 e 390 px, 2 defeitos visuais corrigidos.
- "Faça tudo que ficou pendente" (C4:2801). Entregas:
  - `OpenFinanceDailySync` às 7h30;
  - contagem de cliques no "Preparar Pix";
  - painel de métricas admin (`AdminMetricsController`, `pages/admin/Metrics.js`);
  - Web Push sem SDK (`service/push/WebPush.java`, RFC 8291/8292, V22, `PushService` com allowlist anti-SSRF, `public/sw.js`), além da remoção do `serviceWorker.unregister()` do template;
  - Prettier em todo o front;
  - **ADR 0006**.
- "Faça todo o necessario" (C4:4683). Entregas:
  - 3 revisões (código, segurança, UX/a11y);
  - `FlywayMigrationsTest` (22 migrações num Postgres limpo);
  - `.github/workflows/ci.yml`;
  - correções: placar inflado (previsão do fim do dia comparada com o saldo das 7h30), números do cartão reconciliados (R$ 504,87 / -R$ 792,67 / R$ 804,87), push herdado no logout (`utils/push.js`), contador do Pix inflável, contraste, foco, áreas de toque de 44 px, hierarquia de títulos e tela Futuro no mobile caindo de ~7.000 para 4.530 px.
- Pesquisa Warren + 20 apps (C4:5371). Entregaram-se três ideias:
  - `LoanCost` ("quanto este dinheiro custa");
  - "Ver se cabe" nas metas;
  - `CancelGuide.js` (como cancelar assinatura).
- A pedido do usuário (C4:4723), as mensagens dos commits foram reescritas: 18 commits, backup em `backup/feat-wallia-antes-reescrita`. A branch `feat/wallia` foi publicada. O projeto ficou rodando: front na 3001, back na 8082, usuário `t_483154`.
- Estado final: 220 testes no backend, 19 no frontend.

**ADRs existentes:** 0001 motor determinístico · 0002 limite diário e perguntas · 0003 saída do buraco, metas e alertas · 0004 auditoria admin e ledger · 0005 envelopes, placar e Pix simulado · 0006 sync diário e push sem SDK.

---

## 2. Problemas pendentes ou parcialmente resolvidos

### Layout
1. **Nenhuma tela foi validada pelo usuário nem no Chrome real.** A extensão claude-in-chrome nunca conectou nas 4 sessões (C1:4026, C3:68, C3:5025 "Não vi as telas no navegador", C4:2504). A partir da sessão 4 a checagem foi só por headless/CDP.
2. **Bug do Grid dentro de Stack do MUI (margem negativa zerada) corrigido tela a tela, não na raiz.** Apareceu em `Future.js` (C1:4037, C2) e voltou em `pages/admin/Metrics.js`: "mesmo bug do Stack zerando a margem negativa do Grid que o Future.js já contorna" (C4:4441). É provável que reapareça em telas novas.
3. **Revisão de UX da sessão 4:** "7 defeitos de acessibilidade e 8 melhorias de hierarquia" (C4:4801). Os de a11y foram listados como corrigidos. Das 8 melhorias de hierarquia, o transcript não confirma quais foram aplicadas (C4:5444 cita reorganização da tela Futuro). **Parcial.**
4. **Tela Futuro no mobile continua longa:** 4.530 px depois da redução (C4:6967).

### Tradução / i18n e texto
5. **Nomenclatura inconsistente:** "Pergunte ao Wall.ia" (C3, `FutureService` C3:3327) contra "Pergunte à Wall.ia" (commit `99c6b0d`). Na sessão 4 houve uma varredura por "o Wall.ia" e "buraco" em texto de usuário (C4:6063), mas o README ainda tem "**Plano de saída do buraco**" (C4:6635 diff, linha 31) e o roteiro de teste usa "Como sair do buraco" (C3:6679).
6. **Números marcados [S] na pesquisa de mercado** (Open Finance, Serasa, Pix Automático) vieram de notícias e nunca foram conferidos na fonte (C1:236-237, C2:881).
7. **O Pergunte depende de regex (`QuestionParser`)**, com cobertura EN limitada. Já teve vários erros de classificação corrigidos (C3:4320-4367, C4:996). A taxa "sem resposta" citada é de 1% (C4:4655), com dados de teste.
8. **[repo]** `frontend/src/i18n.js` aparece modificado e não commitado no `git status` atual (fora dos transcripts; pode ser da sessão corrente).

### Integração
9. **Front↔back depende de configuração local.** O front aponta para a 8082 via `frontend/.env.local` (C3:1031), e o padrão 8080 colide com o `forja-web` do usuário. Foi a causa raiz das duas primeiras reclamações.
10. **Open Finance só em sandbox.** O `ParticipantSandboxOpenFinanceGateway` (mTLS) nunca foi revisado: "Código não lido" (C3:418).
11. **Pix real:** só simulado. Depende de credenciamento como iniciador de pagamento, "decisão de negócio" (C4:2792, C4:4657).
12. **Gemini/IA.** A camada "IA explica" com ferramentas controladas, do documento de visão (C1:170-176), **não foi construída**. O Pergunte responde sem LLM (ADR 0002), e o classificador com IA foi adiado até ">20% das perguntas sem resposta" (C4:4655). O `InsightsService` (Gemini) só ganhou teto global e fallback local.
13. **E-mail:** só testado com Mailpit local. Nenhum SMTP real foi validado. O cadastro continua sem confirmação de e-mail; só o código do Open Finance e o alerta exigem e-mail verificado.
14. **Push:** testado só no Chrome/FCM (C4:4337). Firefox, Safari e Edge não foram testados. As chaves VAPID precisam ser geradas por servidor e nunca trocadas (C4:4666).

### Rotas
15. **Nenhuma pendência de rota relatada.** Movimentar foi removida e `/transfers` redireciona para Envelopes (C4:2522). `/admin/metrics` é só para admin. Registro também o tropeço do Git Bash reescrevendo caminhos que começam com "/" (C4:4437), que é armadilha de ferramenta, não do app.

### Análise / insights
16. **Placar de precisão ainda sem dados reais.** A primeira comparação ficou marcada para 01/10 (C4:2783). Depende do sync diário.
17. **Tarifas e IOF:** o sandbox não tem esses dados, então a funcionalidade "Tarifas e IOF no mês" ficou na fila (C4:6359, estratégia item 2).
18. **Próximos da estratégia, não feitos** (C4, `docs/estrategia-produto.md` "Sexta rodada"): painel de dívidas com o SCR do Registrato, tarifas/IOF, planejador de quitação do cartão (avalanche/bola de neve) e divisor da renda na chegada.

### Predição / motor Futuro
19. **Estimativa de gasto não é separada por cartão.** Adiada até "quando houver usuários com mais de um cartão ativo" (C4:4661).
20. **O cheque especial usa o teto legal de 8% a.m. como limite superior**, não a taxa real do usuário (C3:2895). É uma estimativa.
21. **Rate limits em memória** (login, cadastro, /ask, Gemini): "Limites de uso em Redis quando a API rodar em mais de uma instância" (C4:4659).
22. **Ledger de dupla entrada** só proposto (ADR 0004, C3:7124). Gatilho: entrar dinheiro de verdade.
23. **A massa sandbox mudou várias vezes os números da demo** (-324,87, -666,39, -637,93, faltas de 504,87/792,67/804,87). Os docs foram atualizados a cada rodada, mas o ADR 0002 já trouxe uma afirmação falsa ("travado em teste", C3:4858). Vale reconferir a coerência numérica dos docs.

### Segurança
24. **Enumeração no cadastro (BAIXO), nunca corrigida.** Foi apontada em C3:393-397 e listada como "Ficou de fora" em C3:1070. As correções seguintes não a trataram. **[repo]** `AuthService.signup` ainda lança `ERROR_USERNAME_EXISTS` / `ERROR_EMAIL_EXISTS` distintos.
25. **IP real atrás de proxy TLS:** só documentado. A produção precisa configurar (C4:4795, C4:7039).
26. **Antes de aplicar a V19 em produção:** procurar usuários duplicados por maiúsculas (C4:7039, guia).
27. **Papéis administrativos da visão** (suporte, operações, auditoria, admin sem poder de movimentar dinheiro; C1:198-200): só existem USER/ADMIN, mais a trilha de auditoria. **Parcial.**
28. **Privacidade visível** ("o assistente pode ver totais…; não pode ver senhas…", C1:182-184): nenhum transcript mostra essa tela ou texto implementado.

### Testes / CI
29. **O resultado do CI nunca foi verificado:** "o gh não está instalado… confira na aba Actions" (C4:6972, C4:7035).
30. **Não há E2E automatizado de interface.** Os roteiros E2E de API e as capturas foram scripts no scratchpad, fora do repositório. O front tem só 19 testes. Na sessão 4, 16 defeitos de tela escaparam dos testes (C4:2743).
31. **O `WalliaApplicationTests` não tinha teste nem subia o contexto** (C4:4698). O `FlywayMigrationsTest` passou a cobrir as migrações. Não há teste de contexto Spring completo.
32. **O empacotamento offline (`mvnw package`) falhou por plugin fora do cache** (C4:2461). O backend foi rodado com `spring-boot:run`.

### Infraestrutura
33. **As imagens Docker nunca foram regeneradas nem testadas depois das mudanças** (C3:7133 "Também não gerei as imagens Docker"). Na sessão 4, os containers `wallia-backend`/`wallia-frontend` de uma "imagem antiga" foram parados (C4:6468, C4:7037).
34. **Containers alheios** (`e-wallet-*`, `forja-*`, `erp_secure_mongo_prod`) religam com o Docker e disputam memória e portas (C2:879, C3:1073, C4:2456).
35. **Pouca memória na máquina:** backend e front foram mortos por falta de memória várias vezes (C3:1080, C3:7138, C4:6419).
36. **Sobrou a pasta vazia do clone aninhado** `Carteira-Digital/Carteira-Digital/`, para apagar à mão (C3:1063).
37. **O pacote Java continua `com.github.yildizmy`** (C1:518, C2:882).
38. **Java 17 em produção e 21 local.** Isso já gerou um bug (`BigDecimal.TWO`, C3:6788). O risco se repete com APIs novas.
39. **Não há PR aberto.** A merge em `main` depende do usuário e do André (C4:7036).

### Produto
40. **O Pix real, o ledger e os papéis aguardam decisão de negócio** (itens 11, 22, 27).
41. **O agente de IA com ferramentas** (consultar fluxo, comparar períodos, consentimentos), do documento de visão, não existe (item 12).
42. **Os Envelopes são só uma reserva lógica.** "A projeção continua no saldo real, porque o banco não conhece envelopes" (C4:2760).

---

## 3. Decisões do usuário (não relitigar)

- **Nome e posicionamento:** Wall.ia, "carteira digital inovadora e futurista", decisões "baseadas em números, projeções e visões" (C1:8-10).
- **Documento de visão (C1:128-209):**
  - não é "mais um banco digital";
  - pergunta antes de assumir (salário, aluguel, assinatura);
  - mostra sempre origem e destino, e se o dado foi confirmado, detectado ou projetado;
  - estimativa nunca misturada com valor confirmado;
  - nada de "score financeiro misterioso";
  - **Java calcula, a IA explica, o usuário decide**; a IA nunca inventa valor nem move dinheiro;
  - o agente começa só de leitura; ações futuras só como proposta com confirmação;
  - privacidade visível;
  - minimizar o que vai para Gemini/OpenAI;
  - Open Finance **explicitamente sandbox**;
  - a verificação por e-mail é proteção da própria carteira, não autenticação de instituição;
  - admin não movimenta dinheiro de cliente;
  - ledger com estorno em vez de exclusão;
  - três valores: clareza, controle e confiança.
- **"Não podemos fugir do nosso case e propósito"** (C3:2097).
- **Limpeza aprovada** (C3:679-682): código morto do template, assets sem uso, docs/Postman do template e clone aninhado.
- **Commit local aprovado** (C3:682), e depois **publicar a branch com commits "o mais simplificado e assertivo possível"** (C4:4723). Já executado.
- **Movimentar vira Envelopes; Pix só simulado** (C4:35-38).
- **Rodar o projeto inteiro para ele testar** (C4:4760).
- **Olhar a Warren e o mercado inteiro** atrás de funcionalidades que façam sentido (C4:5371).
- Decisões técnicas do assistente, aceitas sem objeção, que convém manter:
  - manter o pacote `com.github.yildizmy` para não conflitar com o André;
  - 422 é o padrão de erro de validação;
  - o Pergunte não usa LLM;
  - ledger só quando houver dinheiro real;
  - não oferecer crédito, investimento nem poupança automática ("Não fazer" da estratégia);
  - nunca sugerir adiar fatura ou empréstimo no plano de saída.
- **Nada foi recusado explicitamente** pelo usuário nos transcripts.

---

## 4. Promessas ou planos do assistente não entregues (ou só em parte)

| Promessa | Onde | Situação |
| --- | --- | --- |
| Testar cadastro/telas no navegador real | C3:68, C3:1031, C3:5025, C3:7133 | Nunca no Chrome do usuário; só headless na sessão 4 |
| Corrigir os 4 achados de segurança | C3:422 e seguintes | 3 corrigidos; a **enumeração no cadastro ficou de fora** e não voltou à pauta |
| Aplicar as "8 melhorias de hierarquia" da revisão de UX | C4:4801 | Não há confirmação item a item |
| Conferir o CI | C4:6968 | Não conferido (sem `gh`) |
| Gerar/validar imagens Docker | C3:7133 | Não feito |
| Conferir os números [S] da pesquisa | C1:236, C2:881 | Só repassado ao usuário |
| Camada de IA que explica com ferramentas | visão C1:170 | Adiada (ADR 0002, gatilho de 20%) |
| Papéis suporte/operações/auditoria | estratégia C3:6627 "Depois" | Só auditoria admin (ADR 0004) |
| Próximos da estratégia (SCR, tarifas/IOF, quitação, divisor de renda) | C4 estratégia | Na fila |
| Pix real | C3:7130, C4:2792 | Decisão de negócio pendente |
| "Explicar no final por que Pix real, LLM, Redis e estimativa por cartão esperam gatilho" | C4:2811 | Entregue (tabela C4:4652) |

Promessas **entregues**:
- correções visuais da sessão 1 (na sessão 2);
- guia passo a passo e plano/trilha de estudos;
- portas configuráveis;
- rate limit do /ask;
- upgrade do Spring Boot;
- documentação do IP atrás de proxy;
- push da branch;
- projeto no ar para teste.

---

## 5. Pedidos recorrentes e o que irrita o usuário

**Pedidos que se repetem:**
- "Analise a última sessão inteira / todas as sessões / o projeto inteiro" (C2:7, C3:7, C4:6), com "seja minucioso" e "veja se deixamos algo para trás" (C4:6).
- "Use todas as skills e tudo que você tem" (C1:8, C3:10, C3:2095, C4:10, C4:4683).
- "Faça tudo / finalize tudo / faça tudo que ficou pendente / faça todo o necessário" (C2:8, C3:1099, C3:5038, C4:2801, C4:4683).
- Diferencial de mercado e pesquisa de concorrentes (C1:216, C3:2092, C4:7, C4:5371).
- Material de aprendizado profundo: passo a passo para rodar, modificar e construir (C2:896), plano de estudos do zero a CTO com cursos gratuitos, vídeos e exercícios (C2:899), e "um md é pouco… steps, estratégias, métodos, planos" (C3:1316).
- Quer **ele mesmo** rodar e testar: "Rode o projeto inteiro também, quero testar" (C4:4760).

**O que irrita (reclamações concretas):**
- **Seguir as instruções e não funcionar.** "Segui o readme e não consegui rodar o projeto" (C2:896), "Tentei criar uma conta e não consegui" (C2:1005), "tentei criar um usuario e logar e não consegui" (C3:1440). Três vezes, a causa foi ambiente ou config (portas, segredo de exemplo, volume antigo) ou um bug simples (login case-sensitive). As entregas vinham "verificadas pela API", mas não pela experiência dele.
- **Documentação rasa.** "Um md é pouco" (C3:1316).
- **Commits verbosos.** Pediu reescrever para "o mais simplificado e assertivo possível" (C4:4723).
- **Contexto do CLAUDE.md global** que se aplica aqui: pt-BR; nada de preâmbulo nem bullets decorativos; nunca dizer "pronto" sem evidência; nada de deploy/push sem aprovação explícita; sem travessão nem setas em texto (regra de escrita, conferida em C3:5004 e C4:2718).
- **Condições que ele sinalizou:** rede corporativa bloqueando downloads (C3:35) e máquina com pouca memória.
- **Padrões de erro do próprio assistente**, registrados no Jarbas:
  - ERR-GIT-001, `git checkout` apagando trabalho não commitado;
  - ERR-TOOL-011/016, heredoc/Python perdendo barras invertidas no Windows;
  - ERR-WEB-009, cadastro;
  - ERR-JAVA-014, login;
  - `BigDecimal.TWO` (Java 19 em projeto Java 17);
  - `serviceWorker.unregister()` do CRA;
  - Grid dentro de Stack no MUI;
  - Git Bash reescrevendo caminhos com "/".
