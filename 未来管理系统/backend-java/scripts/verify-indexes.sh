#!/usr/bin/env bash
# 索引验证脚本（指令 #074）：列出新增 idx_perf_* 并做 EXPLAIN 抽样
set -eo pipefail

DB_NAME="${DB_NAME:-drama_system}"
DB_USER="${DB_USER:-root}"
DB_HOST="${MYSQL_HOST:-127.0.0.1}"
DB_PORT="${MYSQL_PORT:-3306}"

run_mysql() {
  if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"${MYSQL_PASSWORD}" "$DB_NAME" "$@"
  else
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" "$DB_NAME" "$@"
  fi
}

echo "=== idx_perf_* 索引（users / dramas / recharge_records / promotion_links）==="
run_mysql -e "
SHOW INDEX FROM users WHERE Key_name LIKE 'idx_perf_%';
SHOW INDEX FROM dramas WHERE Key_name LIKE 'idx_perf_%';
SHOW INDEX FROM recharge_records WHERE Key_name LIKE 'idx_perf_%';
SHOW INDEX FROM promotion_links WHERE Key_name LIKE 'idx_perf_%';
"

echo ""
echo "=== EXPLAIN 抽样（列名与 schema 一致）==="

echo "-- 按推广字段查用户（promote_id）"
run_mysql -e "EXPLAIN SELECT * FROM users WHERE promote_id = 'P001' LIMIT 10;"

echo "-- 短剧：状态 + 时间排序（ENUM）"
run_mysql -e "EXPLAIN SELECT * FROM dramas WHERE status = 'published' ORDER BY created_at DESC LIMIT 10;"

echo "-- 充值：业务 user_id（字符串）+ 排序"
run_mysql -e "EXPLAIN SELECT * FROM recharge_records WHERE user_id = '101' ORDER BY created_at DESC LIMIT 20;"

echo "完成。"
