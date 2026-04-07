#!/usr/bin/env bash
# Mapper 查询粗分析（指令 #075）：统计 SELECT、提示人工查 Service 循环调用
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAPPER_DIR="$SCRIPT_DIR/../src/main/resources/mapper"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}查询分析（MyBatis XML）${NC}"
echo -e "${GREEN}========================================${NC}"

if [[ ! -d "$MAPPER_DIR" ]]; then
  echo -e "${RED}未找到: $MAPPER_DIR${NC}" >&2
  exit 1
fi

echo -e "${YELLOW}1. SELECT 数量${NC}"
echo -n "总 SELECT："
grep -r "<select" "$MAPPER_DIR" --include='*.xml' | wc -l | tr -d ' '

echo ""
echo -e "${YELLOW}2. JOIN 使用情况（文件名 + 次数）${NC}"
grep -rni "join" "$MAPPER_DIR" --include='*.xml' | cut -d: -f1 | sort | uniq -c | sort -nr | head -20

echo ""
echo -e "${YELLOW}3. 仍在用 select * 的语句（可逐步改为列清单）${NC}"
grep -rni "select \\*" "$MAPPER_DIR" --include='*.xml' | head -15 || true

echo ""
echo -e "${YELLOW}4. 人工检查${NC}"
echo "请检索 Java：在 for/stream 内调用 mapper.selectById / getById 等，易形成 N+1。"
echo -e "${GREEN}完成${NC}"
