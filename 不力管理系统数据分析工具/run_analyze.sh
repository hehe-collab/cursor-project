#!/bin/bash
# 剧分析：每日消耗与利润
# 用法：bash run_analyze.sh [--list]
# --list: 列出所有剧名，将选中的填入 config.yaml 的 剧名 字段
cd "$(dirname "$0")"
.venv/bin/pip install -q -r requirements.txt 2>/dev/null
.venv/bin/python3 src/analyze.py "$@"
