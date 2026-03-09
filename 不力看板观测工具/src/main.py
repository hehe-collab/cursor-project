"""
不力看板观测工具 - 主入口
登录 → 截图 → 发邮件，支持夜间跳过
"""
import os
import sys
from datetime import datetime
from pathlib import Path
from typing import Optional

from dotenv import load_dotenv

from .browser import login_and_screenshot
from .email_sender import send_screenshot_email


def _in_active_hours(config: dict) -> bool:
    """是否在允许执行的时间段内（排除夜间）"""
    schedule = config.get("schedule", {})
    start = schedule.get("active_start_hour", 7)
    end = schedule.get("active_end_hour", 22)
    hour = datetime.now().hour
    return start <= hour < end


def run(force: bool = False, debug: bool = False):
    config_dir = Path(__file__).parent.parent
    load_dotenv(config_dir / ".env")

    from .config_loader import load_config

    config = load_config(config_dir)

    # 夜间跳过（--force 可强制执行，用于测试）
    if not force and not _in_active_hours(config):
        print(f"[{datetime.now()}] 当前为夜间时段，跳过执行")
        return 0

    username = os.environ.get("ADMIN_USERNAME")
    password = os.environ.get("ADMIN_PASSWORD")
    qq_email = os.environ.get("QQ_EMAIL")
    qq_auth = os.environ.get("QQ_AUTH_CODE")
    recipient = os.environ.get("RECIPIENT_EMAIL")

    missing = []
    if not username:
        missing.append("ADMIN_USERNAME")
    if not password:
        missing.append("ADMIN_PASSWORD")
    if not qq_email:
        missing.append("QQ_EMAIL")
    if not qq_auth:
        missing.append("QQ_AUTH_CODE")
    if not recipient:
        missing.append("RECIPIENT_EMAIL")

    if missing:
        print(f"缺少环境变量: {', '.join(missing)}，请配置 .env")
        return 1

    output_dir = Path(__file__).parent.parent / "output"
    output_dir.mkdir(exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d_%H%M")
    screenshot_base = output_dir / f"kanban_{ts}"

    try:
        paths = login_and_screenshot(
            username=username,
            password=password,
            config=config,
            output_path=str(screenshot_base) + ".png",
            headless=not debug,
        )
        for p in paths:
            print(f"截图已保存: {p}")

        filters = config.get("filters", {})
        combinations = filters.get("combinations", [])
        countries = filters.get("countries", [])
        if combinations:
            label = f"{len(combinations)}组"
        else:
            label = "、".join(countries) if countries else ""

        send_screenshot_email(
            smtp_user=qq_email,
            smtp_password=qq_auth,
            recipient=recipient,
            screenshot_path=paths,
            subject=f"不力看板截图 {ts}" + (f"（{label}）" if label else ""),
            body=f"看板截图已生成，时间 {datetime.now().strftime('%Y-%m-%d %H:%M')}。",
        )
        print("邮件已发送")
        return 0

    except Exception as e:
        print(f"执行失败: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    if "--init-excel" in sys.argv:
        from .config_loader import create_excel_template
        config_dir = Path(__file__).parent.parent
        excel_path = config_dir / "配置.xlsx"
        create_excel_template(excel_path)
        print(f"已创建配置模板: {excel_path}")
        sys.exit(0)
    if "--schedule-info" in sys.argv or "--schedule" in sys.argv:
        config_dir = Path(__file__).parent.parent
        load_dotenv(config_dir / ".env")
        from .config_loader import load_config
        cfg = load_config(config_dir)
        s = cfg.get("schedule", {})
        start, end = s.get("active_start_hour", 8), s.get("active_end_hour", 24)
        interval = s.get("interval_minutes", 30)
        now = datetime.now()
        in_active = start <= now.hour < end
        print(f"定时配置: {start}:00-{end}:00 执行，每 {interval} 分钟")
        print(f"当前时段: {'允许执行' if in_active else '夜间跳过'}")
        print("启用定时: bash setup_launchd.sh --load")
        sys.exit(0)
    force = "--force" in sys.argv or "-f" in sys.argv
    debug = "--debug" in sys.argv or "-d" in sys.argv
    sys.exit(run(force=force, debug=debug))
