#!/usr/bin/env bash
# TikTok 扩展表导入（指令 #095-2）
# 依赖：已执行 import-tiktok-core-schema.sh（存在 tiktok_accounts）
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$PROJECT_DIR/src/main/resources/sql/schema-tiktok-extended.sql"

DB_NAME="${DB_NAME:-drama_system}"
DB_USER="${DB_USER:-root}"
DB_HOST="${MYSQL_HOST:-${DB_HOST:-localhost}}"
DB_PORT="${MYSQL_PORT:-3306}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "未找到 SQL 文件: $SQL_FILE" >&2
  exit 1
fi

echo "========================================"
echo "TikTok 扩展表导入 -> ${DB_NAME} @ ${DB_HOST}:${DB_PORT}"
echo "========================================"

mysql_base() {
  if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$MYSQL_PASSWORD" "$@"
  else
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" "$@"
  fi
}

echo "测试数据库连接..."
if ! echo "SELECT 1;" | mysql_base "$DB_NAME" >/dev/null 2>&1; then
  echo "错误: 无法连接数据库" >&2
  exit 1
fi

echo "检查依赖表 tiktok_accounts ..."
if ! echo "SHOW TABLES LIKE 'tiktok_accounts';" | mysql_base "$DB_NAME" | grep -q "tiktok_accounts"; then
  echo "错误: 表 tiktok_accounts 不存在，请先执行 scripts/import-tiktok-core-schema.sh" >&2
  exit 1
fi

echo "导入 schema-tiktok-extended.sql ..."
mysql_base "$DB_NAME" <"$SQL_FILE"

for t in tiktok_pixels tiktok_conversion_logs tiktok_reports; do
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
  AND TABLE_NAME IN ('tiktok_pixels','tiktok_conversion_logs','tiktok_reports')
  AND REFERENCED_TABLE_NAME IS NOT NULL;
" 2>/dev/null || echo "0")

echo "外键约束数（去重）: ${FK_COUNT}（预期 4：pixels×1 + conversion×2 + reports×1）"
echo "========================================"
echo "TikTok 扩展表导入完成。"
echo "========================================"
