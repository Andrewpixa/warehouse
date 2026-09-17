#!/usr/bin/env bash
set -euo pipefail

: "${MYSQL_HOST:?MYSQL_HOST required}"
: "${MYSQL_PORT:=3306}"
: "${MYSQL_DB:=pharma_ims}"
: "${MYSQL_USERNAME:?MYSQL_USERNAME required}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD required}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/pharma-ims}"
KEEP="${KEEP:-14}"

mkdir -p "$BACKUP_DIR"
chmod 750 "$BACKUP_DIR"

stamp="$(date +%Y%m%d-%H%M%S)"
out="$BACKUP_DIR/pharma-ims-$stamp.sql.gz"

mysqldump \
  --host="$MYSQL_HOST" \
  --port="$MYSQL_PORT" \
  --user="$MYSQL_USERNAME" \
  --password="$MYSQL_PASSWORD" \
  --single-transaction \
  --routines \
  --triggers \
  --default-character-set=utf8mb4 \
  "$MYSQL_DB" | gzip -c > "$out"

chmod 640 "$out"
echo "backup written: $out"

ls -1t "$BACKUP_DIR"/pharma-ims-*.sql.gz 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm -f
