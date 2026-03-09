#!/bin/bash
# 短剧素材制作工具 - 运行脚本
# 用法: ./run.sh analyze /path/to/episodes
#       ./run.sh plan analysis.json

cd "$(dirname "$0")"

if [ ! -d "venv" ]; then
  echo "创建虚拟环境..."
  python3 -m venv venv
fi
source venv/bin/activate 2>/dev/null || source venv/Scripts/activate 2>/dev/null

pip install -q -r requirements.txt

case "$1" in
  analyze)
    python src/analyze.py --input "$2" --output "${3:-analysis.json}"
    ;;
  plan)
    python src/plan.py --input "$2" --output "${3:-plan.json}"
    ;;
  *)
    echo "用法:"
    echo "  ./run.sh analyze <剧集目录> [输出JSON]"
    echo "  ./run.sh plan <分析结果JSON> [输出JSON]"
    exit 1
    ;;
esac
