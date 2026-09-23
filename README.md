# Wall.ia

Uma carteira que entende seus compromissos, explica de onde cada número veio e deixa você testar o futuro antes de mexer no dinheiro.

React, Spring Boot e PostgreSQL, com Open Finance em sandbox. Nenhum valor real é movimentado.

> Projeto em desenvolvimento. Não use para dinheiro real.

Primeira vez rodando, ou algo não subiu? O [guia do desenvolvedor](docs/guia-do-desenvolvedor.md) explica cada peça, as portas, os erros mais comuns e como criar telas, endpoints e migrações.

## O problema

82% das famílias brasileiras estão endividadas e 29,9% têm contas em atraso (Peic/CNC, ago/2026). O cartão aparece em 84,6% dessas dívidas, e 42,6% das compras no crédito são parceladas: dinheiro que já tem destino nos próximos meses, mas não aparece no saldo.

Os apps de controle financeiro no Brasil mostram, no máximo, o saldo previsto no fim do mês. Quem termina o mês no positivo ainda pode ficar sem dinheiro entre o vencimento da fatura e o salário. É esse buraco que o Wall.ia mostra.

Os números, as fontes e as projeções de negócio (três cenários de 36 meses, teto de CAC) estão em [docs/pesquisa-mercado.md](docs/pesquisa-mercado.md).

## Como funciona

1. **Dados vêm do Open Finance.** Contas, saldos, transações, cartões, faturas, parcelas e empréstimos (sandbox local com estrutura próxima das APIs reais).
2. **O sistema pergunta em vez de supor.** Uma entrada de R$ 4.200 da Concentrix S.A. todo dia 5 vira a pergunta "isso é seu salário?", com o histórico que levou à suspeita. Só o que o usuário confirma entra nas contas.
3. **Java calcula, a IA explica, o usuário decide.** O motor de projeção é determinístico e testado; nenhum valor é inventado por modelo de linguagem. Decisão registrada em [ADR 0001](docs/adr/0001-motor-futuro-deterministico.md).

## Funcionalidades

**Futuro**

- Saldo agora, quanto já tem destino (separando confirmado de estimado) e margem livre.
- Saldo projetado dia a dia em 30, 60 ou 90 dias, com o menor saldo marcado.
- Alerta de janela negativa ("entre 03/10 e 04/10 seu saldo pode ficar em -R$ 324,87") e "ver por quê", que lista as contas que causam o buraco e a entrada que o fecha.
- Perguntas rápidas sobre recorrências detectadas, com evidência (valor, dia, vezes, conta de origem) e resposta em um toque.
- Modo Futuro: simula compra à vista ou parcelada, guardar todo mês ou perder uma renda, sem gravar nada.

**Cartões e dívidas**

- Fatura aberta e faturas futuras com as compras e parcelas que as compõem.
- Limite comprometido.
- Empréstimos: quanto falta, parcela, parcelas restantes, próximo vencimento e instituição de origem.

**Base da carteira**

- Cadastro e login com JWT, limite de tentativas.
- Carteiras, transferências e extrato, com autorização por dono da carteira.
- Conexão Open Finance protegida por código de 8 dígitos enviado ao e-mail cadastrado ([roteiro de teste](docs/open-finance-email-tests.md)).
- Análise do extrato com regras locais ou Gemini, sem enviar o extrato bruto ([segurança e IA](docs/security-and-ai-reports.md)).
- Interface em português e inglês, tema escuro, responsiva, validada em 1440 px e 390 px.
- Swagger em `/swagger-ui/index.html`.

## Próximos passos

Em ordem de valor para o usuário:

1. **Assistente somente leitura.** Chat que chama o motor como ferramenta ("posso comprar um notebook de R$ 4.000?") e responde com os números do backend. Não movimenta dinheiro. Tela de privacidade dizendo o que ele vê e o que não vê.
2. **Ledger de dupla entrada.** Todo débito com crédito correspondente, carteira encerrada sem apagar histórico, correção por estorno.
3. **Papéis administrativos.** Usuário, suporte, operações, auditoria e administrador, com trilha de auditoria. Administrador não movimenta dinheiro de cliente.
4. **Metas** entrando na projeção como compromisso.
5. **Categorias controladas** para gastos variáveis (mercado, transporte), hoje fora da projeção.

## Tecnologias

### Backend

- Java 17
- Spring Boot
- Spring Web
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- MapStruct
- Jakarta Validation
- Maven
- JUnit 5
- Mockito
- Springdoc OpenAPI

### Frontend

- React
- React Router
- Material UI
- Axios
- React Hook Form
- Notistack
- i18next (PT/EN)
- Node.js
- npm

### Infraestrutura

- Docker
- Docker Compose
- PostgreSQL
- Mailpit (caixa de e-mail local para o código de 8 dígitos)

## Estrutura do projeto

```text
Carteira-Digital/
├── backend/
│   ├── src/main/java/
│   │   └── com/github/yildizmy/
│   │       ├── config/
│   │       ├── controller/
│   │       ├── domain/
│   │       ├── dto/
│   │       ├── exception/
│   │       ├── repository/
│   │       ├── security/
│   │       ├── service/
│   │       │   ├── future/        # motor de projeção (sem Spring)
│   │       │   └── openfinance/   # sandbox e gateway
│   │       └── validator/
│   └── src/main/resources/
│       ├── db/migration/
│       └── application.yml
├── frontend/
│   ├── public/
│   └── src/
│       ├── components/
│       ├── layouts/
│       ├── pages/             # future/, commitments/, open-finance/...
│       ├── services/
│       └── theme/
├── docs/                      # guia do desenvolvedor, pesquisa, ADR, roteiros de teste
├── docker-compose.yml
├── docker-compose.prod.yml
├── .env.example.properties
└── README.md
```

## Pré-requisitos

Antes de iniciar, instale:

- Git
- Java 17 ou superior (o Maven vem pelo wrapper `mvnw`)
- Node.js LTS com npm
- Docker Desktop

Confira as instalações:

```powershell
git --version
java -version
node --version
npm.cmd --version
docker --version
```

## Configuração inicial

Clone o repositório:

```powershell
git clone https://github.com/AndreGraccia/Carteira-Digital.git
cd Carteira-Digital
```

Crie o arquivo local de configuração:

```powershell
Copy-Item .env.example.properties .env.properties
```

Abra o arquivo:

```powershell
notepad .env.properties
```

Configure os valores locais:

```properties
db_name=wallia
db_username=wallia
db_password=SUBSTITUA_PELA_SENHA_LOCAL
jwt_secret=SUBSTITUA_POR_UM_SEGREDO_ALEATORIO
frontend_origin=http://localhost:3000
gemini_api_key=
gemini_model=gemini-3.8-flash
gemini_search_grounding=true
db_port=5432
backend_port=8080
frontend_port=3000
```

As portas só precisam mudar se 5432, 8080 ou 3000 já estiverem em uso por outro programa. Nesse caso, troque também `frontend_origin` e crie `frontend/.env.local` a partir de `frontend/.env.example`. O passo a passo está na [seção de portas do guia](docs/guia-do-desenvolvedor.md#4-portas-conferir-e-trocar).

Troque `db_password` antes de subir o banco pela primeira vez: o PostgreSQL grava a senha só na criação do volume.

Para gerar um segredo JWT no PowerShell:

```powershell
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Copie o resultado para `jwt_secret`.

O arquivo `.env.properties` contém informações locais e está ignorado pelo Git. Nunca publique esse arquivo.

`gemini_api_key` é opcional. Com o campo vazio, a tela **Análise** continua funcionando com regras locais determinísticas. Para habilitar a Gemini, crie uma chave no Google AI Studio, salve-a somente nesse arquivo e reinicie o backend. A chave nunca deve usar o prefixo `REACT_APP_` nem ser colocada no frontend.

O relatório envia apenas totais mensais, direção das operações e contagem de movimentações. Nomes, e-mails, códigos de carteira, descrições e identificadores não são enviados. Consulte [segurança e relatórios com IA](docs/security-and-ai-reports.md) para entender o fluxo, os limites e as referências.

## Executando o PostgreSQL e o Mailpit

Abra o Docker Desktop e aguarde o mecanismo iniciar.

Na raiz do projeto, abra o primeiro PowerShell e execute:

```powershell
docker compose --env-file .env.properties up -d db mailpit
```

O Mailpit recebe o código de confirmação do Open Finance em http://localhost:8025. Nenhum e-mail sai da máquina.

Confira o container:

```powershell
docker compose --env-file .env.properties ps
```

Teste a disponibilidade do banco:

```powershell
docker compose --env-file .env.properties exec db sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

O resultado esperado deve conter:

```text
accepting connections
```

## Executando o backend

Abra um segundo PowerShell, entre na pasta do backend e inicie o Spring Boot:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Se você já iniciou `WalliaApplication` pelo VS Code, pule esse comando. Apenas uma instância pode usar a porta `8080` (ou a `backend_port`) ao mesmo tempo.

Aguarde a linha `Started WalliaApplication`. Se o processo parar antes disso, veja [Problemas comuns](docs/guia-do-desenvolvedor.md#problemas-comuns).

O backend estará disponível em:

```text
http://localhost:8080
```

A documentação Swagger estará disponível em:

```text
http://localhost:8080/swagger-ui/index.html
```

Mantenha esse terminal aberto enquanto estiver usando a aplicação.

## Executando o frontend

Abra um terceiro PowerShell a partir da raiz do projeto e entre na pasta do frontend:

```powershell
cd frontend
```

Instale as dependências:

```powershell
npm.cmd install
```

Inicie a aplicação:

```powershell
npm.cmd start
```

A interface estará disponível em:

```text
http://localhost:3000
```

Mantenha esse terminal aberto enquanto estiver usando a aplicação.

## Executando tudo com Docker

Como alternativa aos três terminais, execute na raiz do projeto:

```powershell
docker compose --env-file .env.properties -f docker-compose.yml -f docker-compose.prod.yml up --build
```

Esse comando inicia PostgreSQL, Mailpit, backend e frontend. Não mantenha as versões locais nas portas `3000` e `8080` enquanto executar a pilha completa em containers.

## Encerrando a aplicação

Para encerrar o frontend ou o backend, pressione:

```text
Ctrl + C
```

Para parar o PostgreSQL preservando os dados:

```powershell
docker compose --env-file .env.properties stop db
```

Para remover os containers preservando o volume do banco:

```powershell
docker compose --env-file .env.properties down
```

## Executando os testes

Testes do backend (a partir da raiz):

```powershell
cd backend
.\mvnw.cmd test
```

Testes do frontend (a partir da raiz):

```powershell
cd frontend
npm.cmd test -- --watchAll=false
npm.cmd run test:i18n
npm.cmd run test:wallet
npm.cmd run lint
```

## Segurança

As seguintes regras devem ser observadas durante o desenvolvimento:

- Nunca publicar `.env.properties`.
- Nunca colocar senhas ou chaves de API diretamente no código.
- Manter chaves do Gemini ou OpenAI somente no backend.
- Validar o usuário autenticado em todas as consultas.
- Impedir o acesso a carteiras pertencentes a outros usuários.
- Não permitir que a inteligência artificial execute movimentações financeiras.
- Não enviar dados pessoais desnecessários para provedores externos.
- Não exibir tokens JWT no console ou em capturas de tela.
- Revisar dependências e configurações antes de publicar uma versão.

## Fluxo de colaboração

Crie uma branch para cada alteração:

```powershell
git switch main
git pull origin main
git switch -c feat/nome-da-funcionalidade
```

Exemplos de nomes:

```text
feat/dashboard-real
feat/relatorios
feat/assistente-ia
fix/autorizacao-carteira
fix/login
docs/configuracao-local
```

Depois das alterações:

```powershell
git status
git add .
git commit -m "feat: descreva a alteração"
git push -u origin feat/nome-da-funcionalidade
```

Abra um Pull Request no GitHub para revisão antes de juntar a alteração à branch `main`.

## Padrão de commits

Exemplos:

```text
feat: adiciona resumo financeiro ao painel
fix: corrige armazenamento do token de login
docs: atualiza instruções de execução
test: adiciona testes de autorização
refactor: reorganiza serviço de transações
chore: atualiza configuração do projeto
```

## Projeto original

Este projeto é uma evolução de [yildizmy/e-wallet](https://github.com/yildizmy/e-wallet), criado por Murat Yıldız e disponibilizado sob a licença MIT.

A licença e os créditos do projeto original são preservados no arquivo [LICENSE](LICENSE).

## Licença

Este projeto é distribuído sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para conhecer os termos.
