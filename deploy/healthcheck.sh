#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1}"
FAIL=0

check() {
  local name="$1"
  shift
  if "$@"; then
    echo "OK  $name"
  else
    echo "FAIL $name"
    FAIL=1
  fi
}

check "nginx-config" nginx -t
check "frontend" curl -fsS -o /dev/null -m 8 "$BASE_URL/"
check "publicKey" curl -fsS -m 8 "$BASE_URL/warehouse/login/publicKey" | grep -q '"code"'

if command -v df >/dev/null; then
  used="$(df -P / | awk 'NR==2 {print $5}' | tr -d '%')"
  if [[ "${used:-100}" -lt 85 ]]; then
    echo "OK  disk ${used}%"
  else
    echo "FAIL disk ${used}%"
    FAIL=1
  fi
fi

if [[ -d "${BACKUP_DIR:-/var/backups/pharma-ims}" ]]; then
  latest="$(ls -1t "${BACKUP_DIR:-/var/backups/pharma-ims}"/pharma-ims-*.sql.gz 2>/dev/null | head -1 || true)"
  if [[ -n "$latest" ]]; then
    echo "OK  latest-backup $latest"
  else
    echo "FAIL no-backup-found"
    FAIL=1
  fi
fi

exit "$FAIL"
