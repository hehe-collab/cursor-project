#!/usr/bin/env python3
"""
使用 save_session.py 保存的会话访问 AdXray，执行配置中的下载任务。

具体「点哪里、下什么」依赖站内页面结构；你提供列表页/详情页 URL 与要下的文件类型后，
可在此补充 locator 与 download 事件处理。
"""
from pathlib import Path

import yaml
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent
CONFIG_CANDIDATES = (ROOT / "config.yaml", ROOT / "config.example.yaml")


def load_config():
    for p in CONFIG_CANDIDATES:
        if p.is_file():
            with open(p, encoding="utf-8") as f:
                return yaml.safe_load(f)
    raise FileNotFoundError("请复制 config.example.yaml 为 config.yaml 并填写 storage_state")


def main() -> None:
    cfg = load_config()
    storage = Path(cfg["storage_state"])
    if not storage.is_file():
        raise FileNotFoundError(
            f"未找到会话文件: {storage}\n请先运行: python save_session.py"
        )
    start_url = cfg.get("start_url", "https://adxray.dataeye.com/")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(storage_state=str(storage))
        page = context.new_page()
        page.goto(start_url, wait_until="domcontentloaded")
        # 若被重定向到登录页，说明会话过期，需重新 save_session
        if "login" in page.url.lower():
            print("会话可能已失效，请重新运行 save_session.py 登录。")
        else:
            print("会话有效，当前 URL:", page.url)
        # TODO: 根据 config["downloads"] 遍历页面并触发下载
        print("下载逻辑待按实际页面补充；当前仅校验会话。")
        browser.close()


if __name__ == "__main__":
    main()
