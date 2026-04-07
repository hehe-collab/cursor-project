#!/usr/bin/env bash
# 性能索引应用脚本（指令 #074）
# 用法：
#   cd backend-java && bash scripts/apply-performance-indexes.sh
# 有密码：MYSQL_PASSWORD='xxx' bash scripts/apply-performance-indexes.sh

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$PROJECT_DIR/src/main/resources/sql/performance_indexes.sql"

DB_NAME="${DB_NAME:-drama_system}"
DB_USER="${DB_USER:-root}"
DB_HOST="${MYSQL_HOST:-127.0.0.1}"
DB_PORT="${MYSQL_PORT:-3306}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "未找到: $SQL_FILE" >&2
  exit 1
fi

echo "正在应用性能索引 -> ${DB_NAME} @ ${DB_HOST}:${DB_PORT}（用户: ${DB_USER}）..."

if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
  mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"${MYSQL_PASSWORD}" "${DB_NAME}" <"${SQL_FILE}"
else
  mysql -h"$DB_HOST" -P"$DB_PORT" -u"${DB_USER}" "${DB_NAME}" <"${SQL_FILE}"
fi

echo "性能索引 SQL 已执行。"
echo "可执行: bash scripts/verify-indexes.sh"
