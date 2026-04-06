#!/usr/bin/env bash
# 导入 demo_data.sql；需已建库 drama_system 并已执行 schema.sql
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$PROJECT_DIR/src/main/resources/sql/demo_data.sql"

DB_NAME="${DB_NAME:-drama_system}"
DB_USER="${DB_USER:-root}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "未找到: $SQL_FILE" >&2
  exit 1
fi

echo "正在导入演示数据 -> 数据库 ${DB_NAME}（用户: ${DB_USER}）..."

if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
  mysql -u"${DB_USER}" -p"${MYSQL_PASSWORD}" "${DB_NAME}" <"${SQL_FILE}"
else
  mysql -u"${DB_USER}" "${DB_NAME}" <"${SQL_FILE}"
fi

echo "演示数据导入成功。"
echo "可测试: GET /api/users、GET /api/recharge、GET /api/dramas/501（需 Bearer Token）"
