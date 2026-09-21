# e-wallet

Plataforma de gestão financeira em ambiente simulado, desenvolvida com React, Spring Boot e PostgreSQL.

O projeto oferece uma base organizada para carteiras, movimentações, fluxo de caixa e relatórios explicados em linguagem natural, com uma integração opcional à API Gemini.

> Este projeto está em desenvolvimento e não deve ser utilizado para movimentar dinheiro real.

## Interface e regras da demonstração

A experiência de carteiras, movimentações, rotas e traduções foi revisada. Consulte [design, regras implementadas e limites do protótipo](docs/wallet-design-and-rules.md), incluindo a migração V6 e as referências oficiais da Apple e do Banco Central.

## Funcionalidades atuais

- Cadastro e autenticação de usuários com JWT.
- Criação e consulta de carteiras.
- Adição e retirada de saldo.
- Transferências entre carteiras.
- Consulta do histórico de movimentações.
- Validação de IBAN.
- Documentação interativa da API com Swagger.
- Persistência de dados em PostgreSQL.
- Relatório de entradas, saídas, saldo do período e transferências internas.
- Análise financeira local ou com Gemini, sem enviar o extrato bruto ao provedor.
- Leituras com referências fixas e verificáveis.
- Limite de tentativas de login e de relatórios com IA.

## Funcionalidades planejadas

O desenvolvimento será realizado por fases.

### Fase 1 — Experiência e interface

- Interface em português.
- Navegação simplificada.
- Painel com dados reais do usuário.
- Remoção dos indicadores fictícios.
- Tratamento de carregamento, ausência de dados e erros.
- Páginas de erro e rotas inexistentes.
- Melhor funcionamento em celulares e computadores.

### Fase 2 — Segurança e consistência financeira

- Autorização por proprietário da carteira.
- Proteção contra acesso a carteiras de outros usuários.
- Identificação da moeda de cada carteira.
- Tipos explícitos de operação: saldo inicial, depósito, retirada, transferência e estorno.
- Proteção contra operações duplicadas.
- Controle de movimentações simultâneas.
- Histórico financeiro auditável.

### Fase 3 — Gestão e relatórios

- Filtros por período, carteira, categoria e tipo de operação.
- Resumo de entradas e saídas.
- Análise de gastos por categoria.
- Comparação entre períodos.
- Metas e orçamento.
- Exportação de relatórios em CSV.
- Painel com gráficos e indicadores.

### Fase 4 — Relatórios com inteligência artificial

- Relatórios explicados em linguagem natural.
- Integração configurável com Gemini ou OpenAI.
- Parâmetros por período, carteira, categoria e moeda.
- Envio somente dos dados necessários para o provedor de IA.
- Respostas estruturadas e verificáveis.
- Controle de limites, custos e erros da API.

### Fase 5 — Assistente financeiro

- Chat sobre os dados da carteira.
- Perguntas sobre gastos, entradas e variações.
- Comparação entre períodos.
- Identificação das movimentações relacionadas a cada resposta.
- Consultas limitadas aos dados do usuário autenticado.
- Assistente somente para leitura, sem permissão para movimentar valores.

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
- ApexCharts
- Node.js
- npm

### Infraestrutura

- Docker
- Docker Compose
- PostgreSQL

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
│   │       └── validator/
│   └── src/main/resources/
│       ├── db/migration/
│       └── application.yml
├── frontend/
│   ├── public/
│   └── src/
│       ├── components/
│       ├── layouts/
│       ├── pages/
│       ├── services/
│       └── theme/
├── docker-compose.yml
├── docker-compose.prod.yml
├── .env.example.properties
└── README.md
```

## Pré-requisitos

Antes de iniciar, instale:

- Git
- Java 17 ou superior
- Maven
- Node.js LTS com npm
- Docker Desktop

Confira as instalações:

```powershell
git --version
java -version
mvn --version
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
db_name=ewallet
db_username=ewallet
db_password=SUBSTITUA_PELA_SENHA_LOCAL
jwt_secret=SUBSTITUA_POR_UM_SEGREDO_ALEATORIO
frontend_origin=http://localhost:3000
gemini_api_key=
gemini_model=gemini-3.8-flash
gemini_search_grounding=true
```

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

## Executando o PostgreSQL

Abra o Docker Desktop e aguarde o mecanismo iniciar.

Na raiz do projeto, abra o primeiro PowerShell e execute:

```powershell
docker compose --env-file .env.properties up -d db
```

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
mvn spring-boot:run
```

Se você já iniciou `EWalletApplication` pelo VS Code, pule esse comando. Apenas uma instância pode usar a porta `8080` ao mesmo tempo.

Aguarde a mensagem indicando que a aplicação foi iniciada.

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

Esse comando inicia PostgreSQL, backend e frontend. Não mantenha as versões locais nas portas `3000` e `8080` enquanto executar a pilha completa em containers.

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

Testes do backend:

```powershell
mvn -f backend/pom.xml test
```

Testes do frontend:

```powershell
cd frontend
npm.cmd test
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

### Conex?o Open Finance com c?digo por e-mail

Consulte [dados e roteiro de teste](docs/open-finance-email-tests.md) para iniciar o Mailpit, cadastrar duas contas e testar confirma??o, reenvio e isolamento de dados.
