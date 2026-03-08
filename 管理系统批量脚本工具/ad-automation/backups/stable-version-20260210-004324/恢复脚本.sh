#!/bin/bash

# ====================================================================
# 稳定版本一键恢复脚本
# 备份时间：2026-02-10 00:43:24
# ====================================================================

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║       稳定版本恢复脚本 v1.0                     ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""

# 获取脚本所在目录（备份目录）
BACKUP_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "📁 备份目录: $BACKUP_DIR"

# 项目根目录（备份目录的上上上级）
PROJECT_DIR="$(cd "$BACKUP_DIR/../../.." && pwd)"
echo "📁 项目目录: $PROJECT_DIR"

echo ""
echo "⚠️  警告：此操作将覆盖当前项目文件！"
echo ""
read -p "确认要恢复到此备份版本吗？(输入 yes 继续): " confirm

if [ "$confirm" != "yes" ]; then
    echo "❌ 已取消恢复"
    exit 0
fi

echo ""
echo "开始恢复..."
echo ""

# 创建临时备份（当前版本）
TEMP_BACKUP="$PROJECT_DIR/backups/temp-before-restore-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$TEMP_BACKUP"
echo "📦 先备份当前版本到: $TEMP_BACKUP"
cp -r "$PROJECT_DIR/lib" "$TEMP_BACKUP/" 2>/dev/null
cp "$PROJECT_DIR/config.js" "$PROJECT_DIR/index.js" "$PROJECT_DIR/package.json" "$TEMP_BACKUP/" 2>/dev/null

# 恢复文件
echo ""
echo "📂 恢复 lib 目录..."
cp -r "$BACKUP_DIR/lib" "$PROJECT_DIR/"

echo "📄 恢复核心文件..."
cp "$BACKUP_DIR/config.js" "$PROJECT_DIR/"
cp "$BACKUP_DIR/index.js" "$PROJECT_DIR/"
cp "$BACKUP_DIR/package.json" "$PROJECT_DIR/"
cp "$BACKUP_DIR/create-template.js" "$PROJECT_DIR/"

echo "📄 恢复文档文件..."
cp "$BACKUP_DIR"/*.txt "$PROJECT_DIR/" 2>/dev/null || true
cp "$BACKUP_DIR"/*.md "$PROJECT_DIR/" 2>/dev/null || true

echo "📄 恢复辅助脚本..."
cp "$BACKUP_DIR"/*.bat "$PROJECT_DIR/" 2>/dev/null || true
cp "$BACKUP_DIR"/*.sh "$PROJECT_DIR/" 2>/dev/null || true
chmod +x "$PROJECT_DIR"/*.sh 2>/dev/null || true

echo ""
echo "✅ 文件恢复完成！"
echo ""

# 验证恢复
echo "🔍 验证文件完整性..."
cd "$PROJECT_DIR"

if node -c lib/automation.js 2>/dev/null && node -c index.js 2>/dev/null; then
    echo "✅ 语法检查通过"
else
    echo "❌ 语法检查失败，请手动检查"
fi

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║              恢复完成！                          ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""
echo "📋 后续步骤："
echo "   1. 运行 npm install（确保依赖完整）"
echo "   2. 运行 npm test（测试Excel读取）"
echo "   3. 运行 npm start（测试完整流程）"
echo ""
echo "💡 提示："
echo "   - 当前版本已备份到: $TEMP_BACKUP"
echo "   - 如需回退，请手动恢复该备份"
echo ""






