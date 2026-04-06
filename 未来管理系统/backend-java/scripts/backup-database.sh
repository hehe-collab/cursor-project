#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-drama_system}"
DB_USER="${DB_USERNAME:-root}"
DB_PASS="${DB_PASSWORD:-${MYSQL_PASSWORD:-}}"

BACKUP_DIR="${HOME}/backups/mysql"
mkdir -p "$BACKUP_DIR"
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_$(date +%Y%m%d_%H%M%S).sql"

echo "备份 ${DB_NAME} → ${BACKUP_FILE}"

if [ -z "$DB_PASS" ]; then
  mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" \
    --single-transaction --routines --triggers --events \
    "$DB_NAME" > "$BACKUP_FILE"
else
  mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
    --single-transaction --routines --triggers --events \
    "$DB_NAME" > "$BACKUP_FILE"
fi

gzip "$BACKUP_FILE"
echo "已压缩: ${BACKUP_FILE}.gz"
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +7 -delete 2>/dev/null || true
ls -lh "$BACKUP_DIR" | tail -15
