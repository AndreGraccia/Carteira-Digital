# Modificações e erros corrigidos

Escopo: alterações realizadas desde a base `main` (`9e83117`) até a branch atual `feat/fase1-interface`, incluindo os ajustes ainda não consolidados em commit.

## `59924f9` — organização inicial das rotas

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Reorganização das rotas públicas, privadas e protegidas em `App.js`. | Corrigiu rotas aninhadas repetidas e páginas renderizadas no nível incorreto. |
| `PrivateRoute` passou a consultar o usuário apenas uma vez e a redirecionar com `replace`. | Evitou consultas duplicadas ao armazenamento e entradas inválidas no histórico do navegador. |
| `ProtectedRoute` passou a aceitar somente listas de papéis válidas e a tratar usuário sem `roles`. | Corrigiu exceções ao executar `includes` quando os papéis não existiam. |
| Criação da página `NotFound` e da rota curinga `*`. | Corrigiu a tela vazia ao acessar endereços inexistentes. |
| Centralização de `ROLE_USER` e `ROLE_ADMIN` nas rotas protegidas. | Removeu regras de autorização duplicadas e inconsistentes entre páginas. |

## `3bfea85` — interface, regras e segurança

### Interface e experiência do usuário

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Redesenho do dashboard, carteiras, extrato, transferências, login e cadastro com cartões, superfícies neutras, hierarquia visual e tipografia de sistema. | Substituiu a aparência genérica do template e corrigiu inconsistências de espaçamento, contraste e hierarquia. |
| Navegação lateral no desktop e inferior no celular. | Corrigiu a dificuldade de navegação em telas pequenas. |
| Criação dos componentes reutilizáveis `wallet-card`, `wallet-statement`, `wallet-ui` e `transaction-detail`. | Eliminou apresentação duplicada e diferente para os mesmos dados financeiros. |
| Criação da página “Sobre o projeto”. | Corrigiu a falta de informação sobre o caráter acadêmico e os limites da demonstração. |
| Estados de carregamento, vazio e erro nas telas de carteira e extrato. | Corrigiu telas sem resposta visual durante requisições ou quando não havia dados. |
| Diálogo de revisão antes de movimentar saldo. | Reduziu confirmações acidentais e permitiu revisar carteira, destino e valor. |
| Bloqueio do botão durante o envio. | Corrigiu operações duplicadas causadas por clique duplo. |

### Traduções

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Criação do catálogo central `i18n.js` em português e inglês. | Corrigiu textos misturados, traduções espalhadas pelos componentes e mensagens sem padrão. |
| Seletor de idioma nas telas públicas e autenticadas. | Corrigiu a impossibilidade de mudar o idioma sem editar o código. |
| Tradução de títulos, botões, estados vazios, validações e mensagens de erro. | Substituiu mensagens técnicas ou parcialmente traduzidas por textos simples para o usuário. |
| Formatação de datas e valores conforme o idioma. | Corrigiu datas e moedas exibidas em formatos inconsistentes. |

### Rotas e autenticação

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Inclusão das rotas `/wallets/:walletId`, `/wallets/new`, `/transfers`, `/transactions` e `/about`. | Corrigiu páginas sem endereço próprio e links que não representavam o estado atual. |
| Redirecionamentos para rotas antigas de movimentação. | Manteve links existentes funcionando depois da reorganização do frontend. |
| Retorno à rota originalmente solicitada depois do login. | Corrigiu o envio obrigatório para o início ao abrir um link protegido. |
| Tratamento de sessão inválida no serviço HTTP. | Corrigiu a permanência do usuário em páginas protegidas após expiração ou rejeição do token. |
| Cadastro público limitado a `ROLE_USER`. | Corrigiu a possibilidade de solicitar papel administrativo pelo corpo da requisição. |

### Dados e cálculos da carteira

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Criação do `WalletContext` para carregar carteiras e transações do usuário autenticado. | Corrigiu requisições repetidas e dados diferentes entre dashboard, carteiras e extrato. |
| Utilitários monetários baseados em centavos. | Corrigiu erros de ponto flutuante em somas e comparações de valores. |
| Consolidação de saldo, entradas e saídas com dados reais da API. | Removeu números estáticos ou derivados de mocks do template. |
| Transferências entre carteiras do mesmo usuário não contam como receita ou despesa consolidada. | Corrigiu totais inflados por movimentação interna. |
| Busca, filtro por carteira, paginação e detalhe da transação. | Corrigiu a dificuldade de localizar e entender movimentações específicas. |
| Eliminação de linhas duplicadas no histórico. | Corrigiu transações exibidas mais de uma vez quando a mesma carteira aparecia como origem e destino. |

### Regras de movimentação

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Validação de valor positivo, máximo e duas casas decimais no frontend e backend. | Impediu valores negativos, zero, excessivos ou com precisão inválida. |
| Verificação de saldo em transferências e retiradas. | Impediu saldo negativo por insuficiência de fundos. |
| Proibição de origem e destino iguais em transferências. | Impediu transferências sem efeito e registros ambíguos. |
| Separação de depósito, retirada e transferência. | Corrigiu operações distintas registradas com o mesmo significado. |
| Geração e reutilização de `requestId` UUID. | Corrigiu a aplicação duplicada de uma operação após clique duplo ou reenvio da rede. |
| Rejeição do mesmo `requestId` com dados diferentes. | Impediu que uma chave de idempotência fosse reutilizada para outra operação. |

### Segurança e privacidade do backend

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Criação de `WalletAccess` e uso do usuário autenticado nas autorizações. | Corrigiu acesso indevido a carteiras por troca de ID ou IBAN na URL/corpo da requisição. |
| Consultas de transação limitadas aos participantes. | Corrigiu exposição do extrato de outros usuários. |
| Respostas de transação reduzidas aos dados necessários. | Corrigiu exposição de saldo, e-mail e perfil do outro participante. |
| Bloqueio da alteração direta de saldo, proprietário e IBAN pelo endpoint de atualização. | Impediu contornar as regras de depósito, retirada e transferência. |
| Exclusão permitida somente para carteira sem saldo e sem histórico. | Impediu apagar carteira com dinheiro simulado ou registros vinculados. |
| Bloqueio pessimista das carteiras em ordem estável. | Reduziu perda de atualização e deadlock em movimentações concorrentes. |
| Transferência executada dentro de uma única transação do banco. | Impediu débito sem crédito correspondente quando uma etapa falhava. |
| Exceções específicas de regra de carteira e respostas HTTP adequadas. | Corrigiu erros de negócio retornados como falhas genéricas do servidor. |
| Validador de IBAN tolerante a entrada nula antes da validação formal. | Corrigiu exceções inesperadas durante validação de formulários incompletos. |

### Banco de dados e testes

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Migração Flyway V6 com `request_id`, restrição única e novos tipos de operação. | Corrigiu a falta de idempotência persistente e de classificação própria para as operações. |
| Restrições de saldo e valor não negativos para novas gravações. | Impediu registros financeiros negativos inseridos fora do fluxo normal do serviço. |
| Testes de `WalletAccess`, autenticação, transações e carteira. | Cobriu autorização, validações, concorrência lógica e reenvio que não possuíam proteção automatizada. |
| Testes do formulário de movimentação. | Cobriu retirada sem saldo, depósito negativo, revisão do valor e clique duplo. |
| Verificador de chaves PT/EN e regras monetárias. | Corrigiu traduções ausentes e regressões nos cálculos da interface. |

## `ea7d3a4` — alinhamento dos tipos de operação

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| IDs 4, 5 e 6 reservados para depósito, retirada e saldo inicial. | Corrigiu conflito com os tipos já existentes: 1 Transfer, 2 Payment e 3 Shopping. |
| `addFunds` alterado para o tipo 4 e `withdrawFunds` para o tipo 5. | Corrigiu depósitos registrados como pagamento e retiradas registradas como compra. |
| Mapeamento de saldo inicial alterado para o tipo 6. | Corrigiu a classificação incorreta do histórico inicial. |
| Atualização da V6 para preservar os tipos antigos. | Impediu colisão de chave primária e mudança de significado de registros existentes. |

## `84c5948` — carteira sempre iniciada com saldo zero

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Nova carteira aceita apenas saldo inicial igual a zero. | Impediu criação de dinheiro simulado sem uma operação explícita. |
| O backend força `BigDecimal.ZERO` na criação. | Impediu que clientes modificados enviassem saldo inicial diferente do apresentado no frontend. |
| Remoção da transação fictícia de saldo inicial. | Corrigiu o extrato criado com uma movimentação que nunca ocorreu. |
| Testes para aceitar zero, rejeitar valor diferente de zero e não criar transação. | Protegeu a nova regra contra regressões. |
| Configuração Jest adicionada ao projeto React. | Corrigiu referências de teste consideradas globais indefinidas pelo ESLint. |

## Ajustes posteriores ao último commit

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Migração V7 com valor maior que zero, limite de `9.999.999.999,99` e duas casas decimais. | Impediu que gravações diretas no PostgreSQL ignorassem as regras monetárias do backend. |
| Comentários nas colunas de IBAN, saldo e `request_id`. | Corrigiu a falta de documentação no próprio esquema sobre dados simulados e idempotência. |
| Carregamento de `.env.properties` pela raiz ou pela pasta `backend`. | Corrigiu a autenticação no PostgreSQL usando literalmente `${db_username}` ao executar o Maven pela raiz. |
| Desativação de SQL e rastreamentos detalhados na configuração padrão. | Evitou logs excessivos e exposição desnecessária de detalhes internos. |
| Remoção das propriedades JPA redundantes `open-in-view` e `generate-ddl`. | Eliminou configuração contraditória com `ddl-auto: none` e com o Flyway responsável pelo esquema. |
| Metadados de `index.html` e `manifest.json` atualizados para e-wallet em português. | Removeu nome, descrição e cores restantes do template Minimal UI Kit. |
| Remoção da fonte externa do Google. | Evitou dependência de rede desnecessária para renderizar a interface. |
| Ajuste do mock de `crypto.randomUUID` para `globalThis` nos testes. | Corrigiu falha do Jest ao tentar sobrescrever `window.crypto.randomUUID`. |
| Ambiente Jest declarado no ESLint. | Corrigiu falsos erros para `jest`, `beforeEach`, `afterEach` e funções de teste. |
| Encaminhamento de `ref` e demais propriedades pelo componente `Logo`. | Corrigiu propriedades recebidas e ignoradas, além dos avisos do ESLint. |
| Remoção de parâmetros de resposta não usados em login e cadastro. | Eliminou avisos de variáveis declaradas e não utilizadas. |
| Comando de verificação do PostgreSQL passou a usar as variáveis do container. | Corrigiu falha do README quando banco e usuário têm nomes diferentes do exemplo. |
| Documentação das regras, rotas, banco e limites da demonstração. | Corrigiu a ausência de uma referência única para comportamento e escopo do sistema. |

## Validações relacionadas às correções

| Validação | Problema que ela detecta |
| --- | --- |
| 48 testes do backend aprovados. | Regressões em autenticação, serviços, acesso a carteiras e movimentações. |
| 3 testes do fluxo de movimentação aprovados. | Envio inválido, retirada acima do saldo e duplicação por clique duplo. |
| 207 chaves de tradução PT/EN verificadas. | Chaves ausentes ou diferentes entre os idiomas. |
| ESLint executado sem erros ou avisos de código. | Imports, variáveis e globais de teste inválidos. |
| Build de produção do React concluído. | Erros de compilação, importação, manifesto e empacotamento. |
| Flyway V1 a V7 validado no PostgreSQL 14.6. | Migração incompatível, versão ausente ou esquema fora de ordem. |
| Restrições financeiras e índice único de `request_id` consultados no banco. | Regras presentes apenas no Java, sem proteção no PostgreSQL. |
| OpenAPI respondeu com as rotas de autenticação, carteiras e transações. | Rotas do backend ausentes ou aplicação indisponível. |

## Branch `feat/wallia`: ambiente local, documentação e revisão de segurança (23/09/2026)

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Portas configuráveis (`db_port`, `backend_port`, `frontend_port`) e `frontend/.env.example`. | O cadastro falhava porque a interface chamava a porta 8080, ocupada por outro projeto. |
| `docs/guia-do-desenvolvedor.md` com peças, configuração, portas, problemas comuns, caminho de uma requisição e receitas. | O README não bastava para rodar o projeto numa máquina com outras aplicações. |
| README com Mailpit no passo do banco, portas e link para o guia. | O código do Open Finance não chegava porque o README subia só o banco. |
| O limitador de login reserva a tentativa antes do BCrypt e soma um teto por usuário (20) além do teto por usuário e IP (5). Entradas vencidas são limpas. | Uma rajada de requisições paralelas passava toda pela verificação antes do primeiro erro ser contado; trocar de IP zerava o limite; o mapa só crescia. |
| O IBAN das carteiras importadas do Open Finance passou a ser aleatório. | O IBAN era derivado de `userId` e do id da conta, então outro usuário podia criá-lo antes e fazer a importação da vítima falhar por unicidade. |
| `frontend/.dockerignore` ignora `.env.local`, `build` e `coverage`. | Configuração local e artefatos entravam no contexto da imagem. |
| `Contexto*.txt` no `.gitignore`. | Exportações de sessão podiam ser commitadas por engano. |
| Login encontra o usuário ignorando maiúsculas e espaços, ou pelo e-mail; campo sem autocapitalização no celular. | Conta criada como `gui_oliveira` não entrava digitando `Gui_oliveira`: o cadastro comparava ignorando maiúsculas e o login, não. |
| Cadastro limitado a 10 por IP por hora. | Um script podia criar contas em massa e multiplicar os limites por usuário (Gemini, e-mails). |
| Código do Open Finance limitado a 10 envios por conta em 24 h (V12). | Uma conta cadastrada com o e-mail de outra pessoa podia mandar a ela um código por minuto. |
| Teto global de 200 relatórios Gemini por hora, com volta para a análise local. | O custo da chave de API não tinha limite total. |
| Remoção do `frontend/yarn.lock`. | Dois lockfiles divergentes; o projeto e o Dockerfile usam npm. |
| V13: `wallet_local_nonnegative_balance`, saldo não negativo só para carteiras `LOCAL`. | A V8 removeu `wallet_nonnegative_balance` para aceitar contas do Open Finance negativas, e desde então o banco aceitava saldo negativo também em carteira local (`UPDATE wallet SET balance = -1` passava). |

| Validação | Resultado |
| --- | --- |
| `mvnw test` | 107 testes, 0 falhas (inclui `AuthenticationRateLimiterTest`, com rajada de 200 requisições paralelas, e `UserDetailsServiceImplTest`). |
| Frontend: Jest, lint, `test:i18n`, `test:wallet`, build | Todos passando; 469 chaves PT/EN. |
| Roteiro de API contra o backend local | Cadastro 201, CORS, 428 sem e-mail, código pelo Mailpit, código errado 400, sincronização, 8 perguntas, Futuro, 403 entre contas, 429 na 6ª senha errada. |

## Motor Futuro, segunda e terceira rodadas (ADR 0002 e 0003)

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Gastos do dia a dia (conta e cartão) como perguntas com evidência. | A projeção ignorava mercado, transporte e compras à vista no cartão: o ponto mais baixo do sandbox era R$ 313 otimista. |
| Limite diário até a próxima renda, com quanto falta quando só as contas já não cabem. | A tela não respondia "quanto posso gastar?". |
| Custo máximo do cheque especial em cada janela negativa. | O alerta não tinha preço. |
| Plano de saída calculado (combinar outra data, gastar menos por dia, quanto falta). | O alerta não dizia o que fazer. |
| Pergunte ao Wall.ia, respondido pelo motor, com limite de 30 perguntas a cada 10 minutos e contagem por intenção sem texto. | Tirar uma dúvida exigia navegar por três telas; não havia medição nem proteção contra excesso de chamadas. |
| Metas mensais na projeção (V16). | Guardar dinheiro só existia no simulador. |
| Alertas por e-mail opcionais, um por janela nova (V17). | Era preciso abrir o app para descobrir o buraco. |

| Validação | Resultado |
| --- | --- |
| `mvnw test` | 169 testes, 0 falhas. |
| Frontend | 13 testes, lint, 571 chaves PT/EN e build de produção. |
| Roteiro de API ao vivo | Cartão, plano de saída, metas (incluindo a meta de outra conta recusada), alertas com e-mail real no Mailpit sem reenvio, 403 para não admin, 429 na 31ª pergunta. |

## Atualização do Spring Boot (3.0.4 para 3.5.16)

| Modificação | Erro ou limitação corrigida |
| --- | --- |
| Spring Boot 3.5.16, springdoc 2.8.17, Lombok 1.18.38, `flyway-database-postgresql`, validação gerenciada pelo Boot. | A 3.0.4 estava sem suporte, com Tomcat 10.1.5 e Spring Security 6.0.2 com CVEs conhecidos. |
| `BigDecimal.TWO` trocado por `BigDecimal.valueOf(2)`. | API do Java 19 num projeto Java 17: compilava com o JDK 21 local e falharia no JRE 17 do container de produção. |
| Swagger e `/v3/api-docs` desligados por padrão no Compose de produção (`springdoc_enabled`). | A documentação da API ficava exposta em produção. |

Validação: 169 testes; aplicação subindo na versão nova; roteiros de API das três rodadas com os mesmos números de antes; login sem diferenciar maiúsculas, 429 na 6ª senha errada e cabeçalhos de segurança conferidos.

