# AdXray 素材下载脚本（会话复用）

登录地址：<https://adxray.dataeye.com/>

## 验证码与登录

本站登录页的图形验证码用于区分真人，**不提供**自动识别验证码并代你登录的实现；请在本机浏览器中手动完成登录。

推荐流程：运行 `save_session.py`，在弹出的窗口里输入账号、密码、验证码并勾选协议后登录；回到终端按回车，会生成 `storage_state.json`，后续下载脚本复用该会话。

## 环境

```bash
cd adx素材下载脚本
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
playwright install chromium
```

## 使用

1. 保存会话：`python save_session.py`
2. 复制配置：`cp config.example.yaml config.yaml`（按需改路径）
3. 校验并扩展下载：`python download.py`（当前仅校验会话；具体下载步骤需按站内页面再写）

## 说明

- `storage_state.json` 含登录态，勿提交到公开仓库。
- 会话过期时需重新执行 `save_session.py`。
