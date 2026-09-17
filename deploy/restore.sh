#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "usage: $0 /path/to/pharma-ims-YYYYMMDD-HHMMSS.sql.gz" >&2
  exit 1
fi

: "${MYSQL_HOST:?MYSQL_HOST required}"
: "${MYSQL_PORT:=3306}"
: "${MYSQL_DB:=pharma_ims}"
: "${MYSQL_USERNAME:?MYSQL_USERNAME required}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD required}"

file="$1"
if [[ ! -f "$file" ]]; then
  echo "file not found: $file" >&2
  exit 1
fi

echo "WARNING: this overwrites database $MYSQL_DB on $MYSQL_HOST" >&2
gunzip -c "$file" | mysql \
  --host="$MYSQL_HOST" \
  --port="$MYSQL_PORT" \
  --user="$MYSQL_USERNAME" \
  --password="$MYSQL_PASSWORD" \
  "$MYSQL_DB"

echo "restore finished: $file"
