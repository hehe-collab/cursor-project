# TikTok 广告批量建设自动化脚本 3.0（Hooked Shorts）

> 基于 Playwright 的浏览器自动化脚本，用于在 **admin.hookedshorts.com** 批量创建 TikTok 广告任务。  
> 功能与参考工具 `管理系统批量脚本工具/ad-automation3.0` 一致，已针对 Hooked Shorts 管理后台 UI 完成适配并通过 E2E 验证。

## 目标地址

| 用途 | URL |
|------|-----|
| 批量工具页 | https://admin.hookedshorts.com/batch-tools |
| 广告任务页（批间轮询） | https://admin.hookedshorts.com/ad-task |
| 登录页 | https://admin.hookedshorts.com/login |

登录账号在 `config.js` 中配置（`username` / `password`）。首次运行可自动登录，之后 Cookie 保存在本地 `browser-data/`（已 gitignore，勿跨设备复制）。

## 快速开始

```bash
cd "/Volumes/存钱罐/cursor项目/未来管理系统批量脚本工具/ad-automation3.0"

# 安装依赖（Chromium 复用参考项目，勿 npm run setup）
npm install

# 生成 Excel 模板
npm run template

# 填写 data/tasks-v3.0.xlsx 后运行（有界面，便于首次确认）
npm start
```

或使用启动脚本：

```bash
./启动脚本.sh
```

## E2E 全链路测试（无头，推荐发版前跑一遍）

脚本会从生产 API 生成/复用 `data/tasks-e2e.xlsx`，自动登录并走完：Smart+2.0 → 主体 → 账户 → 项目 → 广告组 → 广告 → 提交任务。

```bash
cd "/Volumes/存钱罐/cursor项目/未来管理系统批量脚本工具/ad-automation3.0"

# 无头全自动（约 1 分钟）
E2E_AUTO=1 E2E_HEADLESS=1 node scripts/e2e-run.js

# 有界面观察（调试 UI 时用）
E2E_AUTO=1 E2E_HEADLESS=0 node scripts/e2e-run.js
```

辅助脚本：

| 命令 | 说明 |
|------|------|
| `node scripts/generate-e2e-excel.js` | 仅从 API 拉数据生成 `data/tasks-e2e.xlsx` |
| `node scripts/probe-batch-tools.js` | 快速探测登录 + 选主体/账户 |

**预期结果**：终端末尾出现 `成功: 1 个`，且各步骤日志含「项目设置完成」「广告组设置完成」「广告设置完成」「任务已提交」。

若报 `profile already in use`，关闭残留 Chromium 并删除锁文件：

```bash
pkill -f "Google Chrome for Testing" 2>/dev/null
rm -f browser-data/SingletonLock browser-data/SingletonCookie browser-data/SingletonSocket
```

## 执行流程（正式批量）

1. 脚本启动，浏览器打开并导航到 `/batch-tools`
2. 若未登录，使用 `config.js` 中的账号自动登录（可关闭 `advanced.autoLogin`）
3. 关闭弹窗；页顶确认 Smart+2.0 模式；按 **Enter** 开始（E2E 模式设 `E2E_AUTO=1` 可自动跳过）
4. 按 Excel 批量执行（默认 10 并发 + 批间轮询广告任务页）
5. 一轮结束后：输入 **`r`** 重读 Excel 再跑；输入 **`q`** 关闭浏览器退出

## Excel 字段

与参考工具 3.0 完全相同，详见参考目录 `README.md` 或运行 `npm run template` 后查看「字段说明」sheet。

**Hooked Shorts 注意（优化目标）**：见下方「优化目标：价值 vs 转化」专节。

## 与参考项目（dramahub8）的差异

| 项目 | 参考（dramahub8） | 本工具（Hooked Shorts） |
|------|-------------------|-------------------------|
| 批量工具 URL | `/advertiseTools/task` | `/batch-tools` |
| 广告任务 URL | `/advertiseTools/adTask` | `/ad-task` |
| Smart+2.0 | 页顶 el-switch；弹窗内可能有 Smart+ tab | 页顶默认 Smart+2.0；**项目/广告组/广告弹窗内无 Smart+ tab**（脚本已跳过） |
| 设置入口 | 按 Y 坐标点第 N 个「设置」 | 点击 `.tool-card` 卡片（项目 / 广告组 / 广告）内的「设置」 |
| 弹窗定位 | 部分用 `.el-dialog:visible` | 优先 `[role=dialog]` + 标题文案 |
| 主体/账户筛选 | Element Plus 1.x 风格 | Element Plus 2.x：placeholder 在 span；账户用 `input.el-select__input` |
| 广告表 UI | 推广链接列点 td | 链接列「未选择」、素材「已选 N 项」、标题「可多选标题包」 |
| 提交按钮 | 「提交」 | 「提交任务」（脚本两种均支持） |
| 优化目标 Excel | 「价值」= 后台「价值」 | 见下方专节（语义与 UI 分离） |
| Chromium | 本地 `pw-browsers/` | **复用**参考项目 `chromium-1208`（见下方） |

## 优化目标：价值 vs 转化

Excel「优化目标」列与 TikTok 投放目标**语义一一对应**，脚本分两层处理：

| Excel 填写 | TikTok 投放目标 | 出价校验规则 |
|------------|-----------------|--------------|
| **价值** | **ROAS**（价值优化 / 目标 ROAS） | 单出价 **≥ 1.1**（低于 1.1 自动抬到 1.1）；**无**转化上限 1.3 |
| **转化** | **转化**（转化量优化） | 单出价 **≤ 1.3**（超过会暂停提示） |

也支持 Excel 填「下单」，脚本会规范化为「转化」。

### 与 Hooked Shorts 后台 UI 的关系（重要）

管理后台广告组弹窗里的「优化目标」**下拉选项**，与 Excel 语义**不一定同名**：

- Excel 填 **「转化」** → UI 选 **「转化」**（一致）
- Excel 填 **「价值」** → 业务上是 **ROAS**，但当前 Hooked Shorts **尚未把「价值/ROAS」与 TikTok ROAS 打通**，UI 下拉可能还没有 ROAS 选项

因此在 ROAS 未打通前，脚本只做 **UI 层临时兜底**：在弹窗里选 `config.js` 中配置的 UI 文案（默认 `excelToUi.价值: '转化'`），**避免自动化卡在找不到「价值」选项**。

**出价校验、Excel 含义不变**：Excel 仍是「价值/ROAS」时，仍按 ROAS 规则（≥1.1，不按转化 1.3 上限卡）。

配置位置（`config.js`）：

```javascript
optimizationTarget: {
  excelToUi: {
    价值: '转化', // 临时 UI 兜底；后台 ROAS 打通后改为 UI 上真实 ROAS/价值选项文案
    转化: '转化',
  },
},
```

后台支持 ROAS 后，只需改 `excelToUi.价值`（例如改回 `'价值'` 或 `'ROAS'`），**不必改 Excel 列含义**。

日志示例（临时兜底时）：

```text
优化目标 UI: 转化（Excel 价值→TT ROAS；后台 ROAS 未打通，临时选 UI「转化」，出价仍按 价值/ROAS 规则）
```

## 浏览器与 Playwright 版本

- 锁定 **`playwright@1.58.2`**（与参考项目一致）
- **不要**在本目录执行 `npm run setup`（会下载 chromium-1223，与借用浏览器 revision 不匹配）
- `config.js` 中 `executablePath` 指向：

  `../../管理系统批量脚本工具/ad-automation3.0/pw-browsers/chromium-1208/.../Google Chrome for Testing`

  路径必须从 `ad-automation3.0/lib/` 向上**两级**到 `cursor项目` 再进入参考目录；少写一级会导致 `Executable doesn't exist`。

本地另有 `pw-browsers` 符号链接指向同一目录，仅供运行时备用；目录本身已在 `.gitignore` 中。

## 目录结构

```
ad-automation3.0/
├── config.js              # URL、登录、并发、浏览器路径
├── index.js               # 主入口
├── lib/
│   ├── automation.js      # 核心自动化（Hooked Shorts UI 适配）
│   ├── excel.js           # Excel 解析
│   ├── parallel.js        # 并行 + 批间轮询
│   └── utils.js           # 日志、E2E_AUTO 等
├── scripts/
│   ├── e2e-run.js         # E2E 全链路
│   ├── generate-e2e-excel.js
│   └── probe-batch-tools.js
├── data/
│   ├── tasks-v3.0.xlsx    # 正式任务 Excel
│   └── tasks-e2e.xlsx     # E2E 测试数据（可 regenerate）
├── browser-data/          # 登录态（gitignore）
├── pw-browsers/           # → 符号链接到参考项目（gitignore）
└── screenshots/           # 执行截图（gitignore）
```

## 常见问题

**Q: 还需要安装 Chromium 吗？**  
A: **不需要。** 复用参考项目已下载的 `chromium-1208` 即可。

**Q: 启动浏览器失败 / Executable doesn't exist？**  
A: ① 确认本目录 `playwright` 为 **1.58.2**（`npm install`）；② 检查 `config.js` 的 `executablePath` 相对路径是否为 `../../管理系统批量脚本工具/...`；③ 确认参考项目下存在 `pw-browsers/chromium-1208/`。

**Q: 自动登录失败？**  
A: 浏览器手动登录一次，或检查 `config.js` 账号；必要时删除 `browser-data/` 后重跑。

**Q: E2E 卡在选主体/账户？**  
A: 多为无头模式下 token 过期；删 `browser-data/` 让脚本重新自动登录，或用 `E2E_HEADLESS=0` 观察页面。

**Q: 批间轮询是什么？**  
A: 每批任务完成后，脚本打开 `/ad-task`，等待第一页无「进行中」任务后再开下一批，避免后台过载。可在 `config.js` 的 `parallel.pollAdTaskAfterBatch` 关闭。

**Q: 和参考脚本能否共用同一份 Excel？**  
A: 列结构相同，可直接复制。「优化目标」列含义也与参考工具一致（价值=ROAS，转化=转化）；Hooked Shorts 仅在 UI 下拉未支持 ROAS 时有临时兜底，见上文专节。

## 参考

- 参考实现：`/Volumes/存钱罐/cursor项目/管理系统批量脚本工具/ad-automation3.0`
- 参考 README 中的 Excel 字段说明与业务规则
