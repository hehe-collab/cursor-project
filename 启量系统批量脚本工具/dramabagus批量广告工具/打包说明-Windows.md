# DramaBagus 批量广告工具 - 打包说明

## 一、打包步骤

### 1. 安装依赖

```bash
npm install
npm run setup
```

### 2. 打包 Windows 安装程序

```bash
npm run build:win
```

打包完成后：`dist/DramaBagus批量广告工具 Setup 1.0.0.exe`

### 3. 打包 Mac 安装程序

```bash
npm run build:mac
```

打包完成后：`dist/DramaBagus批量广告工具-1.0.0-universal.dmg`（支持 Intel 和 Apple Silicon）

### 4. 仅生成未打包目录（用于测试）

```bash
npm run pack:win   # Windows
npm run pack:mac  # Mac
```

---

## 二、安装包使用说明（给最终用户）

### Windows

1. **复制** `DramaBagus批量广告工具 Setup 1.0.0.exe` 到目标 Windows 电脑
2. **双击安装**，选择安装目录
3. **首次运行**：会自动创建数据目录、下载 Chromium（需联网）
4. **填写 Excel**：点击「打开数据目录」→ 编辑 `tasks-dramabagus.xlsx`
5. **点击「开始运行」** 启动自动化

### Mac

1. **复制** `DramaBagus批量广告工具-1.0.0-universal.dmg` 到目标 Mac
2. **双击打开 DMG**，将应用拖入「应用程序」文件夹
3. **首次打开**：若提示「无法打开，因为无法验证开发者」，请右键应用 → 选择「打开」
4. **首次运行**：会自动创建数据目录、下载 Chromium（需联网）
5. **填写 Excel**：点击「打开数据目录」→ 编辑 `tasks-dramabagus.xlsx`
6. **点击「开始运行」** 启动自动化

---

## 三、目录结构（打包后）

- **Windows 安装目录**：用户选择的路径，如 `C:\Program Files\DramaBagus批量广告工具\`
- **Mac 安装**：拖入「应用程序」后位于 `/Applications/DramaBagus批量广告工具.app`
- **用户数据**（Windows：`%APPDATA%\dramabagus-batch-ad-tool\`，Mac：`~/Library/Application Support/dramabagus-batch-ad-tool/`）：
  - `data/` - Excel 任务文件
  - `browser-data/` - 浏览器登录状态
  - `pw-browsers/` - Chromium（首次运行自动下载）
  - `screenshots/` - 截图

---

## 四、注意事项

1. **首次运行需联网**：用于下载 Chromium 浏览器
2. **登录状态**：每台电脑需单独登录一次，登录信息保存在该电脑的 `browser-data`
3. **配置修改**：安装目录内的 `config.js` 可修改账号密码等（需用记事本编辑）
4. **建议在 Windows 上打包**：若在 Mac 上打包，Chromium 需首次运行时下载；在 Windows 上打包可预先包含 Chromium（体积更大）

---

## 五、本地测试（带界面）

```bash
npm run electron
```

会启动 Electron 窗口，与打包后的界面一致。需先执行 `npm install` 和 `npm run setup`。
