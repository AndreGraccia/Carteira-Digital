#!/usr/bin/env sh
# Backup do PostgreSQL da Wall.ia (docs/operacao.md). Agendar uma vez por dia no servidor, por exemplo:
#   30 2 * * * cd /srv/wallia && scripts/backup.sh /srv/backups >> /var/log/wallia-backup.log 2>&1
# O arquivo só aparece com o nome final quando o pg_dump termina: um backup pela metade nunca parece completo.
# Os descritivos saem cifrados no dump; sem a data_encryption_key, eles não voltam. Guarde a chave à parte.
set -eu

DIR=${1:-./backups}
KEEP_DAYS=${BACKUP_KEEP_DAYS:-14}
CONTAINER=${DB_CONTAINER:-wallia-db}

mkdir -p "$DIR"
FILE="$DIR/wallia-$(date +%Y%m%d-%H%M%S).dump"
docker exec "$CONTAINER" sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$FILE.partial"
mv "$FILE.partial" "$FILE"
# Backups antigos saem: exclusão de conta e retenção (ADR 0015) só se completam quando o último backup com o dado expira.
find "$DIR" -name 'wallia-*.dump' -mtime +"$KEEP_DAYS" -delete
echo "$FILE"
