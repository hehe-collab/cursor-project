#!/usr/bin/env bash
# TikTok 核心表结构导入（指令 #095-1）
# 需已建库 drama_system；与 import-demo-data.sh 一致使用 DB_NAME / DB_USER / MYSQL_PASSWORD
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$PROJECT_DIR/src/main/resources/sql/schema-tiktok-core.sql"

DB_NAME="${DB_NAME:-drama_system}"
DB_USER="${DB_USER:-root}"
DB_HOST="${MYSQL_HOST:-${DB_HOST:-localhost}}"
DB_PORT="${MYSQL_PORT:-3306}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "未找到 SQL 文件: $SQL_FILE" >&2
  exit 1
fi

echo "========================================"
echo "TikTok 核心表结构导入 -> ${DB_NAME} @ ${DB_HOST}:${DB_PORT}"
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
  echo "错误: 无法连接数据库，请检查 DB_NAME / DB_USER / MYSQL_PASSWORD / MYSQL_HOST" >&2
  exit 1
fi
echo "连接成功。"

echo "导入 schema-tiktok-core.sql ..."
mysql_base "$DB_NAME" <"$SQL_FILE"
echo "导入完成。"

echo "验证表是否存在..."
for t in tiktok_accounts tiktok_campaigns tiktok_adgroups tiktok_ads; do
  if echo "SHOW TABLES LIKE '$t';" | mysql_base "$DB_NAME" | grep -q "$t"; then
    echo "  ✓ $t"
  else
    echo "  ✗ 缺少表 $t" >&2
    exit 1
  fi
done

echo "========================================"
echo "TikTok 核心表就绪。"
echo "若曾单独执行 tiktok-accounts.sql 且无 balance 列，请按 SQL 文件头注释执行 ALTER。"
echo "========================================"
