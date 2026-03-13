# API 中转配置指南

## 第一步：搜索并选择平台

1. 打开 **百度** 或 **Google**
2. 搜索：`OpenAI API 中转` 或 `ChatGPT API 国内`
3. 打开几个平台官网，对比价格和评价

常见平台（自行核实）：老张 AI、API2D、OpenRouter 等

---

## 第二步：注册并充值

1. 打开平台官网 → 点击 **注册**
2. 用手机号或邮箱注册
3. 登录 → 找到 **充值** / **余额**
4. 选择 **支付宝** 或 **微信**，一般 100 元起充
5. 完成支付

---

## 第三步：获取 API Key 和 Base URL

1. 登录后在后台找 **API Key** / **密钥管理**
2. 点击 **创建 Key** → 复制生成的 Key（`sk-xxxxx`）
3. 在 **文档** 或 **设置** 里找到 **API 地址** / **Base URL**
   - 格式通常为：`https://api.xxx.com/v1` 或 `https://openai.xxx.com/v1`

---

## 第四步：配置素材制作工具

1. 复制配置模板：
   ```bash
   cp config.example.yaml config.yaml
   ```

2. 用记事本或 Cursor 打开 `config.yaml`

3. 修改 `openai` 部分：
   ```yaml
   openai:
     api_key: "你从中转平台复制的Key"
     base_url: "https://中转平台提供的地址/v1"
     model: "gpt-4o"
   ```

4. 保存文件

---

## 第五步：运行测试

```bash
cd /Volumes/存钱罐/cursor项目/素材制作工具
source venv/bin/activate
python src/analyze.py -i 短剧文件夹 -o analysis.json -n 2
```

---

## 常见问题

**Q：base_url 填什么？**  
A：从中转平台文档里找，一般是 `https://api.xxx.com/v1` 这种格式。

**Q：直连 OpenAI 怎么配？**  
A：只填 `api_key`，`base_url` 留空或删掉即可。

**Q：模型名要改吗？**  
A：大部分中转支持 `gpt-4o` 和 `gpt-4o-mini`，具体看平台文档。当前默认用 gpt-4o 提升分析质量。
