# Teste de conex?o com c?digo por e-mail

Esta etapa adiciona confirma??o do e-mail cadastrado antes de importar o sandbox local para cada usu?rio. A confirma??o por e-mail ? pr?pria da e-wallet; n?o representa autentica??o/certifica??o do Open Finance oficial.

## Iniciar

Na raiz do projeto:

```powershell
docker compose --env-file .env.properties up -d db mailpit
mvn -f backend/pom.xml spring-boot:run
```

Em outro terminal:

```powershell
cd frontend
npm.cmd start
```

Se o Maven tentar usar C:\.m2, execute:
```powershell
mvn "-Dmaven.repo.local=C:/Users/HP/.m2/repository" -f backend/pom.xml spring-boot:run
```

O backend aplica as migra??es V9 e V10 automaticamente. Para rodar backend em container, o Compose j? usa o host SMTP mailpit.

## Dados para cadastrar

Use duas contas de teste para verificar o isolamento:

| Campo | Conta 1 | Conta 2 |
|---|---|---|
| Nome | Ana | Bruno |
| Sobrenome | Teste | Teste |
| Usu?rio | otpana2026 | otpbruno2026 |
| E-mail | ana.otp@example.test | bruno.otp@example.test |
| Senha | Teste@12345 | Teste@12345 |

Esses dados s?o exclusivamente locais. Cadastre as contas pela tela se ainda n?o existirem. Nenhum c?digo ? fixo.

## Roteiro

1. Entre em http://localhost:3000 e abra **Open Finance**.
2. Clique em **Conectar dados de teste**.
3. Abra http://localhost:8025 (Mailpit), localize a mensagem pelo destinat?rio e copie o c?digo de 8 d?gitos.
4. Informe o c?digo na carteira. A primeira conex?o deve importar **2 contas e 15 movimenta??es**, com saldos de **R$ 5.842,70** e **R$ 2.650,00**.
5. Clique em sincronizar novamente: devem aparecer **0 novas movimenta??es**.
6. Repita com a segunda conta: ela recebe suas pr?prias 2 contas e 15 movimenta??es, sem colis?es ou compartilhamento com a primeira.
7. Revogue a conex?o. Para reconectar ser? necess?rio outro c?digo.

Os extratos atuais incluem sal?rio, mercado, aluguel, cafeteria, streaming, transporte, restaurante, curso e reserva. A amplia??o de destinat?rios e cen?rios fica para a pr?xima etapa.

## Verifica??es de prote??o

- Sem confirmar o e-mail, POST /api/v1/open-finance/sync retorna 428.
- C?digo errado retorna 400 e n?o importa dados.
- O c?digo expira em 10 minutos e pode ser usado uma ?nica vez.
- Ap?s 5 erros, o c?digo ? bloqueado; aguarde at? seu vencimento para solicitar outro.
- Reenvio exige intervalo de 60 segundos e invalida o c?digo anterior.
- Revogar, reutilizar um c?digo ou usar o c?digo de outra conta n?o libera uma nova conex?o.
- O banco armazena hash BCrypt, nunca o c?digo em texto puro.
- Conex?es anteriores ? verifica??o precisam confirmar o e-mail antes da pr?xima sincroniza??o.

## Receber na caixa de e-mail real

O padr?o local entrega somente no Mailpit. Para receber na caixa cadastrada, configure um servi?o SMTP em .env.properties e reinicie o backend:

```properties
mail_host=smtp.seu-provedor.com
mail_port=587
mail_username=seu-usuario-smtp
mail_password=sua-credencial-smtp
mail_auth=true
mail_starttls=true
mail_from=remetente-verificado@seu-dominio.com
```

Use as configura??es do provedor; estas credenciais n?o devem ser commitadas. Cadastre a conta com o endere?o que voc? controla. O navegador n?o escolhe o destinat?rio: o servidor l? o e-mail cadastrado.

Refer?ncias: [OWASP: c?digos tempor?rios](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html), [Spring: envio de e-mail](https://docs.spring.io/spring-framework/reference/integration/email.html).
