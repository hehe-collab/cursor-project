#!/bin/bash
# 重建虚拟环境（项目移动后 venv 会失效，需重新创建）
cd "$(dirname "$0")"
echo "删除旧 venv..."
rm -rf .venv
echo "创建新 venv..."
python3 -m venv .venv
echo "安装依赖..."
.venv/bin/pip install -r requirements.txt
echo "完成。可运行: bash run_parse.sh"