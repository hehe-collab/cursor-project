#!/bin/bash
# 安装 launchd 定时任务：按 config.yaml 的 interval_minutes 定期执行（夜间自动跳过）
# 运行: bash setup_launchd.sh
# 可选: bash setup_launchd.sh --load  自动加载任务

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PLIST_NAME="com.buli.kanban.monitor"
PLIST_PATH="$HOME/Library/LaunchAgents/${PLIST_NAME}.plist"

# 从 config.yaml 读取 interval_minutes，默认 30
INTERVAL_MINUTES=30
if [ -f "${SCRIPT_DIR}/config.yaml" ]; then
    val=$(grep -E "interval_minutes:" "${SCRIPT_DIR}/config.yaml" | head -1 | sed 's/.*: *//' | tr -d ' ')
    [ -n "$val" ] && [ "$val" -gt 0 ] 2>/dev/null && INTERVAL_MINUTES=$val
fi
INTERVAL_SEC=$((INTERVAL_MINUTES * 60))

mkdir -p "${SCRIPT_DIR}/output"

cat > "$PLIST_PATH" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>${PLIST_NAME}</string>
    <key>ProgramArguments</key>
    <array>
        <string>/bin/bash</string>
        <string>${SCRIPT_DIR}/run.sh</string>
    </array>
    <key>WorkingDirectory</key>
    <string>${SCRIPT_DIR}</string>
    <key>StartInterval</key>
    <integer>${INTERVAL_SEC}</integer>
    <key>RunAtLoad</key>
    <true/>
    <key>StandardOutPath</key>
    <string>${SCRIPT_DIR}/output/launchd.log</string>
    <key>StandardErrorPath</key>
    <string>${SCRIPT_DIR}/output/launchd_err.log</string>
</dict>
</plist>
EOF

echo "已生成: $PLIST_PATH"
echo "定时间隔: 每 ${INTERVAL_MINUTES} 分钟（config.yaml schedule.interval_minutes）"
echo ""

if [ "$1" = "--load" ] || [ "$1" = "-l" ]; then
    launchctl unload "$PLIST_PATH" 2>/dev/null
    launchctl load "$PLIST_PATH"
    echo "已加载定时任务，登录后会自动执行。"
else
    echo "加载: launchctl load $PLIST_PATH"
    echo "卸载: launchctl unload $PLIST_PATH"
    echo "状态: launchctl list | grep $PLIST_NAME"
    echo ""
    echo "或运行: bash setup_launchd.sh --load  自动加载"
fi
echo ""
echo "说明：脚本内部按 config.yaml schedule 检查时段，夜间自动跳过。"
