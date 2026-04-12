#!/bin/bash
# generate-keys.sh - 生成安全密钥脚本
# 用法: chmod +x scripts/generate-keys.sh && ./scripts/generate-keys.sh

echo "生成安全密钥..."
echo ""

# 生成 32 字符随机密钥
ENCRYPT_KEY=$(openssl rand -base64 32 | tr -d '\n')
JWT_KEY=$(openssl rand -base64 48 | tr -d '\n')

echo "密钥生成成功！"
echo ""
echo "请将以下内容添加到 .env 文件："
echo "================================"
echo "ENCRYPT_SECRET_KEY=$ENCRYPT_KEY"
echo "JWT_SECRET=$JWT_KEY"
echo "================================"
echo ""
echo "警告：请妥善保管密钥，不要提交到 Git！"
