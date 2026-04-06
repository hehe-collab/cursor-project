@echo off
chcp 65001 >nul
echo.
echo ╔═══════════════════════════════════════════════════════════╗
echo ║        TikTok 广告批量建设脚本 - Windows 启动器          ║
echo ╚═══════════════════════════════════════════════════════════╝
echo.

REM 检查 Node.js 是否安装
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ 未检测到 Node.js
    echo.
    echo 请先安装 Node.js:
    echo   1. 访问 https://nodejs.org/
    echo   2. 下载 LTS 版本
    echo   3. 安装完成后重新运行此脚本
    echo.
    pause
    exit /b 1
)

echo ✅ Node.js 已安装
node --version
echo.

REM 检查依赖是否安装
if not exist "node_modules\" (
    echo 📦 首次运行，正在安装依赖...
    echo.
    call npm install
    if %errorlevel% neq 0 (
        echo.
        echo ❌ 依赖安装失败
        echo.
        echo 可能的解决方法:
        echo   1. 使用国内镜像: npm config set registry https://registry.npmmirror.com
        echo   2. 以管理员身份运行此脚本
        echo   3. 检查网络连接
        echo.
        pause
        exit /b 1
    )
    echo.
    echo ✅ 依赖安装完成
    echo.
)

REM 检查 Excel 文件是否存在
if not exist "data\tasks-v3.0.xlsx" (
    echo ⚠️  未找到任务数据文件
    echo.
    echo 正在生成 Excel 模板...
    call npm run template
    echo.
    echo ✅ 模板已生成: data\tasks-v3.0.xlsx
    echo.
    echo 👉 请先填写任务数据，然后重新运行此脚本
    echo.
    pause
    exit /b 0
)

echo 🔍 建议先测试 Excel 数据...
echo.
choice /C YN /M "是否先测试 Excel 数据格式 (Y=是, N=跳过直接启动)"
if errorlevel 2 goto :start_script
if errorlevel 1 goto :test_data

:test_data
echo.
echo 📊 正在测试 Excel 数据...
call npm test
echo.
pause
goto :start_script

:start_script
echo.
echo 🚀 启动脚本...
echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo   执行流程:
echo   1. 浏览器将自动打开
echo   2. 如需登录，请手动登录
echo   3. 关闭所有弹窗
echo   4. 确认在批量工具页面后，按 Enter 开始执行
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

call npm start

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo   执行完成！
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
pause

