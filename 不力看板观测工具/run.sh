#!/bin/bash
# 不力看板观测 - 一键执行
cd "$(dirname "$0")"

if [ ! -d ".venv" ]; then
    echo "请先运行: bash setup_venv.sh"
    exit 1
fi

if [ ! -f ".env" ]; then
    echo "请复制 .env.example 为 .env 并填入配置"
    exit 1
fi

# 复用其他工具的 Playwright 浏览器（ad-automation2.0 等已安装）
PW_BROWSERS="$(cd "$(dirname "$0")/../管理系统批量脚本工具/ad-automation2.0/pw-browsers" 2>/dev/null && pwd)"
if [ -d "$PW_BROWSERS" ] && [ -d "$PW_BROWSERS/chromium_headless_shell-1208" ]; then
    export PLAYWRIGHT_BROWSERS_PATH="$PW_BROWSERS"
fi

.venv/bin/python -m src.main "$@"
