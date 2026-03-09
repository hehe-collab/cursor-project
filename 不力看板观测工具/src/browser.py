"""
浏览器自动化：登录、导航、筛选、截图
"""
import random
from pathlib import Path
from typing import List, Optional

from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeout


def _first_selector(page, selector_str: str):
    """从配置的多个选择器中取第一个存在的"""
    for sel in (s.strip() for s in selector_str.split(",") if s.strip()):
        try:
            if page.locator(sel).count() > 0:
                return sel
        except Exception:
            continue
    return None


def _fill_for_vue(locator, value: str):
    """针对 Vue/Element UI：设置 value 并派发 InputEvent（含 data 以兼容 Vue）"""
    locator.evaluate(
        """(el, val) => {
            el.focus();
            el.value = val;
            el.dispatchEvent(new InputEvent('input', { bubbles: true, data: val, inputType: 'insertText' }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
        }""",
        value,
    )


def _fill_by_typing(page, acc_locator, pwd_locator, username: str, password: str):
    """模拟真实打字，最兼容 Vue/Element UI"""
    acc_locator.click()
    page.wait_for_timeout(200)
    acc_locator.press("Meta+a")  # 全选（Mac 用 Cmd）
    acc_locator.press_sequentially(username, delay=80)
    page.wait_for_timeout(300)
    pwd_locator.click()
    page.wait_for_timeout(200)
    pwd_locator.press("Meta+a")
    pwd_locator.press_sequentially(password, delay=80)


def _fill_filter_input(page, selector: str, value: str):
    """填入筛选输入框（Vue 兼容）"""
    if not value:
        return
    try:
        loc = page.locator(selector).first
        if loc.count() > 0:
            loc.click()
            page.wait_for_timeout(150)
            loc.press("Meta+a")
            loc.press_sequentially(value, delay=60)
    except Exception:
        pass


def _clear_filter_input(page, selector: str):
    """清空筛选输入框（用于剧名=不限定时清除上一组合的残留）"""
    try:
        loc = page.locator(selector).first
        if loc.count() > 0:
            loc.click()
            page.wait_for_timeout(100)
            loc.press("Meta+a")
            loc.press("Backspace")
            page.wait_for_timeout(100)
    except Exception:
        pass


def _select_country(page, country_sel: str, country: str):
    """选择国家下拉（Element UI el-select）"""
    try:
        sel = _first_selector(page, country_sel) or country_sel
        page.locator(sel).first.click()
        page.wait_for_timeout(600)
        # 点击包含该国家名的选项（下拉在 body 下）
        page.locator(f".el-select-dropdown__item:has-text('{country}')").first.click(timeout=5000)
        page.wait_for_timeout(400)
    except Exception:
        pass


def _fill_combo(page, combo: dict, filter_selectors: dict, country_sel: str, click_reset: bool = False):
    """根据组合填入筛选：推广ID、推广名称、剧名（空则不填）、账户、国家"""
    reset_sel = filter_selectors.get("重置", "button:has-text('重置')")
    if click_reset:
        try:
            if page.locator(reset_sel).count() > 0:
                page.locator(reset_sel).first.click()
                page.wait_for_timeout(800)
        except Exception:
            pass

    key_to_sel = {"推广ID": "推广ID", "推广名称": "推广名称", "剧名": "剧名", "账户": "账户"}
    for key, sel_key in key_to_sel.items():
        val = combo.get(key)
        sel = filter_selectors.get(sel_key)
        if key == "剧名" and (val is None or (isinstance(val, str) and not val.strip())):
            if sel:
                for s in (x.strip() for x in str(sel).split(",") if x.strip()):
                    try:
                        if page.locator(s).count() > 0:
                            _clear_filter_input(page, s)
                            break
                    except Exception:
                        pass
            continue
        if val is None or (isinstance(val, str) and not val.strip()):
            continue
        if sel and val:
            for s in (x.strip() for x in str(sel).split(",") if x.strip()):
                try:
                    if page.locator(s).count() > 0:
                        _fill_filter_input(page, s, str(val))
                        break
                except Exception:
                    pass
    country = combo.get("国家")
    if country:
        _select_country(page, country_sel, country)


def login_and_screenshot(
    username: str,
    password: str,
    config: dict,
    output_path: str,
    headless: bool = True,
) -> List[str]:
    """
    登录不力管理系统，进入看板页，截图并保存。
    支持多国家：每个国家单独截图。返回截图文件路径列表。
    """
    base_url = config.get("base_url", "")
    login_url = config.get("login_url", base_url)
    dashboard_url = config.get("dashboard_url", "")
    login_cfg = config.get("login", {})
    dashboard_cfg = config.get("dashboard", {})
    filters_cfg = config.get("filters", {})
    screenshot_cfg = config.get("screenshot", {})

    full_page = screenshot_cfg.get("full_page", True)
    timeout_ms = screenshot_cfg.get("timeout_ms", 10000)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=headless)
        context = browser.new_context(
            viewport={"width": 1920, "height": 1080},
            user_agent="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
        )
        page = context.new_page()

        try:
            # 1. 直接访问登录页（确保在正确页面）
            login_page_url = login_url.rstrip("/")
            if "/login" not in login_page_url:
                login_page_url = f"{base_url.rstrip('/')}/login"
            login_page_url = f"{login_page_url}?redirect=%2Fstatistics"
            page.goto(login_page_url, wait_until="networkidle", timeout=30000)

            # 2. 等待 Element UI 表单渲染（Vue 异步）
            page.wait_for_load_state("domcontentloaded")
            page.wait_for_selector("input.el-input__inner", timeout=10000)
            page.wait_for_timeout(2000)  # Vue 挂载需要时间

            # 3. 登录：Vue/Element UI 不响应 fill()，需用 evaluate 派发 input 事件
            user_sel = "input.el-input__inner[placeholder='账号']"
            pass_sel = "input.el-input__inner[type='password']"
            # 备选：账号和密码可能是前两个 el-input__inner
            user_sel_alt = "input.el-input__inner"
            submit_sel = "button:has-text('登录')"

            def _click_login():
                """多种方式尝试点击登录按钮（Element UI 可能需 force 或 JS click）"""
                btn_selectors = [
                    "button:has-text('登录')",
                    ".el-button:has-text('登录')",
                    "button.el-button--primary",
                ]
                for sel in btn_selectors:
                    try:
                        if page.locator(sel).count() > 0:
                            page.locator(sel).first.click(force=True, timeout=3000)
                            return True
                    except Exception:
                        continue
                try:
                    page.get_by_role("button", name="登录").click(force=True)
                    return True
                except Exception:
                    pass
                # 用 JS 直接触发点击（绕过可能的事件拦截）
                try:
                    clicked = page.evaluate("""() => {
                        const btn = document.querySelector('button.el-button--primary')
                            || Array.from(document.querySelectorAll('button')).find(b => b.textContent.includes('登录'));
                        if (btn) { btn.click(); return true; }
                        return false;
                    }""")
                    return bool(clicked)
                except Exception:
                    pass
                return False

            # 优先用模拟打字（最兼容 Vue），evaluate 为备选
            logged_in = False
            has_direct = page.locator(user_sel).count() > 0 and page.locator(pass_sel).count() > 0
            has_alt = page.locator(user_sel_alt).count() >= 2

            if has_direct:
                acc_loc = page.locator(user_sel).first
                pwd_loc = page.locator(pass_sel).first
                _fill_by_typing(page, acc_loc, pwd_loc, username, password)
            elif has_alt:
                acc_loc = page.locator(user_sel_alt).nth(0)
                pwd_loc = page.locator(user_sel_alt).nth(1)
                _fill_by_typing(page, acc_loc, pwd_loc, username, password)
            else:
                acc_loc = page.locator("input.el-input__inner").first
                pwd_loc = page.locator("input[type='password']").first
                _fill_for_vue(acc_loc, username)
                page.wait_for_timeout(300)
                _fill_for_vue(pwd_loc, password)

            page.wait_for_timeout(800)  # 等待 Vue 更新

            # 点击登录并等待导航（登录成功会跳转）
            try:
                with page.expect_navigation(timeout=25000):
                    _click_login()
            except Exception:
                pass
            logged_in = "/login" not in page.url

            # 若仍在登录页，用 evaluate 重试一次（可能 Vue 未收到 typing 事件）
            if not logged_in and has_direct:
                page.wait_for_timeout(1000)
                _fill_for_vue(page.locator(user_sel).first, username)
                page.wait_for_timeout(400)
                _fill_for_vue(page.locator(pass_sel).first, password)
                page.wait_for_timeout(800)
                try:
                    with page.expect_navigation(timeout=20000):
                        _click_login()
                except Exception:
                    pass
                logged_in = "/login" not in page.url

            # 4. 进入看板页（若已通过登录重定向到达则跳过）
            if "/statistics" not in page.url:
                page.goto(dashboard_url, wait_until="networkidle", timeout=30000)

            # 4. 切换到「看板」标签（若存在）
            kanban_tab = dashboard_cfg.get("kanban_tab_selector", "")
            if kanban_tab:
                try:
                    tab_sel = _first_selector(page, kanban_tab)
                    if tab_sel:
                        page.click(tab_sel, timeout=5000)
                        page.wait_for_load_state("networkidle", timeout=5000)
                except Exception:
                    pass  # 可能已在看板或选择器不对

            # 5. 应用筛选并截图（支持笛卡尔积组合 或 旧版多国家）
            output_dir = Path(output_path).parent
            output_dir.mkdir(parents=True, exist_ok=True)
            base_name = Path(output_path).stem
            ext = Path(output_path).suffix or ".png"

            filter_selectors = filters_cfg.get("selectors", {})
            search_sel = filter_selectors.get("搜索", "button:has-text('搜索')")
            country_sel = filter_selectors.get("国家", ".el-select")

            combinations = filters_cfg.get("combinations", [])
            if combinations:
                # 笛卡尔积模式：每个组合单独填筛选、截图
                anti = config.get("anti_rate_limit", {})
                delay_range = anti.get("delay_between_combos") or [0, 0]
                wait_ms = anti.get("wait_after_search_ms", 2500)
                click_reset = anti.get("click_reset_before_combo", False)

                screenshot_paths = []
                for i, combo in enumerate(combinations):
                    if i > 0 and delay_range and (delay_range[0] > 0 or delay_range[1] > 0):
                        sec = random.uniform(delay_range[0], delay_range[1])
                        page.wait_for_timeout(int(sec * 1000))

                    _fill_combo(page, combo, filter_selectors, country_sel, click_reset=click_reset)
                    try:
                        if page.locator(search_sel).count() > 0:
                            page.locator(search_sel).first.click()
                            page.wait_for_load_state("networkidle", timeout=8000)
                    except Exception:
                        pass
                    page.wait_for_timeout(wait_ms)
                    # 截图文件名：推广名_剧名_国家
                    parts = [combo.get("推广名称", ""), combo.get("剧名") or "不限", combo.get("国家", "")]
                    safe_name = "_".join(str(p).replace("/", "-") for p in parts if p)
                    out_path = output_dir / f"{base_name}_{i:02d}_{safe_name}{ext}"
                    page.screenshot(path=str(out_path), full_page=full_page, timeout=timeout_ms)
                    screenshot_paths.append(str(out_path))
                return screenshot_paths

            # 兼容旧版：values + countries
            filter_values = filters_cfg.get("values", {})
            countries = filters_cfg.get("countries", [])
            if not countries:
                countries = [None]

            anti = config.get("anti_rate_limit", {})
            delay_range = anti.get("delay_between_combos") or [0, 0]
            wait_ms = anti.get("wait_after_search_ms", 2500)

            screenshot_paths = []
            for idx, country in enumerate(countries):
                if idx > 0 and delay_range and (delay_range[0] > 0 or delay_range[1] > 0):
                    sec = random.uniform(delay_range[0], delay_range[1])
                    page.wait_for_timeout(int(sec * 1000))
                for key, val in filter_values.items():
                    if key in filter_selectors and val:
                        sel = filter_selectors[key]
                        for s in (x.strip() for x in str(sel).split(",") if x.strip()):
                            try:
                                if page.locator(s).count() > 0:
                                    _fill_filter_input(page, s, str(val))
                                    break
                            except Exception:
                                pass
                if country:
                    _select_country(page, country_sel, country)
                try:
                    if page.locator(search_sel).count() > 0:
                        page.locator(search_sel).first.click()
                        page.wait_for_load_state("networkidle", timeout=8000)
                except Exception:
                    pass
                page.wait_for_timeout(wait_ms)
                if country:
                    out_path = output_dir / f"{base_name}_{country}{ext}"
                else:
                    out_path = Path(output_path)
                page.screenshot(path=str(out_path), full_page=full_page, timeout=timeout_ms)
                screenshot_paths.append(str(out_path))
            return screenshot_paths

        except PlaywrightTimeout as e:
            raise RuntimeError(f"页面加载超时: {e}")
        except Exception as e:
            raise RuntimeError(f"截图失败: {e}")
        finally:
            browser.close()


if __name__ == "__main__":
    import os
    from dotenv import load_dotenv
    import yaml

    load_dotenv()
    with open(Path(__file__).parent.parent / "config.yaml", encoding="utf-8") as f:
        cfg = yaml.safe_load(f)

    out = Path(__file__).parent.parent / "output" / "kanban_test.png"
    login_and_screenshot(
        username=os.environ["ADMIN_USERNAME"],
        password=os.environ["ADMIN_PASSWORD"],
        config=cfg,
        output_path=str(out),
    )
    print(f"截图已保存: {out}")
