#!/bin/bash
# 快捷同步到 GitHub（排除 .gitignore 中的 screenshots、mp4 等）

set -e

PROJECT_DIR="/Volumes/存钱罐/cursor项目"
cd "$PROJECT_DIR"

MSG="${1:-sync}"
git add .
git commit -m "$MSG"
git push origin main

echo "✅ 已同步到 GitHub"
