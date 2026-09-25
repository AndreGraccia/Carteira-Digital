# Roteiro de teste: cadastro com código por e-mail e consentimento Open Finance

Desde a V24 (ADR 0008), o código de 8 dígitos confirma o e-mail no cadastro, uma vez por conta. A conexão com o Open Finance não pede mais código: ela segue o ciclo de consentimento do padrão (`AWAITING_AUTHORISATION`, `AUTHORISED`, `REJECTED`). Nenhuma das duas coisas representa autenticação ou certificação do Open Finance oficial; tudo roda em sandbox.

## Iniciar

Na raiz do projeto:

```powershell
docker compose --env-file .env.properties up -d db mailpit
cd backend
.\mvnw.cmd spring-boot:run
```

Em outro terminal:

```powershell
cd frontend
npm.cmd start
```

O backend aplica as migrações sozinho. Rodando o backend em container, o Compose já usa o host SMTP `mailpit`.

## Dados para cadastrar

Use duas contas para verificar o isolamento:

| Campo | Conta 1 | Conta 2 |
|---|---|---|
| Nome | Ana | Bruno |
| Sobrenome | Teste | Teste |
| Usuário | otpana2026 | otpbruno2026 |
| E-mail | ana.otp@example.test | bruno.otp@example.test |
| Senha | Teste@12345 | Teste@12345 |

Esses dados são só locais. Nenhum código é fixo.

## Roteiro

1. Entre em http://localhost:3001 e clique em **Criar conta**.
2. A tela pede o código. Abra o Mailpit em http://localhost:8025, localize a mensagem pelo destinatário, copie o código e confirme.
3. Entre com o usuário e a senha.
4. Abra **Open Finance** e clique em **Conectar**. Confira na tela de consentimento o que será lido, a finalidade e até quando vale. Clique em **Autorizar**.
5. A primeira conexão importa 2 contas com 3 meses de histórico, 1 cartão com faturas e 1 empréstimo.
6. Sincronize de novo: devem aparecer 0 novas movimentações.
7. Abra **Futuro** e responda às perguntas sobre recorrências (salário da Concentrix S.A., aluguel, escola, luz, internet, academia, Netflix e uma renda eventual de Marina Souza).
8. Depois de confirmar, a projeção mostra a janela abaixo de zero, o "ver por quê" com as causas e o alívio, e o custo estimado do cheque especial com a origem da taxa.
9. Confirme "Gastos do dia a dia" nas perguntas. O ponto mais baixo desce e o custo sobe.
10. Abra **Perguntar**. "Posso comprar um notebook de R$ 4.000 em 10x?" deve responder "Melhor não agora". "Qual a previsão do tempo?" responde que ainda não sabe.
11. Em "Como evitar o negativo" aparece combinar outra data para o aluguel, sem nenhum dia negativo.
12. Em **Metas**, crie "Reserva de emergência", R$ 200 por mês, dia 10. O limite e a projeção passam a descontar a meta.
13. Ative "Avisar por e-mail". Para ver o e-mail sem esperar as 8h, suba o backend com `future_alert_cron=0 * * * * *` e confira o Mailpit: um e-mail por janela nova, nenhum repetido.
14. Repita com a segunda conta: ela vê só os próprios dados.
15. Revogue o compartilhamento. Contas, cartões, empréstimos e lançamentos importados somem, e o status fica `REJECTED` com `CUSTOMER_MANUALLY_REVOKED`. Reconectar pede um consentimento novo.

Os valores exatos (saldo total, mínimo da janela, datas) mudaram depois de 23/09 com o ajuste de dia útil (ADR 0007) e os lançamentos agendados (ADR 0009), e ainda não foram reconferidos com o backend no ar. A massa é gerada a partir da data atual (`Clock`); o cenário de 22/09/2026 está travado em `ContractSandboxOpenFinanceGatewayTest`.

## Verificações de proteção

Código do cadastro (`EmailVerificationService`):
- Login antes de confirmar: 403 `EMAIL_NOT_VERIFIED`.
- Código errado: 400 `EMAIL_CODE_INVALID`, e nada muda.
- O código expira em 10 minutos (410 `EMAIL_CODE_EXPIRED`) e vale uma vez só.
- Depois de 5 erros: 429 `EMAIL_CODE_ATTEMPTS_EXCEEDED`. Peça outro código.
- Reenvio antes de 60 segundos: 429 `EMAIL_CODE_RESEND_WAIT`. O reenvio invalida o código anterior, e há um teto de envios por dia (`EMAIL_CODE_DAILY_LIMIT`).
- Cadastro com usuário ou e-mail já existentes responde 202, igual a um cadastro novo, para não revelar quem tem conta.
- O banco guarda o hash BCrypt, nunca o código em texto puro.

Consentimento (`OpenFinanceService`):
- Sincronizar sem consentimento autorizado: 428 `OPEN_FINANCE_CONSENT_REQUIRED`.
- Autorizar depois de 60 minutos: 410 `OPEN_FINANCE_CONSENT_EXPIRED`, e o consentimento vira `REJECTED` com `CONSENT_EXPIRED`.
- Pedir outra conexão com uma já ativa: 409 `OPEN_FINANCE_ALREADY_CONNECTED`.
- Ler o extrato de outra conta trocando o id na URL: 403.

## Receber na caixa de e-mail real

Por padrão o envio local vai só para o Mailpit. Para receber na caixa cadastrada, configure um SMTP em `.env.properties` e reinicie o backend:

```properties
mail_host=smtp.seu-provedor.com
mail_port=587
mail_username=seu-usuario-smtp
mail_password=sua-credencial-smtp
mail_auth=true
mail_starttls=true
mail_from=remetente-verificado@seu-dominio.com
```

Essas credenciais não vão para o Git. O navegador não escolhe o destinatário: o servidor lê o e-mail do cadastro.

Referências: [OWASP, códigos temporários](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html), [Spring, envio de e-mail](https://docs.spring.io/spring-framework/reference/integration/email.html).
