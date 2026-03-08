#!/bin/bash
# 推广链接复制工具 2.0 - 一键启动（精确定位版，Excel: link-tasks-v2.0.xlsx）

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TOOL_DIR="${SCRIPT_DIR}/promotion-link-tool2.0"
cd "$TOOL_DIR" || { echo "❌ 未找到目录: promotion-link-tool2.0"; exit 1; }

echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║      TikTok 推广链接复制工具 2.0 - 一键启动（精确定位）  ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

if ! command -v node &> /dev/null; then
    echo "❌ 未检测到 Node.js，请先安装 Node.js"
    exit 1
fi

if [ ! -d "node_modules" ]; then
    echo "📦 正在安装依赖..."
    npm install
    [ $? -ne 0 ] && exit 1
fi

if [ ! -f "data/link-tasks-v2.0.xlsx" ]; then
    echo "⚠️  未找到 data/link-tasks-v2.0.xlsx，正在生成模板..."
    npm run template
    echo "👉 请填写 data/link-tasks-v2.0.xlsx 后重新运行本脚本"
    exit 0
fi

echo "🚀 启动推广链接 2.0..."
echo ""
npm start
echo ""
