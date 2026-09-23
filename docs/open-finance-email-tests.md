# Teste de conexão com código por e-mail

Antes de importar o sandbox para um usuário, o Wall.ia pede um código de 8 dígitos enviado ao e-mail cadastrado. Essa confirmação é uma proteção da própria carteira. Ela não representa a autenticação nem a certificação do Open Finance oficial.

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

O backend aplica as migrações (até a V13) sozinho. Rodando o backend em container, o Compose já usa o host SMTP `mailpit`.

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

1. Entre em http://localhost:3000 e abra **Open Finance**.
2. Clique em **Conectar dados de teste**.
3. Abra o Mailpit em http://localhost:8025, localize a mensagem pelo destinatário e copie o código.
4. Informe o código. A primeira conexão importa 2 contas e 59 movimentações (3 meses de histórico), 1 cartão com faturas até março/2027 e 1 empréstimo. Saldo total: R$ 2.555,61.
5. Sincronize de novo: devem aparecer 0 novas movimentações.
6. Abra **Futuro**. Devem aparecer 8 perguntas (salário da Concentrix S.A., aluguel, escola, luz, internet, academia, Netflix e uma renda eventual de Marina Souza).
7. Confirme todas as sugestões. A projeção passa a mostrar saldo abaixo de zero entre 03/10 e 04/10, com mínimo de -R$ 324,87, e o botão "ver por quê" lista empréstimo, aluguel e fatura como causa e o salário do dia 5 como alívio.
8. Repita com a segunda conta: ela recebe os próprios dados, sem compartilhamento com a primeira.
9. Revogue a conexão. Reconectar exige outro código.

Números conferidos em 23/09/2026 com o roteiro de API contra o backend local. A massa é gerada a partir da data atual (`Clock`), então meses e valores do buraco mudam conforme o dia em que o teste roda. O cenário de 22/09/2026 está travado em `ContractSandboxOpenFinanceGatewayTest`.

## Verificações de proteção

- Sem confirmar o e-mail, `POST /api/v1/open-finance/sync` retorna 428.
- Código errado retorna 400 e não importa nada.
- O código expira em 10 minutos e vale uma vez só.
- Depois de 5 erros o código é bloqueado; espere o vencimento para pedir outro.
- O reenvio exige 60 segundos de intervalo e invalida o código anterior.
- Revogar, reutilizar um código ou usar o código de outra conta não libera nova conexão.
- O banco guarda o hash BCrypt, nunca o código em texto puro.
- Conexões feitas antes dessa verificação precisam confirmar o e-mail antes da próxima sincronização.

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
