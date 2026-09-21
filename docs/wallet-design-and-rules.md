# Carteira de demonstração: design e regras

Atualizado em 20/09/2026. Escopo confirmado: demonstração acadêmica, sem dinheiro real.

## Interface

O projeto usa cartões, tipografia do sistema, superfícies neutras, ações curtas e detalhes em diálogos. A navegação se adapta: barra lateral no computador e barra inferior no celular. Não usa marca, logotipo ou botões de pagamento da Apple.

- Início: saldo total, entradas e saídas, carteiras e últimas movimentações, todos calculados com dados do usuário.
- Carteiras: cartões, saldo, código de teste, cópia do código e ações de movimentação.
- Movimentar: adicionar, enviar e retirar saldo fictício; revisão antes da confirmação.
- Extrato: busca, filtro por carteira, paginação e detalhes da movimentação.
- Análise: fluxo mensal, observações, próximos passos e leituras com referências verificáveis.
- Login e cadastro: formulários com validação, carregamento, mensagens de erro e seleção de idioma.
- Sobre o projeto: explica a simulação e apresenta referências oficiais.

## Rotas

| Rota | Função |
| --- | --- |
| `/` | Início com os dados do usuário |
| `/wallets` | Carteiras |
| `/wallets/new` | Criar carteira com saldo zero |
| `/wallets/:walletId` | Carteira selecionada e extrato |
| `/transfers?mode=transfer` | Enviar saldo |
| `/transfers?mode=deposit` | Adicionar saldo fictício |
| `/transfers?mode=withdraw` | Retirar saldo fictício |
| `/transactions?wallet=:id` | Extrato, com filtro opcional |
| `/insights` | Relatório de fluxo de caixa e referências |
| `/about` | Escopo e referências |
| `/wallets/addFunds` | Redirecionamento para a operação atual |
| `/login`, `/signup` | Autenticação |

Os links diretos protegidos voltam à rota solicitada após o login. O servidor que hospedar o React precisa encaminhar as rotas da aplicação para `index.html`.

## Regras implementadas

- Saldos da demonstração são apresentados em BRL. A API original não modela múltiplas moedas; não há câmbio.
- Carteiras novas começam zeradas pela interface. Um código com estrutura IBAN é gerado pelo servidor para compatibilidade com o modelo existente. É apenas um identificador de teste, sem vínculo bancário ou com o Pix.
- Valores de operações são positivos, têm até duas casas decimais e até dez dígitos inteiros. Retiradas e transferências não podem exceder o saldo.
- Destino e origem de uma transferência devem ser diferentes. O servidor determina o tipo da operação.
- A autorização usa o usuário autenticado: consultas e alterações de carteiras exigem propriedade; extratos só podem ser consultados pelos participantes.
- Cadastro público sempre recebe `ROLE_USER`; não pode escolher administrador ou sobrescrever outro usuário por ID.
- Atualizações de saldo usam transações e bloqueios pessimistas de carteiras, em ordem de ID. Transferências debitam e creditam na mesma transação.
- `requestId` identifica reenvios: um pedido idêntico retorna o resultado existente; dados diferentes com a mesma chave são rejeitados. Uma restrição única no banco impede a aplicação duplicada. A interface mantém a chave ao repetir uma tentativa sem editar os dados.
- Depósito, retirada, transferência e saldo inicial têm tipos distintos. Transferências entre carteiras do próprio usuário não entram como receita/despesa no total consolidado.
- A consulta de histórico elimina linhas duplicadas e retorna lista vazia quando ainda não há movimentações.
- Respostas de transações não expõem saldo, e-mail ou perfil do outro participante. Datas saem em ISO 8601 e são formatadas no idioma do usuário.
- Alteração direta de saldo, proprietário ou código pelo endpoint de edição é rejeitada. Carteiras com saldo ou histórico não podem ser excluídas.
- O token de acesso fica no `sessionStorage` e é removido ao encerrar a sessão do navegador; tokens antigos no `localStorage` são descartados.
- O login limita cinco falhas por combinação de usuário e endereço em uma janela de quinze minutos.
- O JWT usa JJWT 0.12.6, segredo Base64 de no mínimo 256 bits e expiração de uma hora.
- Respostas internas não devolvem stack trace nem detalhes técnicos ao cliente, e o CORS aceita apenas a origem configurada.
- Relatórios usam somente dados do usuário autenticado e limitam chamadas à IA a oito por hora.

## Banco de dados e execução

As migrações Flyway `V6__wallet_operation_types.sql` e `V7__financial_value_constraints.sql` adicionam os tipos de operação, a chave de reenvio e limites financeiros para novas gravações. Elas são aplicadas ao iniciar a versão atualizada do backend; a versão 7 foi validada com PostgreSQL 14.6.

Registros antigos de saldo inicial, identificados pela descrição original, recebem o tipo correto. Outras operações antigas entre a mesma carteira não são reclassificadas por suposição: o modelo anterior não registrava se eram depósito ou retirada. Os resumos históricos podem, por isso, não reconstruir entradas e saídas anteriores à migração. As restrições de saldo não negativo são adicionadas como `NOT VALID` para não apagar nem corrigir dados antigos silenciosamente; valem para novas gravações.

Frontend: `npm start` na pasta `frontend`. A URL da API pode ser configurada com `REACT_APP_API_URL`; o padrão local é `http://localhost:8080/api/v1`. No Docker, o React é compilado e servido por Nginx sem privilégios, com fallback para as rotas da SPA e cabeçalhos de segurança.

Verificação: `npm run test:wallet`, `npm test -- --watchAll=false --runInBand` e `npm run build`. No backend, `mvn -Dtest=*ServiceTest,WalletAccessTest test` executa os testes unitários sem exigir o banco da aplicação. Estes testes não substituem um teste de concorrência e migração em PostgreSQL real.

## O que depende de serviços externos

A referência visual não habilita Apple Pay. A Apple distingue o app Carteira, que organiza cartões e passes, do Apple Pay, usado para pagamentos. Apple Pay na web exige configuração de comerciante, certificados e domínio, além da integração de processamento de pagamento.

Pix e contas de pagamento reais exigem uma instituição e uma integração compatíveis com as regras aplicáveis do Banco Central. O protótipo não é uma instituição autorizada, não oferece crédito, custódia de dinheiro real, remuneração de saldo, seguro ou garantia do FGC. Uma futura operação comercial exigiria análise regulatória própria, provedor habilitado e desenvolvimento de cadastro, prevenção a fraude, conciliação, limites, estornos e auditoria.

Referências oficiais consultadas:

- [Apple Wallet — visão geral](https://www.apple.com/br/wallet/)
- [Apple — Human Interface Guidelines: Wallet](https://developer.apple.com/design/human-interface-guidelines/wallet/)
- [Apple — configurar Apple Pay na web](https://developer.apple.com/help/account/capabilities/configure-apple-pay-on-the-web)
- [Banco Central — instituições de pagamento](https://www.bcb.gov.br/estabilidadefinanceira/instituicaopagamento)
- [Banco Central — Pix](https://www.bcb.gov.br/estabilidadefinanceira/pix)
- [Banco Central — contas de pagamento](https://www.bcb.gov.br/meubc/faqs/s/contas-de-pagamento)
