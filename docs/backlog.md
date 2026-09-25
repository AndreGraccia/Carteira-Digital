# Backlog e fluxo de entrega da Wall.ia

Criado em 25/09/2026. Junta tudo o que foi planejado, idealizado e achado até aqui: [`plano-de-evolucao.md`](plano-de-evolucao.md) (Sprints 0 a 10), [`analise-2026-09-25.md`](analise-2026-09-25.md), [`estrategia-produto.md`](estrategia-produto.md), as auditorias de 24/09 em [`auditoria-2026-09-24/`](auditoria-2026-09-24/), os ADRs 0001 a 0014 e a auditoria do ChatGPT de 25/09. Cada item foi conferido no código em 25/09.

A partir daqui, **este arquivo é a fonte da verdade do que falta**. O plano de evolução fica como histórico das Sprints 0 a 10.

Premissa: o código vai para produção. A homologação existe para validar tudo antes disso, então cada item é tratado com o critério de produção.

## 1. Fluxo

### Ambientes

- **Dev (local).** `docker compose up db mailpit` mais backend na 8082 e frontend na 3001. Open Finance no modo `sandbox` (dados sintéticos). Mailpit recebe os e-mails.
- **Homologação.** A mesma imagem que vai para produção, com `docker-compose.yml` mais `docker-compose.prod.yml` e o arquivo `.env.homolog`. Mailpit é aceito aqui, mas declarado (`mail_host=mailpit`), nunca por omissão. Open Finance em sandbox. Dados de teste, nunca dados reais de clientes.
- **Produção.** Mesma imagem da homologação, com e-mail transacional real, TLS no proxy da borda, backup diário testado e Open Finance só depois da épica E5.

### Caminho de uma mudança

1. Item sai do backlog com critério de aceite escrito (Definition of Ready).
2. Branch `feat/<id>-<nome>` a partir de `feat/wallia` (enquanto ela não entra na `main`; depois, a partir da `main`).
3. Teste que falha antes da correção. Depois a correção.
4. Pipeline local igual ao CI (seção 3). Commit em pt-BR.
5. Push e CI verde no GitHub. Sem CI conferido, o item não fecha.
6. Implantação na homologação e roteiro de validação do item.
7. Só itens validados na homologação entram na versão que vai para produção, com CHANGELOG e plano de volta (rollback).

### Definition of Ready

- Diz o problema, a fonte (arquivo, relatório, lei) e o critério de aceite verificável.
- Diz o que muda no banco (migração nova, nunca edição de migração aplicada).
- Quando há tela: estados de carregando, vazio, erro e sucesso, em PT e EN, em 375 px e 1280 px.

### Definition of Done (critério de produção)

- Teste automatizado que falhava antes e passa depois. Regra de dinheiro com teste de centavos.
- `mvnw clean test` verde, com os testes de integração rodando (Docker ou CI). Frontend: lint, Prettier, i18n, testes e build.
- CI do GitHub verde e conferido.
- Nenhum segredo em código, log ou commit. Dado pessoal fora dos logs.
- Migração Flyway nova aplica em banco vazio e em banco com dados.
- Documentação atualizada (CHANGELOG, guia, ADR quando houver decisão).
- Validado na homologação.

## 2. Épicos e itens

Prioridade: **P0** bloqueia a homologação, **P1** bloqueia produção, **P2** bloqueia crescer em usuários, **P3** diferencial ou melhoria.

### E1. Pipeline e qualidade

- **B01 (P0) CI verde conferido.** Push do `168a6cb` e leitura do CI com `gh`. Instalar: `winget install GitHub.cli` e `gh auth login`.
- **B02 (P0) Triagem do npm audit.** 68 alertas no CI (3 críticos, 33 altos). Rodar `npm audit --omit=dev`, corrigir o que vai para o navegador, registrar por que o resto não se aplica. Remover `web-vitals` do `package.json`. Precisa do registry npm.
- **B03 (P1) Actions atualizadas.** `checkout@v4` e `setup-node@v4` avisam runtime Node 20 depreciado.
- **B04 (P1) `@WebMvcTest` por controller.** Hoje existe 1 para 10 controllers. Cobrir status HTTP, validação e acesso de outro usuário (403/404).
- **B05 (P1) E2E com Playwright no CI.** Cadastro com código do Mailpit, login, Futuro, conectar Open Finance sandbox, revogar, excluir conta.
- **B06 (P2) Placar de precisão.** Primeira leitura real em 01/10/2026.

### E2. Exatidão financeira

- **B10 (P1) Parcela perde centavos.** `OpenFinanceImportService` divide com `RoundingMode.DOWN`: R$ 100,00 em 3 vira R$ 99,99 no futuro projetado. Aceite: a soma das parcelas projetadas é igual ao total da compra.
- **B11 (P1) Transferência entre contas próprias só por valor e dia.** `InsightsService.pairKey` junta entrada e saída independentes. Aceite: só pareia entre carteiras Open Finance diferentes do mesmo usuário, e um par ambíguo (mais de um candidato) não é escondido.
- **B12 (P2) Estados de transação.** Regras e testes para pendente, efetivada, estornada e reembolsada, e pagamento de fatura que não pode contar duas vezes com as compras da fatura.
- **B13 (P3) Alerta de cobrança duplicada ou estranha.** Diferencial citado na pesquisa de mercado; usa só dados que já existem.
- **B14 (P3) "Pix no crédito" como dívida no planejador de quitação.** CET perto de 8% ao mês (Agência Brasil, dez/2025).
- **B15 (P3) Fila da estratégia:** SCR/Registrato, tarifas, quitação do cartão.

### E3. Segurança e LGPD

- **B20 (P1) Exportação dos dados do titular.** LGPD art. 18 (acesso e portabilidade). Hoje só existe a exclusão. Aceite: `GET /api/v1/account/export` devolve JSON com perfil, carteiras, lançamentos, compromissos, metas e consentimentos do próprio usuário.
- **B21 (P1) Tela Privacidade fiel ao que a exclusão faz.** A exclusão pseudonimiza e retém usuário, carteiras e histórico (ADR 0012). A tela precisa dizer isso, e o que é apagado de fato.
- **B22 (P1) Política de retenção.** Por dado: quanto tempo, por quê, e como é apagado. Vira ADR.
- **B23 (P1) Rotação da chave de criptografia.** `FieldCipher` usa `enc:v1:` sem id de chave. Prefixo `enc:v2:<id>:`, leitura das duas versões e a chave dentro do plano de backup.
- **B24 (P1) Gemini.** Só dados agregados, opcional, e registro da transferência internacional (ANPD).
- **B25 (P1) Plano de resposta a incidente.** Quem avisa, em quanto tempo (ANPD, Resolução de comunicação de incidente), como revogar credenciais.
- **B26 (P2) `CascadeType.ALL` em `User.wallets`.** Caminho novo que apague usuário levaria carteiras e, por elas, lançamentos da contraparte.

### E4. Plataforma e operação (produção)

- **B30 (P0) Ambiente de homologação definido.** `.env.homolog.example`, `mail_host` obrigatório (Mailpit declarado na homologação), roteiro de subida.
- **B31 (P0) Health check.** Actuator com `/actuator/health` (liveness e readiness), `healthcheck` no compose e `depends_on` com `condition: service_healthy`, para o backend não subir antes do banco.
- **B32 (P1) Backup e restauração testados.** `pg_dump` diário, retenção, e um teste de restauração documentado junto com a chave de criptografia.
- **B33 (P1) TLS e HSTS.** O nginx do frontend escuta HTTP na 3000. Documentar o proxy da borda com TLS e ligar HSTS lá.
- **B34 (P1) Imagem de runtime.** Backend compila com Java 21 e roda em `eclipse-temurin:17-jre`. Alinhar a versão e regenerar as imagens.
- **B35 (P1) Migração para Spring Boot 4.1 e Java 25.** A linha 3.5 ficou sem suporte gratuito em 30/06/2026. Primeiro o último 3.5, zerar depreciações, depois 4.x.
- **B36 (P2) Observabilidade.** Métricas (Micrometer), logs estruturados sem dado pessoal, alerta quando o job diário falha ou demora.
- **B37 (P2) Limites em Redis.** Só quando houver mais de uma instância da API (gatilho da estratégia).

### E5. Open Finance real

Nada aqui liga dados reais antes de E3 e E4 fechadas.

- **B40 (P1) ADR: provedor.** Participante próprio não (decisão fechada). Comparar agregador habilitado (Pluggy e outros): custo, cobertura, contrato, saída.
- **B41 (P1) Várias instituições por usuário.** Hoje há um gateway por instalação. Com várias, `revoke`, `expireIfDue` e `endedAtTheInstitution` precisam agir por conexão, e `purge(userId)` precisa virar limpeza por instituição (hoje apaga tudo do usuário).
- **B42 (P1) Consentimento como registro próprio.** Finalidade, versão do texto mostrado, escopos concedidos e eventos em tabela, não em texto separado por vírgula.
- **B43 (P1) Contrato do gateway.** Criar consentimento, retorno do banco, troca de código, consultar, revogar. Hoje o gateway só busca dados.
- **B44 (P2) Sync incremental.** Carga inicial limitada e depois só o novo (`/transactions-current`), com cursor salvo depois da gravação.
- **B45 (P2) Fila e concorrência limitada.** `OpenFinanceDailySync` é sequencial. Respeitar `Retry-After` e 429.
- **B46 (P2) ADR eventos contra polling.** 61,5% das consultas de saldo por polling não trazem novidade (Zetta, set/2026).

### E6. Experiência

- **B50 (P1) Estados de dado atrasado.** Instituição, horário da última atualização e aviso quando o dado está velho.
- **B51 (P1) Validação da tela Futuro em 375 px.** As seções recolhíveis entraram na Sprint 10; falta medir no navegador.
- **B52 (P2) Rótulo de origem em todo número.** Manual, importado, simulado ou previsto.
- **B53 (P3) Idioma sob demanda.** `i18n.js` tem 109 KB. Só depois de medir o pacote com gzip.
- **B54 (P3) Dividir `FutureService` (979 linhas).** Só quando uma mudança real doer. Não ganha desempenho.

### Pendências de fonte

- Normas citadas só pelo ChatGPT (IN BCB 759/760/720, Res. 526/541, IN 746/2026). Confirmar no site do BCB antes de citar em tela ou ADR.

## 3. Pipeline local igual ao CI

Backend: `./mvnw -B -ntp clean test` (com Docker ligado, os testes de integração rodam).

Frontend:

```
npm run lint
npx prettier --check --end-of-line auto src
npm run test:i18n
CI=true npm test -- --watchAll=false
npm run build
```

No Windows, o `--end-of-line auto` evita os falsos alarmes de CRLF (ERR-TOOL-023 no Jarbas).

## 4. Sprints

Uma sprint por sessão, com commit verde ao fim.

- **Sprint 11: homologação possível.** B30, B31, B10, B11, B01. B02 e B03 se houver rede para o npm.
- **Sprint 12: dados do titular.** B20, B21, B22, B26.
- **Sprint 13: produção segura.** B23, B25, B32, B33, B34.
- **Sprint 14: testes de ponta a ponta.** B04, B05, B51, B50.
- **Sprint 15: plataforma nova.** B35 (Boot 4.1 e Java 25), B36.
- **Sprint 16: Open Finance preparado.** B40, B41, B42, B43.
- **Sprint 17: Open Finance em escala.** B44, B45, B46, B12.
- **Sprint 18: diferenciais.** B13, B14, B52, e o que o placar de precisão indicar.

A versão 1.0 vai para produção ao fim da Sprint 14, sem Open Finance real (só manual e sandbox rotulado). Open Finance real entra numa versão seguinte, depois da Sprint 17 e da análise jurídica.
