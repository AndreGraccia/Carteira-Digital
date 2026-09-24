# ADR 0008: e-mail verificado no cadastro e consentimento no ciclo do Open Finance Brasil

Status: aceito e implementado no backend. 24/09/2026.

## Contexto

A verificação de e-mail tinha nascido dentro da conexão Open Finance: um código de 8 dígitos liberava a importação e "valia" 90 dias. A auditoria de 24/09 (`docs/auditoria-2026-09-24/backend.md`, O2, O3, O4, O7 e O8) mostrou o custo disso:

1. O código fazia papel de consentimento, mas consentimento no Open Finance é outra coisa: um recurso com permissões, prazo e ciclo de status, autorizado na instituição transmissora.
2. A conexão caía em 90 dias qualquer que fosse o consentimento, e pedir um código novo derrubava uma conexão ativa.
3. Quem não usava Open Finance não recebia alerta por e-mail, porque o e-mail "verificado" era o do Open Finance.
4. Cada sync gravava `expiresAt = agora + 90 dias`: com o sync diário, o consentimento nunca expirava.
5. Revogar só mudava o status. Carteiras, cartões, empréstimos e transações importados continuavam no saldo e na projeção.
6. A chamada ao banco rodava dentro da transação, com a linha do usuário travada.
7. O cadastro dizia se um e-mail já tinha conta.

## Decisão

### E-mail: da conta, no cadastro

- `user.email_verified_at` (V24). O login só emite token para conta verificada; a checagem vem **depois** da senha, então só quem sabe a senha descobre que a conta existe e não está verificada (`EMAIL_NOT_VERIFIED`, 403).
- `POST /api/v1/auth/signup` responde 202 com o e-mail mascarado. Se o e-mail já tem conta, a resposta tem o mesmo formato e o dono recebe um aviso por e-mail com o usuário dele. Usuário já em uso continua sendo `USERNAME_TAKEN` (409): nome de usuário é público por natureza.
- `POST /api/v1/auth/email/verify` (login + código) e `POST /api/v1/auth/email/resend` (login). Login desconhecido no verify é "código inválido"; o resend responde igual para qualquer login.
- O código continua o mesmo mecanismo (hash BCrypt, 10 minutos, 5 tentativas, 10 envios por dia), com a tabela renomeada para `email_verification_challenge` e a configuração em `app.email-verification.*` (as variáveis `open_finance_code_*` antigas ainda valem).
- Contas que já existiam entram como verificadas: quem verificou pelo fluxo antigo mantém a data; as demais recebem a data da migração. Testado sobre a cópia do banco de desenvolvimento (27 usuários, 12 com verificação antiga, nenhum ficou de fora).
- Alertas por e-mail dependem de `email_verified_at`, não mais do Open Finance.

### Consentimento: o ciclo da API Consents v3

| Ação | Status | Motivo (`rejection.reason.code`) |
| --- | --- | --- |
| `POST /open-finance/connection` (prazo de 3, 6 ou 12 meses) | `AWAITING_AUTHORISATION` | |
| `POST /open-finance/connection/authorise` em até 60 min | `AUTHORISED` + primeira importação | |
| Autorização depois de 60 min | `REJECTED` por ASPSP | `CONSENT_EXPIRED` |
| `POST /open-finance/connection/reject` | `REJECTED` por USER | `CUSTOMER_MANUALLY_REJECTED` |
| `DELETE /open-finance/connection` | `REJECTED` por USER, dados importados apagados | `CUSTOMER_MANUALLY_REVOKED` |
| Chegou o `expirationDateTime` | `REJECTED` por ASPSP | `CONSENT_MAX_DATE_REACHED` |

- Não existe `REVOKED` no padrão; a V25 converte as linhas antigas.
- As permissões pedidas são as que a importação lê: contas, cartões (inclusive faturas e limites), empréstimos (inclusive cronograma e pagamentos) e `RESOURCES_READ`, obrigatória em todo agrupamento. Ficam gravadas na linha do consentimento (finalidade junto com o dado, LGPD art. 8º).
- A tela de autorização da instituição é simulada pelo sandbox: no Open Finance real, ela é um redirecionamento FAPI para o banco. O resto do ciclo é o do padrão.
- O prazo é escolhido na criação e nenhum sync o altera. A importação não mexe mais em status nem em datas.
- A leitura do banco roda fora de qualquer transação e fora do lock do usuário; a importação roda numa transação curta, que confere de novo se o consentimento ainda vale.
- Revogar apaga transações, carteiras, cartões e empréstimos importados (LGPD art. 18, VI). As respostas do usuário às perguntas (recorrências, metas, envelopes) ficam: são dele, não da instituição.

### Erros

Os códigos passam a ser `EMAIL_*` (`EMAIL_CODE_INVALID`, `EMAIL_CODE_EXPIRED`, `EMAIL_CODE_RESEND_WAIT`, `EMAIL_CODE_DAILY_LIMIT`, `EMAIL_CODE_ATTEMPTS_EXCEEDED`, `EMAIL_NOT_VERIFIED`, `EMAIL_ALREADY_VERIFIED`, `EMAIL_UNAVAILABLE`) e `OPEN_FINANCE_*` (`OPEN_FINANCE_CONSENT_REQUIRED`, `OPEN_FINANCE_CONSENT_NOT_FOUND`, `OPEN_FINANCE_CONSENT_EXPIRED`, `OPEN_FINANCE_ALREADY_CONNECTED`, `OPEN_FINANCE_INVALID_DURATION`). `OpenFinanceVerificationException` virou `CodedException` (código + status HTTP + Retry-After), reaproveitável pelas outras regras.

## Consequências

- O frontend precisa da tela de código depois do cadastro e da tela de autorização do sandbox no lugar do modal de código do Open Finance.
- O modo `participant-sandbox` continua preso a um usuário (ADR desta auditoria, item O1): o token configurado é de uma pessoa.
- Revogar é destrutivo para os dados importados. Reconectar importa tudo de novo.

## Descartado

- **Manter o código no Open Finance e só acrescentar o do cadastro.** Duas verificações do mesmo e-mail, e o código de 10 minutos continuaria fingindo ser consentimento.
- **Token no verify (login sem senha).** Evitaria um passo, mas transformaria o código de 8 dígitos em senha alternativa. O frontend reaproveita a senha digitada no cadastro para entrar logo depois.
- **Guardar os dados importados depois da revogação.** Deixaria o saldo e a projeção usando dados que a pessoa pediu para parar de compartilhar.
