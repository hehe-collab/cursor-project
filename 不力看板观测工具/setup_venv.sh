#!/bin/bash
# 创建虚拟环境并安装依赖
cd "$(dirname "$0")"
echo "删除旧 venv..."
rm -rf .venv
echo "创建新 venv..."
python3 -m venv .venv
echo "安装 Python 依赖..."
.venv/bin/pip install -r requirements.txt
echo "安装 Chromium 浏览器（Playwright 需要）..."
.venv/bin/playwright install chromium
echo "完成。可运行: bash run.sh"
