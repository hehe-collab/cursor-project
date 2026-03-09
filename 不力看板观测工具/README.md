# 不力看板观测工具

定时登录不力管理系统看板页，截图并发送到邮箱。

## 功能

- 自动登录（无验证码）
- 进入统计/看板页，整页截图
- 通过 QQ 邮箱 SMTP 发送到指定邮箱
- 每 30 分钟执行一次，**夜间（22:00–07:00）自动跳过**

## 快速开始

### 0. 浏览器（可选复用）

若项目中已有 `管理系统批量脚本工具/ad-automation2.0/pw-browsers`，`run.sh` 会自动复用，无需再安装。否则需运行 `bash setup_venv.sh` 安装 Chromium。

### 1. 安装依赖

```bash
cd "/Volumes/存钱罐/cursor项目/不力看板观测工具"
bash setup_venv.sh
```

### 2. 配置

```bash
cp .env.example .env
# 编辑 .env，填入：
# - ADMIN_USERNAME / ADMIN_PASSWORD（不力后台登录）
# - QQ_EMAIL / QQ_AUTH_CODE（QQ 邮箱 + 授权码）
# - RECIPIENT_EMAIL（收件邮箱）
```

### 3. 手动测试

```bash
bash run.sh --force
```

若成功，会在 `output/` 下生成截图，并收到邮件。登录失败时可加 `--debug` 查看浏览器过程：

```bash
bash run.sh --force --debug
```

### 4. 定时任务（可选）

```bash
bash setup_launchd.sh
launchctl load ~/Library/LaunchAgents/com.buli.kanban.monitor.plist
```

## 配置说明

### 配置方式

**推荐：Excel 配置**（更直观）

```bash
bash run.sh --init-excel   # 首次创建 配置.xlsx 模板
```

用 Excel/WPS 打开 `配置.xlsx`，在第二行填入：
- 推广ID、推广名称、副名称、账户
- 国家1、国家2、国家3（可填多个，留空不截）

**config.yaml**：保留 URL、选择器、时段等，筛选值以 Excel 为准

### 选择器调整

若登录或筛选失败，用浏览器开发者工具检查页面元素，更新 `config.yaml` 中对应选择器。

## 项目结构

```
不力看板观测工具/
├── config.yaml       # 页面 URL、选择器、时段
├── 配置.xlsx         # 筛选配置（推广名称、国家等，用 Excel 编辑）
├── .env              # 账号密码、邮箱（勿提交）
├── .env.example      # 配置模板
├── requirements.txt
├── run.sh            # 执行入口
├── setup_venv.sh     # 安装依赖
├── setup_launchd.sh  # 安装定时任务
├── output/           # 截图输出
└── src/
    ├── main.py       # 主流程
    ├── browser.py    # 登录、截图
    └── email_sender.py # 发邮件
```
