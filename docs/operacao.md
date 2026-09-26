# Operação da Wall.ia em homologação e produção

O que fazer para subir, proteger, guardar e recuperar o ambiente. O fluxo de entrega está em [`backlog.md`](backlog.md); o roteiro de homologação no [guia](guia-do-desenvolvedor.md), seção 13.1.

## 1. Entrada pública com TLS

A pilha não fala HTTPS sozinha. O nginx do frontend escuta HTTP na porta 3000, publicada só em `127.0.0.1:3001`. A API fica em `127.0.0.1:8082`. Ninguém de fora do servidor alcança essas portas.

Na frente vai um proxy com TLS. Exemplo com Caddy, que emite e renova o certificado sozinho:

```
wallia.exemplo.com.br {
    reverse_proxy 127.0.0.1:3001
    header Strict-Transport-Security "max-age=31536000; includeSubDomains"
}
```

- **HSTS fica no proxy**, porque só ele sabe que a conexão é HTTPS. Liga depois que o certificado estiver funcionando, porque o navegador passa a recusar HTTP por um ano.
- **IP do cliente.** O proxy manda `X-Forwarded-For`. O nginx só confia nele quando a conexão vem de faixa privada (`set_real_ip_from`), que é o caso do proxy chegando pelo gateway do Docker. Sem isso, todo mundo teria o IP do proxy e os limites de cadastro e login por endereço virariam um limite único para todos. Teste feito em 25/09: com 6 tentativas erradas do cliente A, A é bloqueado (429) e B continua livre (401).
- **`frontend_origin`** no `.env.properties` precisa ser o endereço público (`https://wallia.exemplo.com.br`).

## 2. Backup e restauração

- **Backup:** `scripts/backup.sh /srv/backups`, uma vez por dia pelo cron do servidor. Guarda `BACKUP_KEEP_DAYS` dias (padrão 14) e apaga os mais antigos.
- **Prova de restauração:** `scripts/restore-check.sh /srv/backups/wallia-AAAAMMDD-HHMMSS.dump`. Restaura num PostgreSQL descartável, nunca no banco em uso, e mostra a versão das migrações e as contagens. Rodar uma vez por mês e depois de cada mudança de versão do PostgreSQL. Teste feito em 25/09: V31, 32 usuários, 36 carteiras e 1003 lançamentos, iguais ao banco de origem.
- **Cópia fora do servidor.** Um backup no mesmo disco não protege contra perda do disco. Copiar para outro lugar (outro servidor ou armazenamento de objetos), com acesso restrito.
- **A chave vai junto, mas separada.** As descrições saem cifradas no dump. Sem a `data_encryption_key` (e as anteriores, durante uma rotação), elas não voltam. Guardar a chave num cofre de segredos, nunca no mesmo lugar do backup.
- **Prazo e LGPD.** Uma conta excluída continua nos backups até o mais novo com o dado expirar. Com 14 dias de retenção, a exclusão se completa em até 14 dias (ADR 0015).

Restaurar de verdade (desastre):

1. Parar a API: `docker compose ... stop backend`.
2. Recriar o banco vazio e restaurar: `docker exec -i wallia-db pg_restore -U <db_username> -d <db_name> --clean --if-exists --no-owner < arquivo.dump`.
3. Subir a API com a mesma `data_encryption_key` do backup. O Flyway aplica as migrações que faltarem.

## 3. Rotação da chave de criptografia (ADR 0016)

Quando trocar: suspeita de vazamento da chave, saída de alguém que tinha acesso, ou uma vez por ano.

1. Gerar a nova: `openssl rand -base64 32`.
2. No `.env.properties`: a nova em `data_encryption_key` e a antiga em `data_encryption_previous_keys` (várias, separadas por vírgula).
3. Reiniciar a API. Tudo o que for gravado a partir daí usa a chave nova, e o que já existe continua legível.
4. O job de rotação (3h45, `encryption_rotation_cron`) regrava o que estiver na chave antiga, em `v1` ou em texto puro. O log mostra `Encryption rotation: N rewritten, 0 unreadable`.
5. Quando uma execução não tiver mais nada a regravar, tirar a chave antiga de `data_encryption_previous_keys` e reiniciar.
6. Os backups feitos antes da troca ainda precisam da chave antiga. Guardar a chave antiga até o último desses backups expirar.

Se o log mostrar `unreadable` maior que zero: existe dado cifrado com uma chave que não está configurada. Não remover nenhuma chave antes de entender de onde ele veio.

## 4. Incidente de segurança

Incidente é qualquer suspeita de acesso indevido a dados pessoais: chave ou senha vazada, dump fora do lugar, acesso estranho no log, falha que expôs dado de outra pessoa.

**Na primeira hora:**

1. Registrar o que se sabe, com horário: o que aconteceu, desde quando, quais dados, quantas pessoas.
2. Conter:
   - **Sessões:** trocar `jwt_secret` e reiniciar a API. Todos os tokens emitidos deixam de valer.
   - **Banco:** trocar `db_password` no PostgreSQL e no `.env.properties`.
   - **Criptografia:** rotacionar a `data_encryption_key` (seção 3).
   - **Integrações:** revogar a chave do Gemini no AI Studio e as credenciais do Open Finance no provedor.
3. Guardar evidência antes de limpar: logs dos containers (`docker compose logs > incidente.log`) e o backup mais recente.

**Comunicação (LGPD, art. 48, e Resolução CD/ANPD nº 15/2024):**

- Se o incidente pode gerar risco ou dano relevante às pessoas (dado financeiro entra aqui), a ANPD e as pessoas afetadas precisam ser avisadas em até 3 dias úteis, contados de quando se soube que dados pessoais foram afetados.
- A comunicação diz: que dados, quantas pessoas, o que foi feito para conter, que riscos elas correm e o que podem fazer.
- Confirmar prazos e formulário no site da ANPD antes de enviar. Esta seção não substitui o jurídico.

**Depois:** registrar causa, correção e prevenção no CHANGELOG e no Jarbas, com um teste que trave a repetição.

## 5. Métricas, logs e alertas

- **Métricas:** `http://127.0.0.1:8082/actuator/prometheus`, só no servidor. O nginx não repassa `/actuator`. Um Prometheus (ou Grafana Agent) no mesmo servidor coleta a cada 30 s.
- **Logs:** em produção, uma linha JSON por evento no padrão ECS (`LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`), pronta para Loki, Elastic ou CloudWatch. Em desenvolvimento, texto normal.

Alertas que valem a pena desde o primeiro dia:

1. **Job parado.** `time() - wallia_job_last_success{name="open-finance-daily-sync"} > 26*3600`. O sync não roda com sucesso há mais de um dia. Vale o mesmo para `data-retention` e `market-rates-refresh`.
2. **Job falhando.** `increase(wallia_job_seconds_count{outcome="failure"}[1d]) > 0`.
3. **API fora do ar.** `up == 0` no alvo da API, ou readiness `DOWN` por mais de 2 minutos.
4. **Erros 5xx.** `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) > 0.1`.
5. **Pool de conexões no limite.** `hikaricp_connections_pending > 0` por mais de 5 minutos.

Os nomes dos jobs (label `name`) são os de `JobLock.runExclusive`: `open-finance-daily-sync`, `future-alerts`, `market-rates-refresh`, `data-retention` e `encryption-rotation`.

## 6. Checagens de rotina

- **Saúde:** `docker compose ... ps` mostra `healthy` no banco e no backend. A readiness fica `DOWN` se o banco cair.
- **Jobs da madrugada**, no log do backend: retenção (3h30), rotação (3h45), taxas do BCB (6h), sync do Open Finance (7h30) e alertas (8h). Todos rodam numa instância só (`JobLock`).
- **Espaço em disco:** logs dos containers giram em 5 arquivos de 100 MB por serviço; backups, pelo `BACKUP_KEEP_DAYS`.
