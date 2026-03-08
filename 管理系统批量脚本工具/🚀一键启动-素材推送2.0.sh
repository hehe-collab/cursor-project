#!/bin/bash
# 素材推送工具 2.0 - 一键启动（Excel: push-tasks-v2.0.xlsx）

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TOOL_DIR="${SCRIPT_DIR}/material-push-tool2.0"
cd "$TOOL_DIR" || { echo "❌ 未找到目录: material-push-tool2.0"; exit 1; }

echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║      素材推送工具 2.0 - 一键启动                          ║"
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

if [ ! -f "data/push-tasks-v2.0.xlsx" ]; then
    echo "⚠️  未找到 data/push-tasks-v2.0.xlsx，正在生成模板..."
    npm run template
    echo "👉 请填写 data/push-tasks-v2.0.xlsx 后重新运行本脚本"
    exit 0
fi

echo "🚀 启动素材推送 2.0..."
echo ""
npm start
echo ""
