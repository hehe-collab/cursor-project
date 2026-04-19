#!/usr/bin/env bash
# =====================================================
# Pre-commit Secret Scanner
# 用途：在 git commit 前扫描 staged 文件，发现疑似 secret 立即阻止提交
# 创建于 2026-04-19
# =====================================================
#
# 安装为 git pre-commit hook：
#   ln -s ../../scripts/check-secrets.sh .git/hooks/pre-commit
#   chmod +x scripts/check-secrets.sh
#
# 手动运行（扫描当前已 staged 的文件）：
#   ./scripts/check-secrets.sh
#
# 跳过本次检查（极少数误报场景）：
#   git commit --no-verify ...
#
# =====================================================

set -uo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m'

HITS=0
HITS_FILE="$(mktemp)"
trap 'rm -f "$HITS_FILE"' EXIT

# 获取本次将被提交的文件列表（仅限新增/修改部分）
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACMR 2>/dev/null)

if [ -z "$STAGED_FILES" ]; then
  echo -e "${GREEN}[check-secrets]${NC} 没有 staged 文件，跳过扫描。"
  exit 0
fi

echo -e "${GREEN}[check-secrets]${NC} 扫描 $(echo "$STAGED_FILES" | wc -l | tr -d ' ') 个 staged 文件……"

# =====================================================
# 检测规则：每条规则 = 「描述|正则」
# =====================================================
PATTERNS=(
  "阿里云 AccessKey ID|LTAI[A-Za-z0-9]{12,30}"
  "AWS Access Key ID|AKIA[0-9A-Z]{16}"
  "AWS Secret Key（疑似）|aws_secret_access_key\\s*=\\s*[A-Za-z0-9/+=]{30,}"
  "GitHub Token|gh[oprsu]_[A-Za-z0-9]{30,}"
  "GitLab Token|glpat-[A-Za-z0-9_-]{20,}"
  "Slack Token|xox[abprs]-[A-Za-z0-9-]{10,}"
  "Google API Key|AIza[0-9A-Za-z_-]{35}"
  "Stripe Key|(sk|pk|rk)_(live|test)_[A-Za-z0-9]{20,}"
  "Private SSH Key|-----BEGIN (OPENSSH|RSA|EC|DSA) PRIVATE KEY-----"
  "JWT Token（疑似）|eyJ[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}"
  "明文密码赋值（>=12位）|(password|passwd|pwd)\\s*[:=]\\s*['\"]?[A-Za-z0-9!@#\$%^&*()_+=-]{12,}['\"]?"
  "明文 secret/token（>=20位）|(secret|token|api[_-]?key)\\s*[:=]\\s*['\"]?[A-Za-z0-9!@#\$%^&*()_+=-]{20,}['\"]?"
)

# =====================================================
# 已知误报白名单（这些值是公开的示例/占位符，不算 secret）
# =====================================================
KNOWN_FALSE_POSITIVES=(
  "LTAI5tABCDEF1234"          # 文档示例占位符
  "LTAIxxxxxxxxxxxxxxxx"      # 文档占位符
  "LTAI你的真实ID粘贴在这里"   # 命令模板占位符
  "AKIAIOSFODNN7EXAMPLE"      # AWS 官方文档示例
  "AIzaSyDxxxxxxxxxxxxxxxxxxx"
  "892c49b1-8b4f-447f-ae84-da0be2b83edb"  # Spring Boot 自动生成的临时密码
  "your_password"
  "YOUR_PASSWORD_HERE"
  "YOUR_SECRET_HERE"
  "REPLACE_ME"
  "changeme"
  "admin123"                  # 测试默认密码（虽然弱，但已知是测试用）
)

is_false_positive() {
  local line="$1"
  for fp in "${KNOWN_FALSE_POSITIVES[@]}"; do
    if echo "$line" | grep -qF -- "$fp"; then
      return 0
    fi
  done
  return 1
}

# =====================================================
# 主扫描循环
# =====================================================
while IFS= read -r FILE; do
  [ -z "$FILE" ] && continue
  [ ! -f "$FILE" ] && continue

  # 跳过二进制文件
  if file --mime "$FILE" 2>/dev/null | grep -q "charset=binary"; then
    continue
  fi

  # 跳过过大文件（>2MB），它们通常是数据/资源
  SIZE=$(wc -c <"$FILE" 2>/dev/null || echo 0)
  if [ "$SIZE" -gt 2097152 ]; then
    continue
  fi

  for PATTERN_ENTRY in "${PATTERNS[@]}"; do
    DESC="${PATTERN_ENTRY%%|*}"
    REGEX="${PATTERN_ENTRY#*|}"

    MATCHES=$(grep -nE "$REGEX" "$FILE" 2>/dev/null || true)
    [ -z "$MATCHES" ] && continue

    while IFS= read -r MATCH_LINE; do
      [ -z "$MATCH_LINE" ] && continue
      LINE_NUM="${MATCH_LINE%%:*}"
      LINE_CONTENT="${MATCH_LINE#*:}"

      if is_false_positive "$LINE_CONTENT"; then
        continue
      fi

      # 截断显示，避免把完整 secret 打到终端 / CI 日志
      DISPLAY=$(echo "$LINE_CONTENT" | cut -c1-80)
      [ "${#LINE_CONTENT}" -gt 80 ] && DISPLAY="${DISPLAY}..."

      echo -e "${RED}[!]${NC} ${YELLOW}${FILE}:${LINE_NUM}${NC} (${DESC})"
      echo -e "    ${DISPLAY}"
      HITS=$((HITS + 1))
      echo "$FILE:$LINE_NUM" >>"$HITS_FILE"
    done <<<"$MATCHES"
  done
done <<<"$STAGED_FILES"

# =====================================================
# 输出结果
# =====================================================
if [ "$HITS" -gt 0 ]; then
  echo ""
  echo -e "${RED}=========================================================${NC}"
  echo -e "${RED}❌ 发现 $HITS 处疑似 secret，已阻止本次 commit。${NC}"
  echo -e "${RED}=========================================================${NC}"
  echo ""
  echo "处理建议："
  echo "  1) 真泄露 → 把 secret 从代码里挪到环境变量 / 凭证管理器，重新 git add"
  echo "  2) 误报   → 把模式加进 scripts/check-secrets.sh 的 KNOWN_FALSE_POSITIVES"
  echo "  3) 真要提交（极不推荐）→ git commit --no-verify"
  echo ""
  exit 1
fi

echo -e "${GREEN}[check-secrets] ✅ 未发现疑似 secret，放行。${NC}"
exit 0
