# O que só você pode fazer

Escrito em 25/09/2026. Cada passo diz onde fazer, o comando exato, o que deve aparecer e o que me mandar de volta. Os comandos são para o **PowerShell** do Windows, a não ser quando o passo diz outra coisa.

Ordem sugerida: os blocos 1 a 4 destravam o meu trabalho e levam menos de uma hora. Os blocos 5 a 8 são decisões e contratos para a produção. O bloco 9 é arrumação.

Legenda do tempo: ⏱ estimativa para fazer o passo.

---

## 1. Guardar a chave de criptografia (faça primeiro) ⏱ 5 min

**Por quê.** Hoje eu gerei uma `data_encryption_key` no seu `.env.properties`. As descrições dos lançamentos estão cifradas com ela. Se o arquivo se perder, essas descrições não voltam nunca mais, nem pelo backup.

1. Abra `C:\Users\Nitro\Downloads\Wall_ia\Carteira-Digital\.env.properties` no VS Code.
2. Procure a linha que começa com `data_encryption_key=`.
3. Copie o valor (tudo depois do `=`).
4. Salve no seu gerenciador de senhas (Bitwarden, 1Password ou o do navegador) com o nome `Wall.ia dev: data_encryption_key`.
5. Faça o mesmo com `jwt_secret` e `db_password`.
6. **Não** cole esses valores em chat, e-mail, print ou commit. O `.env.properties` está no `.gitignore`: confira com o comando abaixo, que deve responder `.gitignore:90:.env.properties`.

```powershell
cd C:\Users\Nitro\Downloads\Wall_ia\Carteira-Digital
git check-ignore -v .env.properties
```

**Me mande:** "chave guardada".

---

## 1.1 Instalar o JDK 25 ⏱ 10 min

**Por quê.** Desde a Sprint 15 o backend é Java 25 (ADR 0017). O seu JDK 21 não compila mais o projeto: `mvnw` vai dizer `release version 25 not supported`.

1. Instale:

```powershell
winget install --id EclipseAdoptium.Temurin.25.JDK -e
```

2. Feche e abra o terminal. Confira. Deve aparecer `openjdk version "25...`:

```powershell
java -version
```

3. Se ainda aparecer 21: em *Configurações do Windows → Sistema → Sobre → Configurações avançadas do sistema → Variáveis de Ambiente*, ajuste `JAVA_HOME` para `C:\Program Files\Eclipse Adoptium\jdk-25...` e ponha `%JAVA_HOME%\bin` no topo do `Path`.
4. No IntelliJ ou no VS Code, escolha o JDK 25 como SDK do projeto `backend`.
5. Teste:

```powershell
cd C:\Users\Nitro\Downloads\Wall_ia\Carteira-Digital\backend
.\mvnw.cmd -q compile
```

Sem erro, está pronto.

**Cuidado com os outros projetos.** Não desinstale o JDK 21. Projetos com Lombok anterior à 1.18.40 não compilam no JDK 25. Se uma das suas APIs Java quebrar depois disso, suba o Lombok dela ou use o JDK 21 só nela: no IntelliJ, *Project Structure → SDK*; no terminal, `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21..."` antes do `mvnw`.

**Me mande:** a primeira linha do `java -version`.

---

## 2. Instalar e logar o GitHub CLI ⏱ 10 min

**Por quê.** O repositório é privado e eu não consigo ler o resultado do CI nem do E2E sem o `gh`. Desde a Sprint 9 o CI nunca foi conferido por mim diretamente, e é isso que falta para abrir o PR.

1. Instale:

```powershell
winget install --id GitHub.cli -e
```

2. **Feche e abra de novo** o terminal (o PowerShell e também o do Claude Code), senão o comando `gh` não é encontrado.
3. Confira a instalação. Deve aparecer `gh version 2.x`:

```powershell
gh --version
```

4. Faça o login:

```powershell
gh auth login
```

Responda assim, uma pergunta por vez:

- *Where do you use GitHub?* → `GitHub.com`
- *What is your preferred protocol?* → `HTTPS`
- *Authenticate Git with your GitHub credentials?* → `Yes`
- *How would you like to authenticate?* → `Login with a web browser`
- Copie o código de 8 caracteres que aparece, aperte Enter, cole o código na página que abrir e autorize.

5. Confira. Deve aparecer `Logged in to github.com account Guilhr-07`:

```powershell
gh auth status
```

6. Confira se você enxerga o repositório do André. Deve listar execuções do `CI` e do `E2E`:

```powershell
gh run list --repo AndreGraccia/Carteira-Digital --branch feat/wallia --limit 6
```

Se der `HTTP 404` ou `Could not resolve to a Repository`: sua conta não tem acesso de leitura ao repositório pelo `gh`. Peça ao André para confirmar que você é colaborador em *Settings → Collaborators*.

**Me mande:** a saída do passo 6 (é só uma lista de nomes, status e horários; não tem segredo).

---

## 3. Auditoria das dependências do frontend (em casa, fora da rede da empresa) ⏱ 15 min

**Por quê.** O CI apontou 68 alertas no `npm audit` (3 críticos, 33 altos). Na rede da empresa o registry do npm é bloqueado e eu não consigo rodar isso. Também falta remover o pacote `web-vitals`, que ficou sem uso.

1. Confirme que o npm alcança o registry. Deve imprimir um número de versão:

```powershell
npm view web-vitals version
```

2. Gere os dois relatórios:

```powershell
cd C:\Users\Nitro\Downloads\Wall_ia\Carteira-Digital\frontend
npm audit --omit=dev > $env:TEMP\audit-producao.txt
npm audit --json > $env:TEMP\audit-completo.json
```

O primeiro mostra só o que vai para o navegador do usuário (o que importa). O segundo tem tudo, inclusive ferramentas de build.

3. **Não** rode `npm audit fix --force`. Ele pode trocar o `react-scripts` por uma versão incompatível e quebrar o build. Eu faço a correção pacote por pacote.

**Me mande:** diga "rodei o audit". Os arquivos ficam em `%TEMP%` e eu leio de lá, sem você colar nada.

Se o passo 1 falhar mesmo em casa: rode `npm config get registry`. Deve ser `https://registry.npmjs.org/`. Se for outro endereço, me mande qual é.

---

## 4. Homologação completa em casa ⏱ 20 min

**Por quê.** Na rede da empresa eu montei a imagem do frontend fora do Dockerfile, porque o `npm ci` dentro do Docker é bloqueado. Em casa dá para subir exatamente como vai para produção e rodar o E2E.

1. Abra o Docker Desktop e espere o ícone ficar verde.
2. Suba tudo com os Dockerfiles oficiais. O `--wait` só termina quando banco e backend estiverem saudáveis:

```powershell
cd C:\Users\Nitro\Downloads\Wall_ia\Carteira-Digital
docker compose --env-file .env.properties -f docker-compose.yml -f docker-compose.prod.yml up -d --build --wait
```

3. Confira. Banco e backend precisam dizer `healthy`:

```powershell
docker compose --env-file .env.properties -f docker-compose.yml -f docker-compose.prod.yml ps
```

4. Instale o Playwright numa pasta temporária (não mexe no projeto) e rode o E2E:

```powershell
npm install --no-save --prefix $env:TEMP\pw playwright@1.61.1
npx --prefix $env:TEMP\pw playwright install chromium
$env:PLAYWRIGHT_MODULE = "$env:TEMP\pw\node_modules\playwright"
cd frontend
node e2e/journey.cjs
```

A última linha precisa ser `E2E ok`.

5. Agora use você mesmo, no celular, como um usuário faria. O celular precisa estar no mesmo Wi-Fi, e o frontend só escuta no próprio PC (`127.0.0.1`), então **faça pelo navegador do PC com a tela estreita**: abra http://localhost:3001, aperte F12, depois Ctrl+Shift+M e escolha "iPhone 12 Pro".
   - Crie uma conta. O código chega em http://localhost:8025.
   - Conecte o Open Finance e autorize.
   - Abra **Futuro** e role até o fim. Anote o que você sentiu: é longo demais? O que você procuraria primeiro?
   - Abra **Privacidade** e baixe seus dados. Abra o arquivo e veja se faz sentido para uma pessoa comum.
   - Exclua a conta e confira que não consegue mais entrar.

**Me mande:** "E2E ok" (ou a mensagem de erro inteira) e as suas impressões do passo 5, em texto livre.

---

## 5. Decisões que eu não posso tomar por você ⏱ 10 min

Responda com o número e a letra.

**5.1 Commit que não compila sozinho (`3a8e9b5`).** O código final está certo; só aquele commit isolado não compila.

- a) Deixar como está. (Recomendo: consertar exige push forçado numa branch que é do repositório do André.)
- b) Reescrever o histórico com push forçado. Só com o André avisado.

**5.2 Branches locais de backup** (`backup/feat-wallia-antes-reescrita`, `backup/feat-wallia-com-coautor`, `feat/wallia-sem-coautor`). Existem só no seu PC e guardam a versão de antes da reescrita de 25/09.

- a) Manter.
- b) Apagar. Antes eu confiro que nada nelas falta na `feat/wallia`.

**5.3 Pull request.** Depois do bloco 2 e com CI e E2E verdes:

- a) Eu abro o PR `feat/wallia` → `main` com a descrição completa, e você e o André revisam.
- b) Você prefere abrir.

**5.4 Containers parados antigos.** Estão parados e ocupam espaço: `wallia-e2e-mail`, `wallia-e2e-db`, `e-wallet-db`, `e-wallet-frontend`, `e-wallet-backend`. Os de outros projetos (`sonarqube-a3`, `gxms_*`, `festive_bhabha`) eu não toco.

- a) Pode apagar os cinco da Wall.ia.
- b) Manter.

---

## 6. Antes de qualquer pessoa real usar (produção) ⏱ depende de terceiros

Nada aqui é código. Cada item bloqueia a produção.

1. **Documentos legais.**
   - Política de Privacidade e Termos de Uso escritos (a tela Privacidade explica, mas não substitui o documento).
   - Nome e contato do **encarregado de dados**, o DPO (LGPD, art. 41), publicados no site.
   - Quem revisar deve ler o [ADR 0015](adr/0015-retencao-e-portabilidade.md) (o que é guardado e por quanto tempo) e o [ADR 0012](adr/0012-exclusao-de-conta-lgpd.md) (o que a exclusão faz).
2. **Registro de acesso (Marco Civil, art. 15).** Perguntar ao jurídico se a Wall.ia precisa guardar IP, data e hora de acesso por 6 meses. Hoje não guarda. Se precisar, eu implemento.
3. **Incidente de segurança.** Confirmar com o jurídico o prazo e o formulário da ANPD (Resolução CD/ANPD nº 15/2024) descritos em [`operacao.md`](operacao.md), seção 4, e definir quem é avisado primeiro.
4. **Normas citadas só pelo ChatGPT.** Conferir no site do Banco Central: IN BCB 759, 760 e 720, Resolução 526 e 541, IN 746/2026. Nenhuma tela cita esses números até alguém confirmar.
5. **Gemini.** A chave está no `.env.properties`. Para produção:
   - confirmar no Google AI Studio que a conta é paga (no plano gratuito, o Google pode usar os dados enviados);
   - registrar que existe transferência internacional de dados. Vão só totais mensais, mas isso precisa estar na Política de Privacidade.

---

## 7. Infraestrutura de produção ⏱ 1 a 2 dias, com contratação

O passo a passo técnico está em [`operacao.md`](operacao.md). Aqui fica o que precisa ser comprado ou contratado.

1. **Servidor** Linux com Docker. Para começar, 2 vCPU e 4 GB de RAM.
2. **Domínio** (por exemplo `wallia.com.br`) apontando para o servidor.
3. **Proxy com TLS** (Caddy, com o exemplo da seção 1 do `operacao.md`).
4. **E-mail transacional** (Amazon SES, Resend, Postmark ou similar). No DNS do domínio:
   - SPF, DKIM e DMARC;
   - `container_mail_host`, `mail_port`, `mail_username`, `mail_password` e `mail_from` no `.env.properties` de produção.
5. **Cópia de backup fora do servidor** (um bucket S3, Backblaze B2 ou outro servidor), com acesso restrito.
6. **Cofre de segredos** para `data_encryption_key`, `jwt_secret` e `db_password` de produção. São **outros valores**, nunca os de desenvolvimento.
7. **Cron no servidor:**

```
30 2 * * * cd /srv/wallia && scripts/backup.sh /srv/backups >> /var/log/wallia-backup.log 2>&1
```

8. **Uma vez por mês:** `scripts/restore-check.sh` com o backup mais recente.

---

## 8. Open Finance real (versão seguinte à 1.0) ⏱ semanas

1. Escolher um **agregador autorizado** (Pluggy, Belvo ou outro). A Wall.ia não vai virar participante do Open Finance: isso já está decidido.
2. Pedir proposta com: preço por conexão ativa, bancos cobertos, se fornece eventos (webhooks) ou só consulta, contrato de saída e como os dados são apagados no fim.
3. Com a proposta em mãos, eu escrevo o ADR de escolha do provedor e implemento o adaptador (épica E5 do [`backlog.md`](backlog.md)).

---

## 9. Arrumação (quando tiver tempo) ⏱ 5 min

1. **Dump do banco de dev** que eu fiz antes de atualizar a pilha: `%TEMP%\claude\...\scratchpad\wallia-dev-antes-v31.dump` e a pasta `backups` ao lado. Têm os dados de teste do seu banco local. Quando o bloco 4 passar, diga "pode apagar os dumps".
2. **01/10/2026:** primeira leitura real do placar de precisão da previsão (tela de métricas do admin). Me avise que eu leio junto.

---

## Resumo do que me mandar

- [x] 1: "chave guardada" (26/09, pela sua palavra; eu não leio o arquivo)
- [x] 1.1: JDK 25 instalado e no `JAVA_HOME` (conferido em 26/09)
- [x] 2: `gh` logado (conferido em 26/09; o CI estava vermelho por um teste de fuso, já corrigido)
- [x] 3: audit rodado (26/09; de 68 para 25 alertas, sem crítico)
- [ ] 4: "E2E ok" e suas impressões no celular
- [x] 5: 5.1 a, 5.2 a, 5.3 a (PR #1 aberto em 26/09), 5.4 a (containers apagados em 26/09)
- [ ] 6 a 8: quando tiver respostas do jurídico e dos fornecedores
