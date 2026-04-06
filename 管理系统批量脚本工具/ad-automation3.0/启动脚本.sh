#!/bin/bash

# 设置颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║      TikTok 广告批量建设脚本 - macOS/Linux 启动器        ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

# 检查 Node.js 是否安装
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ 未检测到 Node.js${NC}"
    echo ""
    echo "请先安装 Node.js:"
    echo "  1. 访问 https://nodejs.org/"
    echo "  2. 下载 LTS 版本"
    echo "  3. 安装完成后重新运行此脚本"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ Node.js 已安装${NC}"
node --version
echo ""

# 检查依赖是否安装
if [ ! -d "node_modules" ]; then
    echo -e "${BLUE}📦 首次运行，正在安装依赖...${NC}"
    echo ""
    npm install
    if [ $? -ne 0 ]; then
        echo ""
        echo -e "${RED}❌ 依赖安装失败${NC}"
        echo ""
        echo "可能的解决方法:"
        echo "  1. 使用国内镜像: npm config set registry https://registry.npmmirror.com"
        echo "  2. 检查网络连接"
        echo "  3. 尝试: sudo npm install"
        echo ""
        exit 1
    fi
    echo ""
    echo -e "${GREEN}✅ 依赖安装完成${NC}"
    echo ""
fi

# 检查 Excel 文件是否存在
if [ ! -f "data/tasks-v3.0.xlsx" ]; then
    echo -e "${YELLOW}⚠️  未找到任务数据文件${NC}"
    echo ""
    echo "正在生成 Excel 模板..."
    npm run template
    echo ""
    echo -e "${GREEN}✅ 模板已生成: data/tasks-v3.0.xlsx${NC}"
    echo ""
    echo "👉 请先填写任务数据，然后重新运行此脚本"
    echo ""
    exit 0
fi

echo -e "${BLUE}🔍 建议先测试 Excel 数据...${NC}"
echo ""
read -p "是否先测试 Excel 数据格式? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${BLUE}📊 正在测试 Excel 数据...${NC}"
    npm test
    echo ""
    read -p "按 Enter 继续启动脚本..."
fi

echo ""
echo -e "${BLUE}🚀 启动脚本...${NC}"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  执行流程:"
echo "  1. 浏览器将自动打开"
echo "  2. 如需登录，请手动登录"
echo "  3. 关闭所有弹窗"
echo "  4. 确认在批量工具页面后，按 Enter 开始执行"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

npm start

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}  执行完成！${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

