#!/usr/bin/env bash
# 查看 MySQL 慢查询相关变量（指令 #075）；密码：MYSQL_PASSWORD 或無密碼
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

DB_HOST="${MYSQL_HOST:-127.0.0.1}"
DB_PORT="${MYSQL_PORT:-3306}"
DB_USER="${MYSQL_USER:-root}"

run_mysql() {
  if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"${MYSQL_PASSWORD}" "$@"
  else
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" "$@"
  fi
}

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}慢查询 / 状态变量${NC}"
echo -e "${GREEN}========================================${NC}"

echo -e "${YELLOW}slow_query_log / long_query_time / slow_query_log_file${NC}"
run_mysql -e "SHOW VARIABLES WHERE Variable_name IN ('slow_query_log','long_query_time','slow_query_log_file');"

echo -e "${YELLOW}Slow_queries（累计）${NC}"
run_mysql -e "SHOW GLOBAL STATUS LIKE 'Slow_queries';"

echo -e "${NC}若需开启（需 DBA 权限）：SET GLOBAL slow_query_log = 'ON'; SET GLOBAL long_query_time = 2;${NC}"
