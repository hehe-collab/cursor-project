#!/bin/bash

# 获取脚本所在目录，确保在任意地方执行该脚本都能以脚本所在目录为工作目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

# 检查是否安装了 python3
if ! command -v python3 &> /dev/null; then
    echo "错误: 未找到 python3，请先安装 Python 3 环境。"
    exit 1
fi

# 检查是否安装了 ffmpeg
if ! command -v ffmpeg &> /dev/null; then
    echo "错误: 未找到 ffmpeg。如果是 macOS，请运行: brew install ffmpeg"
    exit 1
fi

# 运行 Python 脚本
python3 merge_drama.py
