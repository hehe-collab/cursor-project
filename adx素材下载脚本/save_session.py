#!/usr/bin/env python3
"""
在已安装的 Chromium 中打开 DataEye AdXray，由你手动完成登录与验证码。
关闭浏览器窗口后，会话会写入 storage_state.json，供 download.py 复用。
"""
from pathlib import Path

from playwright.sync_api import sync_playwright

STORAGE = Path(__file__).resolve().parent / "storage_state.json"
START_URL = "https://adxray.dataeye.com/"


def main() -> None:
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        context = browser.new_context()
        page = context.new_page()
        page.goto(START_URL, wait_until="domcontentloaded")
        print("请在浏览器中完成登录（含验证码）。")
        input("登录成功后回到此终端，按回车保存会话并退出浏览器… ")
        context.storage_state(path=str(STORAGE))
        browser.close()
    print(f"已保存会话: {STORAGE}")


if __name__ == "__main__":
    main()
