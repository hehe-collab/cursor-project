#!/bin/bash
# 解析 xlsx：用法 bash run_parse.sh [xlsx路径]
# 若报错 ModuleNotFoundError 或 bad interpreter，先运行: bash setup_venv.sh
cd "$(dirname "$0")"
.venv/bin/python3 src/parse_export.py "$@"
