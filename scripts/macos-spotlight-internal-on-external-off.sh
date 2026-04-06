#!/usr/bin/env bash
# 在内置系统卷重新开启 Spotlight 索引，在外置卷关闭（减轻 corespotlightd / 外置盘压力）
# 用法：bash "/Volumes/存钱罐/cursor项目/scripts/macos-spotlight-internal-on-external-off.sh"
#
# 【重要】请勿与下列命令混用或反复执行，否则会把刚开好的索引再次关掉，并触发长时间重建，
#        corespotlightd 会长时间高 CPU：
#   sudo mdutil -E /          # 抹索引，必引发大量后台工作
#   sudo mdutil -a -i off     # 全盘关聚焦，与「内置可搜」目标相反
#
# 说明：部分 macOS 上对 /System/Volumes/Data 直接 mdutil 会报 -405 / unknown indexing state，
# 用户目录实际在同一 APFS 组，仅开启 / 往往已足够让聚焦索引用户文件；Data 报错可忽略。

set -uo pipefail

EXTERNAL="/Volumes/存钱罐"

enable_vol() {
  local mp="$1"
  if sudo mdutil -i on "$mp"; then
    return 0
  fi
  echo "    （警告）$mp 未能开启索引，已跳过，后文仍会处理外置卷。"
  return 1
}

echo "==> 开启内置卷索引：/ （以及尽量开启 /System/Volumes/Data）"
enable_vol / || true
enable_vol /System/Volumes/Data || true

if [[ -d "$EXTERNAL" ]]; then
  echo "==> 关闭外置卷索引：$EXTERNAL"
  sudo mdutil -i off "$EXTERNAL" || echo "    （警告）外置卷关闭失败，请检查路径是否仍挂载。"
else
  echo "==> 未挂载 $EXTERNAL，跳过外置卷"
fi

echo ""
echo "==> 当前状态"
mdutil -s / || true
mdutil -s /System/Volumes/Data || true
mdutil -s "$HOME" 2>/dev/null || true
[[ -d "$EXTERNAL" ]] && mdutil -s "$EXTERNAL" || true

echo ""
echo "完成。"
echo "若 /System/Volumes/Data 仍为 -405：可重启后再执行本脚本；"
echo "并打开「系统设置 → Siri 与聚焦 → 聚焦隐私」确认未误排除整个内置磁盘。"
echo ""
echo "关于 corespotlightd 高占用：抹索引(-E)或反复开关后，等 10～30 分钟或重启后再观察；"
echo "若需长期减负：在「聚焦隐私」里排除大目录（如影片、整盘 node_modules、巨型素材库），"
echo "不要再用 mdutil -E / 与 mdutil -a -i off（会再次全盘关索引并加重后台）。"
