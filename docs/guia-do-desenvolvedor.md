# Guia do desenvolvedor

Este guia leva você de "clonei o repositório" até "criei uma tela nova com endpoint, migração e teste". Ele parte do que deu errado de verdade na primeira vez que alguém seguiu o README nesta máquina: portas ocupadas por outros projetos, um volume antigo do banco com outra senha e o `jwt_secret` ainda com o texto de exemplo. Cada um desses casos tem uma seção em [Problemas comuns](#problemas-comuns).

Comandos em PowerShell, a partir da raiz do projeto (a pasta que tem `backend/`, `frontend/` e `docker-compose.yml`), salvo quando o bloco disser outra coisa.

## Sumário

1. [Como as peças conversam](#1-como-as-peças-conversam)
2. [Pré-requisitos](#2-pré-requisitos)
3. [Configuração: o arquivo .env.properties](#3-configuração-o-arquivo-envproperties)
4. [Portas: conferir e trocar](#4-portas-conferir-e-trocar)
5. [Subir o banco e o Mailpit](#5-subir-o-banco-e-o-mailpit)
6. [Subir o backend](#6-subir-o-backend)
7. [Subir o frontend](#7-subir-o-frontend)
8. [Primeiro uso](#8-primeiro-uso)
9. [Problemas comuns](#problemas-comuns)
10. [Como uma requisição atravessa o sistema](#10-como-uma-requisição-atravessa-o-sistema)
11. [Receitas para construir coisas novas](#11-receitas-para-construir-coisas-novas)
12. [Testes](#12-testes)
13. [Fluxo de Git](#13-fluxo-de-git)
14. [Referência rápida](#14-referência-rápida)

## 1. Como as peças conversam

```text
Navegador
  |  http://localhost:3000  (React, npm start)
  v
Frontend React  ----- axios, REACT_APP_API_URL ----->  API Spring Boot  (porta 8080)
                                                        |    |     |
                                   JDBC (porta 5432) <--+    |     +--> Gemini (opcional, só com chave)
                                            |                |
                                        PostgreSQL        SMTP (porta 1025)
                                        (Docker)             |
                                                          Mailpit (Docker, caixa de e-mail falsa
                                                                   em http://localhost:8025)
```

| Peça | O que faz | Onde está | Porta padrão |
| --- | --- | --- | --- |
| Frontend | Telas, rotas, traduções PT/EN. Guarda o token JWT no navegador e manda em cada chamada. | `frontend/` | 3000 |
| Backend | Regras de negócio, segurança, motor de projeção, sandbox Open Finance. | `backend/` | 8080 |
| PostgreSQL | Usuários, carteiras, transações, dados importados, respostas sobre recorrências. | container `wallia-db` | 5432 |
| Flyway | Cria e atualiza as tabelas quando o backend sobe (uma por arquivo em `db/migration`). | `backend/src/main/resources/db/migration/` | - |
| Mailpit | Recebe o código de 8 dígitos da conexão Open Finance. Nenhum e-mail sai da máquina. | container `wallia-mailpit` | 1025 (SMTP) e 8025 (web) |
| Sandbox Open Finance | Gera contas, cartão, faturas e empréstimo de teste a partir da data de hoje. | `backend/.../service/openfinance/ContractSandboxOpenFinanceGateway.java` | - |
| Gemini | Só escreve o texto do relatório da tela Análise. Sem chave, uma análise local faz o mesmo papel. | `backend/.../service/InsightsService.java` | - |

Duas regras seguram o desenho inteiro:

- O frontend nunca decide nada sobre dinheiro. Toda validação importante existe no backend, e as mais críticas também no banco (restrições da V7).
- O motor de projeção (`service/future/`) é Java puro, sem Spring e sem IA. Ele recebe fatos e devolve números. Motivo registrado no [ADR 0001](adr/0001-motor-futuro-deterministico.md).

## 2. Pré-requisitos

| Ferramenta | Versão | Como conferir | Observação |
| --- | --- | --- | --- |
| Git | qualquer recente | `git --version` | |
| Java (JDK) | 17 ou maior | `java -version` | O projeto compila para 17; JDK 21 funciona. |
| Maven | não precisa instalar | - | O `backend/mvnw.cmd` baixa a versão certa na primeira execução. |
| Node.js | LTS (18 ou 20) | `node --version` | |
| npm | vem com o Node | `npm.cmd --version` | No PowerShell use `npm.cmd`; `npm` pode ser bloqueado pela política de scripts. |
| Docker Desktop | qualquer recente | `docker --version` e `docker ps` | O `docker ps` precisa responder sem erro. Se disser que não achou o `dockerDesktopLinuxEngine`, o Docker Desktop está fechado. |

Rede corporativa ou proxy: o primeiro `mvnw`, o `npm.cmd install` e o download das imagens Docker precisam de internet. Se a rede bloquear, rode esses três passos uma vez em outra rede; depois disso tudo fica em cache e o projeto roda offline.

## 3. Configuração: o arquivo .env.properties

```powershell
Copy-Item .env.example.properties .env.properties
notepad .env.properties
```

O backend lê esse arquivo da raiz ou de `backend/` (`spring.config.import` em `application.yml`). O Docker Compose também lê, pelo `--env-file`. Ele está no `.gitignore` e nunca deve ir para o Git.

| Chave | Para que serve | Valor de exemplo | Precisa trocar? |
| --- | --- | --- | --- |
| `db_name` | Nome do banco | `wallia` | Não |
| `db_username` | Usuário do banco | `wallia` | Não |
| `db_password` | Senha do banco | texto de exemplo | Sim, antes do primeiro `docker compose up` |
| `jwt_secret` | Assina os tokens de login | texto de exemplo | Sim. O backend não sobe com o texto de exemplo |
| `frontend_origin` | Endereço do frontend que o backend aceita (CORS) | `http://localhost:3000` | Só se mudar a porta do frontend |
| `db_port` | Porta do banco na sua máquina | `5432` | Só se estiver ocupada |
| `backend_port` | Porta da API | `8080` | Só se estiver ocupada |
| `frontend_port` | Porta do frontend quando roda em container | `3000` | Só se estiver ocupada |
| `gemini_api_key` | Chave da Gemini | vazio | Opcional |
| `open_finance_mode` | `contract-sandbox` (local) ou `participant-sandbox` (credenciais reais de sandbox) | `contract-sandbox` | Não |
| `future_alert_cron` | Quando o job de alertas por e-mail roda (cron do Spring, horário de Brasília) | `0 0 8 * * *` | Só para testar; `-` desliga |

Chaves que não aparecem no exemplo, mas existem com valor padrão em `application.yml`: `mail_host` (localhost), `mail_port` (1025), `open_finance_code_ttl_minutes` (10), `open_finance_resend_cooldown_seconds` (60), `open_finance_code_max_attempts` (5).

### Gerar os dois segredos

No PowerShell, cada bloco imprime um valor aleatório. Cole o primeiro em `jwt_secret` e o segundo em `db_password`:

```powershell
# jwt_secret: Base64 de 64 bytes (512 bits)
$b = New-Object byte[] 64; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)

# db_password: 24 bytes em hexadecimal
$b = New-Object byte[] 24; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); ($b | ForEach-Object { $_.ToString('x2') }) -join ''
```

O `jwt_secret` precisa ser Base64 com pelo menos 32 bytes (256 bits). O `JwtUtils` confere isso quando o backend sobe e para com uma mensagem clara se não for.

Não cole os segredos em chat, issue, print ou commit.

## 4. Portas: conferir e trocar

Se você tem outros projetos nesta máquina, as portas 5432, 8080 e 3000 podem já estar em uso. Confira antes:

```powershell
Get-NetTCPConnection -State Listen -LocalPort 3000,3001,5432,8080,8025,1025 -ErrorAction SilentlyContinue |
  Select-Object LocalPort, OwningProcess, @{n='Processo';e={(Get-Process -Id $_.OwningProcess).ProcessName}}
docker ps --format "table {{.Names}}\t{{.Ports}}"
```

Uma porta de container aparece como `com.docker.backend` no primeiro comando. O segundo diz qual container é.

Para trocar, altere os três lugares juntos. Exemplo real desta máquina, onde 5432, 8080 e 3000 já eram de outros projetos:

`.env.properties`:

```properties
db_port=5434
backend_port=8082
frontend_origin=http://localhost:3001
```

`frontend/.env.local` (crie a partir de `frontend/.env.example`; também é ignorado pelo Git):

```properties
PORT=3001
REACT_APP_API_URL=http://localhost:8082/api/v1
```

Por que três lugares: o banco escuta em `db_port`, o backend escuta em `backend_port` e só aceita chamadas vindas de `frontend_origin`, e o React só sabe o endereço da API pelo `REACT_APP_API_URL`, lido quando o `npm start` começa. Mudou o `.env.local`? Pare o `npm start` com Ctrl+C e rode de novo.

## 5. Subir o banco e o Mailpit

Abra o Docker Desktop e espere o ícone ficar estável. Depois:

```powershell
docker compose --env-file .env.properties up -d db mailpit
docker compose --env-file .env.properties ps
docker compose --env-file .env.properties exec db sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Saída esperada no último comando: `accepting connections`.

O Mailpit fica em http://localhost:8025. É lá que chega o código da conexão Open Finance.

## 6. Subir o backend

Em um segundo terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

A primeira execução baixa o Maven e as dependências (alguns minutos). Espere a linha:

```text
Started WalliaApplication in 5.2 seconds
```

Confira em outro terminal (troque 8080 se mudou `backend_port`):

```powershell
curl.exe -s -o NUL -w "%{http_code}`n" http://localhost:8080/api/v1/future/overview
```

`401` é o resultado certo: a API está no ar e recusou porque não há login. O Swagger fica em http://localhost:8080/swagger-ui/index.html.

Pelo VS Code ou IntelliJ, rode a classe `WalliaApplication` com a pasta `backend` como diretório de trabalho. Só uma instância pode usar a porta.

## 7. Subir o frontend

Em um terceiro terminal:

```powershell
cd frontend
npm.cmd install
npm.cmd start
```

O `npm.cmd install` só é necessário na primeira vez e quando o `package.json` mudar. O arquivo `frontend/.npmrc` já tem `legacy-peer-deps=true`, necessário por causa das versões do react-scripts.

Espere `Compiled successfully!` e abra o endereço que aparecer (http://localhost:3000, ou a `PORT` do `.env.local`).

## 8. Primeiro uso

1. Clique em **Criar conta**. Usuário de 3 a 20 caracteres (letras, números, ponto, hífen e sublinhado; sem `@`), senha de 10 a 100.
2. Entre com o usuário criado.
3. Abra **Open Finance** e clique em **Conectar dados de teste**.
4. Abra http://localhost:8025, copie o código de 8 dígitos e cole na tela.
5. A conexão importa 2 contas, cartão com faturas e 1 empréstimo do banco fictício "Banco Aurora Sandbox".
6. Abra **Futuro**. Aparecem 8 perguntas sobre recorrências (salário, aluguel, escola, contas). Responda.
7. Depois de confirmar, o gráfico mostra o saldo dia a dia e, se houver, a janela em que ele fica negativo, com o botão "ver por quê".

O roteiro completo, com as proteções do código por e-mail, está em [open-finance-email-tests.md](open-finance-email-tests.md).

8. O código do passo 4 também confirma o e-mail. Só depois dele o aviso por e-mail da tela Futuro pode ser ligado.
9. **Envelopes** separa parte do saldo; o limite diário do Futuro passa a deixar esse valor de fora.
10. **Quanto a Wall.ia acertou** aparece depois do primeiro sync com perguntas respondidas. A primeira comparação sai 7 dias depois do sync seguinte.

### Avisos no navegador (Web Push)

Ficam desligados até existirem as chaves VAPID no `.env.properties`. Gere um par uma vez por ambiente e não troque depois: os navegadores ficam inscritos nessa chave pública, e trocar derruba todas as inscrições.

```powershell
node -e "const e=require('node:crypto').createECDH('prime256v1');e.generateKeys();console.log('vapid_public_key='+e.getPublicKey().toString('base64url'));console.log('vapid_private_key='+Buffer.concat([Buffer.alloc(32),e.getPrivateKey()]).subarray(-32).toString('base64url'))"
```

Cole as duas linhas no `.env.properties`, reinicie o backend e, na tela Futuro, use "Ativar aqui" abaixo do aviso. Navegador só aceita push em `https` ou em `localhost`. O servidor só envia para os serviços de push dos navegadores (Google, Mozilla, Apple, Microsoft); qualquer outro endereço de inscrição é recusado.

### Ter um administrador

Desde a V19 não existe admin pronto: as contas `johndoe` e `lindacalvin` do template original tinham senha pública e foram desativadas. Para promover uma conta sua, no banco local:

```sql
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM "user" u, role r
WHERE lower(u.username) = 'seu_usuario' AND r.type = 'ROLE_ADMIN';
```

Entre de novo para o token trazer o papel novo. Todo acesso de admin fica em `admin_audit` ([ADR 0004](adr/0004-auditoria-admin-e-ledger.md)).

## Problemas comuns

### "Não consigo criar conta" ou toda chamada dá erro de rede

Quase sempre o frontend está falando com a API errada. Abra o DevTools do navegador (F12), aba Rede, e tente de novo. Olhe o endereço da chamada `signup`:

- Porta diferente da `backend_port`: ajuste `REACT_APP_API_URL` no `frontend/.env.local` e reinicie o `npm start`.
- Porta certa, mas erro de CORS no console: `frontend_origin` no `.env.properties` não é igual ao endereço da barra do navegador (inclusive a porta). Corrija e reinicie o backend.
- Resposta 422: o formulário tem campo inválido. A mensagem aparece embaixo do campo.
- Resposta 409 ou "já existe": usuário ou e-mail já cadastrado.

Para testar a API sem o navegador:

```powershell
$body = '{"firstName":"Ana","lastName":"Teste","username":"ana2026","email":"ana@example.test","password":"Teste@12345"}'
curl.exe -s -i -X POST http://localhost:8080/api/v1/auth/signup -H "Content-Type: application/json" -H "Origin: http://localhost:3000" -d $body
```

`201` e o cabeçalho `Access-Control-Allow-Origin` com o seu endereço indicam backend e CORS corretos.

### Porta já em uso

`Port 8080 was already in use` no backend, `Something is already running on port 3000` no React, ou `Bind for 0.0.0.0:5432 failed` no Docker. Veja quem ocupa (seção 4) e troque a porta do Wall.ia. Não encerre processos de outros projetos sem saber o que são.

### `password authentication failed for user "wallia"`

O PostgreSQL só cria usuário e senha na primeira vez que o volume é criado. Se o volume já existia (de uma versão antiga do projeto, com outro nome de banco ou outra senha), mudar o `.env.properties` depois não muda nada lá dentro.

Primeiro veja o que há no volume:

```powershell
docker compose --env-file .env.properties exec db sh -c 'psql -U "$POSTGRES_USER" -d postgres -c "\l" -c "\du"'
```

Duas saídas, conforme o caso:

- O volume tem dados que importam: crie o usuário e o banco que o `.env.properties` pede, sem apagar nada. Rode `psql` como o superusuário listado em `\du` e use `CREATE ROLE ... LOGIN PASSWORD ...` e `CREATE DATABASE ... OWNER ...`.
- O volume é descartável (só dados de teste): `docker compose --env-file .env.properties down -v` apaga o volume, e o próximo `up` cria tudo do zero com a senha atual. Isso não tem volta.

### `jwt_secret must be a Base64 value`

O `jwt_secret` ainda é o texto de exemplo, tem caracteres fora do Base64 (como `_`) ou é curto demais. Gere um novo (seção 3). Se editou o arquivo no Windows e o erro continua, confira se não sobrou espaço ou aspas no fim da linha.

### O backend sobe e para logo depois

Leia as últimas 30 linhas do terminal. As causas mais comuns, nessa ordem: banco fora do ar (`Connection refused`: rode a seção 5), senha errada (acima), `jwt_secret` inválido (acima), migração com checksum diferente (abaixo).

### `Validate failed: Migration checksum mismatch`

Alguém editou uma migração que já tinha rodado. Regra do projeto: migração aplicada nunca muda. Desfaça a edição e crie uma migração nova (receita 11.2).

### `npm ERR! ERESOLVE`

Falta o `frontend/.npmrc` com `legacy-peer-deps=true`, ou você rodou o install fora da pasta `frontend`.

### O código do Open Finance não chega

Confira se o Mailpit está de pé (`docker ps`) e aberto em http://localhost:8025. Pedir um código novo exige esperar 60 segundos, e o pedido invalida o anterior. Cinco erros bloqueiam o código até ele vencer (10 minutos).

### Containers antigos `e-wallet-*`

Se o Docker mostrar `e-wallet-frontend` ou `e-wallet-backend`, são da versão antiga do projeto e ocupam portas. Pare com `docker stop e-wallet-frontend e-wallet-backend`. Para removê-los, use `docker rm`.

## 10. Como uma requisição atravessa o sistema

Exemplo: abrir a tela Futuro com horizonte de 60 dias.

| Passo | Arquivo | O que acontece |
| --- | --- | --- |
| 1 | `frontend/src/App.js` | A rota `/future` renderiza `<Future />` dentro de `PrivateRoute` (exige login) e `ProtectedRoute` (exige `ROLE_USER`). |
| 2 | `frontend/src/pages/future/Future.js` | O `useEffect` chama `HttpService.getWithAuth('/future/overview?horizon=60')`. |
| 3 | `frontend/src/services/HttpService.js` e `AuthHeader.js` | Acrescenta `Authorization: Bearer <token>`. Um 401 vira a mensagem de sessão expirada (`errorKey` em `utils/wallet.js`); o `WalletContext` também encerra a sessão quando recebe 401. |
| 4 | `frontend/src/services/axios.js` | Monta a URL com `REACT_APP_API_URL`. |
| 5 | `backend/.../config/SecurityConfig.java` | Confere o CORS (`frontend_origin`) e exige autenticação para tudo fora de `/auth/**` e do Swagger. |
| 6 | `backend/.../security/AuthTokenFilter.java` e `JwtUtils.java` | Valida a assinatura e a validade do token e coloca o usuário no contexto. |
| 7 | `backend/.../controller/FutureController.java` | Aceita só horizonte 30, 60 ou 90. Pega o id do usuário do token, nunca da URL. |
| 8 | `backend/.../service/FutureService.java` | Busca saldos, recorrências confirmadas, cartões e empréstimos do usuário. |
| 9 | `backend/.../service/future/CashEventPlanner.java` | Converte esses fatos em eventos datados, cada um com certeza CONFIRMED, ESTIMATED ou HYPOTHETICAL. |
| 10 | `backend/.../service/future/ProjectionEngine.java` | Soma dia a dia, acha o menor saldo e as janelas negativas. |
| 11 | `backend/.../dto/response/FutureResponses.java` | Formato do JSON devolvido, com `Cache-Control: no-store`. |
| 12 | `frontend/src/components/projection-chart/` e `future-ui/` | Desenham o gráfico e as linhas de evento. |

Erros seguem outro caminho: exceções de regra (`WalletRuleException` e parecidas) chegam ao `exception/GlobalExceptionHandler.java`, que devolve um código de erro estável. O frontend traduz esse código com `errorKey` em `utils/wallet.js` e as chaves de `i18n.js`. Validação de corpo inválido volta como 422.

## 11. Receitas para construir coisas novas

### 11.1 Endpoint novo

1. DTOs em `dto/request` e `dto/response`. Records Java funcionam bem (veja `FutureRequests.java`).
2. Regra no service. Receba o `userId` como parâmetro; nunca confie em id de usuário vindo do corpo ou da URL.
3. Se o endpoint lê ou mexe em carteira, passe pelo `WalletAccess` (`security/WalletAccess.java`), que confere se a carteira é do usuário.
4. Controller fino: valida entrada (`@Valid`), chama o service, devolve `ResponseEntity`. Pegue o usuário com `@AuthenticationPrincipal UserDetailsImpl user`.
5. Teste do service em `backend/src/test/java/...`, com Mockito, como `TransactionServiceTest`. Cubra pelo menos o caso de outro usuário tentando acessar.
6. Confira no Swagger.

### 11.2 Mudança no banco (migração nova)

1. Veja o último número em `backend/src/main/resources/db/migration/` e crie o próximo, por exemplo `V15__descricao_curta.sql`. Dois sublinhados depois do número.
2. Escreva SQL que funcione em banco com dados: `ALTER TABLE ... ADD COLUMN ... DEFAULT ...`, nunca `DROP` sem plano.
3. Coloque restrições no banco quando a regra for de dinheiro (`CHECK (amount > 0)`, como a V7).
4. Suba o backend. O Flyway aplica sozinho e registra em `flyway_schema_history`.
5. Atualize a entidade em `domain/entity`. O Hibernate não cria tabela (`ddl-auto: none`); o Flyway é a única fonte do esquema.
6. Nunca edite uma migração que já rodou em outro ambiente. Errou na sua antes de ela chegar ao `main`? Apague o banco local de teste e rode de novo. Depois de chegar ao `main`, corrija com uma migração seguinte.

### 11.3 Tela nova com rota e menu

1. Crie `frontend/src/pages/minha-tela/MinhaTela.js`. Use `SectionCard`, `EmptyState` e demais peças de `components/wallet-ui` para manter o visual.
2. Rota em `App.js`, dentro do bloco do `DashboardLayout`.
3. Item de menu em `layouts/dashboard/DashboardLayout.js` (`mobile: true` põe o item na barra inferior do celular, que já tem 5 itens; sem essa marca ele aparece no menu lateral do desktop e, no celular, no menu que abre pelo avatar).
4. Textos em `i18n.js`, nas duas línguas (receita 11.4).
5. Título da aba com `<Helmet><title>... | Wall.ia</title></Helmet>`.
6. Estados de carregando, vazio e erro. Toda tela que busca dado tem os três.

### 11.4 Texto novo com tradução

Toda chave existe em `pt` e em `en` dentro de `frontend/src/i18n.js`. Use `t('app.minhaTela.titulo')` no componente. Depois rode:

```powershell
cd frontend
npm.cmd run test:i18n
npm.cmd run test:wallet
```

O `test:wallet` compara as chaves PT e EN e falha se faltar alguma.

### 11.5 Mudar a massa do sandbox Open Finance

Tudo está em `ContractSandboxOpenFinanceGateway.java`. As datas são geradas a partir do `Clock` injetado, então a massa acompanha o dia de hoje. O teste `ContractSandboxOpenFinanceGatewayTest` trava o cenário de 22/09/2026 com um `Clock` fixo. Mudou a massa? Atualize o teste e os números de `open-finance-email-tests.md`.

### 11.6 Regra nova no motor de projeção

O pacote `service/future` não depende de Spring nem de banco, então dá para testar com `new` e datas fixas (veja `ProjectionEngineTest`). Escreva o teste primeiro com um cenário pequeno: saldo, dois eventos e o resultado esperado em centavos.

## 12. Testes

| Comando | Onde | O que cobre | Tempo |
| --- | --- | --- | --- |
| `.\mvnw.cmd test` | `backend` | Serviços, autorização, motor de projeção, sandbox, código por e-mail | cerca de 1 min |
| `npm.cmd test -- --watchAll=false` | `frontend` | Formulário de movimentação, Open Finance, utilitários | cerca de 30 s |
| `npm.cmd run lint` | `frontend` | ESLint | segundos |
| `npm.cmd run test:i18n` | `frontend` | Troca de idioma, plurais, tipos desconhecidos | segundos |
| `npm.cmd run test:wallet` | `frontend` | Regras monetárias e paridade PT/EN | segundos |
| `npm.cmd run build` | `frontend` | Compila como em produção | cerca de 1 min |

Antes de abrir um Pull Request, rode todos.

## 13. Fluxo de Git

```powershell
git switch main
git pull origin main
git switch -c feat/minha-mudanca
# ... código, testes ...
git status
git add caminho/do/arquivo
git commit -m "feat: descreve a mudança"
git push -u origin feat/minha-mudanca
```

Prefira `git add` com caminhos a `git add .`, para não levar `.env.properties`, prints ou arquivos de contexto sem querer.

Cuidado com `git checkout <arquivo>` e `git restore <arquivo>`: eles descartam mudanças não commitadas sem pedir confirmação. Na dúvida, `git stash` guarda tudo e deixa recuperar depois.

## 14. Referência rápida

| Quero... | Comando |
| --- | --- |
| Subir banco e Mailpit | `docker compose --env-file .env.properties up -d db mailpit` |
| Parar banco e Mailpit, mantendo dados | `docker compose --env-file .env.properties stop` |
| Ver logs do banco | `docker compose --env-file .env.properties logs -f db` |
| Abrir o banco no terminal | `docker compose --env-file .env.properties exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'` |
| Subir a API | `cd backend; .\mvnw.cmd spring-boot:run` |
| Subir a interface | `cd frontend; npm.cmd start` |
| Tudo em containers | `docker compose --env-file .env.properties -f docker-compose.yml -f docker-compose.prod.yml up --build` |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| Caixa de e-mail de teste | http://localhost:8025 |
| Ver quem ocupa uma porta | `Get-NetTCPConnection -State Listen -LocalPort 8080` |
