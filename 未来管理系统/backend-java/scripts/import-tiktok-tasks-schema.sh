#!/usr/bin/env bash
# TikTok 任务表导入（指令 #095-3）
# 依赖：已执行 import-tiktok-core-schema.sh
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$PROJECT_DIR/src/main/resources/sql/schema-tiktok-tasks.sql"

DB_NAME="${DB_NAME:-drama_system}"
DB_USER="${DB_USER:-root}"
DB_HOST="${MYSQL_HOST:-${DB_HOST:-localhost}}"
DB_PORT="${MYSQL_PORT:-3306}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "未找到 SQL 文件: $SQL_FILE" >&2
  exit 1
fi

echo "========================================"
echo "TikTok 任务表导入 -> ${DB_NAME} @ ${DB_HOST}:${DB_PORT}"
echo "========================================"

mysql_base() {
  if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$MYSQL_PASSWORD" "$@"
  else
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" "$@"
  fi
}

if ! echo "SELECT 1;" | mysql_base "$DB_NAME" >/dev/null 2>&1; then
  echo "错误: 无法连接数据库" >&2
  exit 1
fi

if ! echo "SHOW TABLES LIKE 'tiktok_accounts';" | mysql_base "$DB_NAME" | grep -q "tiktok_accounts"; then
  echo "错误: 表 tiktok_accounts 不存在，请先执行 import-tiktok-core-schema.sh" >&2
  exit 1
fi

echo "导入 schema-tiktok-tasks.sql ..."
mysql_base "$DB_NAME" <"$SQL_FILE"

for t in tiktok_ad_tasks tiktok_excel_imports tiktok_sync_logs; do
  if echo "SHOW TABLES LIKE '$t';" | mysql_base "$DB_NAME" | grep -q "$t"; then
    echo "  ✓ $t"
  else
    echo "  ✗ 缺少表 $t" >&2
    exit 1
  fi
done

FK_COUNT=$(mysql_base "$DB_NAME" -N -e "
SELECT COUNT(DISTINCT CONSTRAINT_NAME)
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = '${DB_NAME}'
  AND TABLE_NAME IN ('tiktok_ad_tasks','tiktok_excel_imports','tiktok_sync_logs')
  AND REFERENCED_TABLE_NAME IS NOT NULL;
" 2>/dev/null || echo "0")
echo "外键约束数（去重）: ${FK_COUNT}（预期 3）"

JSON_COUNT=$(mysql_base "$DB_NAME" -N -e "
SELECT COUNT(*)
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = '${DB_NAME}'
  AND TABLE_NAME IN ('tiktok_ad_tasks','tiktok_excel_imports','tiktok_sync_logs')
  AND DATA_TYPE = 'json';
" 2>/dev/null || echo "0")
echo "JSON 列数量: ${JSON_COUNT}（预期 5）"

echo "========================================"
echo "TikTok 任务表导入完成。"
echo "查看全部: SHOW TABLES LIKE 'tiktok_%';"
echo "========================================"
