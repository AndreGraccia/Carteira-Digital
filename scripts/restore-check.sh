#!/usr/bin/env sh
# Prova que um backup volta (docs/operacao.md): restaura num PostgreSQL descartável, nunca no banco em uso,
# e mostra a versão das migrações e a contagem das tabelas principais. Uso: scripts/restore-check.sh backups/wallia-X.dump
set -eu

FILE=$1
NAME=wallia-restore-check

docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" -e POSTGRES_PASSWORD=restore postgres:14.6-alpine >/dev/null
trap 'docker rm -f "$NAME" >/dev/null 2>&1' EXIT
# The image restarts once after initdb: wait until a real query answers.
until docker exec "$NAME" psql -U postgres -tAc 'select 1' >/dev/null 2>&1; do sleep 1; done
sleep 2
until docker exec "$NAME" psql -U postgres -tAc 'select 1' >/dev/null 2>&1; do sleep 1; done

docker exec -i "$NAME" pg_restore -U postgres -d postgres --no-owner --exit-on-error < "$FILE"
docker exec "$NAME" psql -U postgres -tA \
  -c "select 'migracao: V' || max(version::int) from flyway_schema_history where success" \
  -c "select 'usuarios: ' || count(*) from \"user\"" \
  -c "select 'carteiras: ' || count(*) from wallet" \
  -c "select 'lancamentos: ' || count(*) from transaction"
echo "restauracao ok"
