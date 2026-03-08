/**
 * Electron 主进程 - DramaBagus 批量广告工具
 * 打包为 Windows 可安装程序
 */
const { app, BrowserWindow, dialog, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');

// 必须在任何 require config 之前设置
const appData = app.getPath('userData');
global.__electronAppPaths = {
  appData,
  exeDir: path.dirname(app.getPath('exe')),
  resourcesDir: process.resourcesPath,
};

// 确保目录存在
const dirs = [
  appData,
  path.join(appData, 'data'),
  path.join(appData, 'browser-data'),
  path.join(appData, 'pw-browsers'),
  path.join(appData, 'screenshots'),
];
dirs.forEach(d => {
  if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true });
});

// 首次运行：复制 Excel 模板到 data 目录
const excelPath = path.join(appData, 'data', 'tasks-dramabagus.xlsx');
if (!fs.existsSync(excelPath)) {
  const templateSource = path.join(__dirname, 'data', 'tasks-dramabagus.xlsx');
  if (fs.existsSync(templateSource)) {
    fs.copyFileSync(templateSource, excelPath);
  } else {
    // 从 create-template 逻辑生成
    try {
      const XLSX = require('xlsx');
      const config = require('./config');
      const headers = ['任务','主体','账户ID','Pixel','项目名称','推广链接关键词','素材关键词','素材ID','标题','优化目标','出价策略','出价','预算','开始日期','开始时间','年龄','认证身份','商品库','商品','启用','提交次数'];
      const wb = XLSX.utils.book_new();
      const ws = XLSX.utils.json_to_sheet([], { header: headers });
      XLSX.utils.book_append_sheet(wb, ws, '任务列表');
      XLSX.writeFile(wb, excelPath);
    } catch (e) {
      console.error('生成 Excel 模板失败:', e);
    }
  }
}

let mainWindow = null;
let pauseResolve = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 900,
    height: 700,
    minWidth: 600,
    minHeight: 400,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'electron-preload.js'),
    },
    icon: fs.existsSync(path.join(__dirname, 'icon.ico')) ? path.join(__dirname, 'icon.ico') : undefined,
  });

  mainWindow.loadFile('electron-ui.html');
  mainWindow.on('closed', () => { mainWindow = null; });
}

app.whenReady().then(() => {
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

// IPC: 开始运行
ipcMain.handle('start-automation', async () => {
  const { setPauseHandler, setLogHandler } = require('./lib/utils');

  setPauseHandler((message) => {
    return new Promise((resolve) => {
      pauseResolve = resolve;
      mainWindow?.webContents.send('show-pause', message);
    });
  });

  setLogHandler((line) => {
    mainWindow?.webContents.send('log', line);
  });

  // 重定向 console.log 到 UI
  const originalLog = console.log;
  console.log = (...args) => {
    const line = args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
    mainWindow?.webContents.send('log', line);
    originalLog.apply(console, args);
  };

  try {
    const main = require('./index-electron.js');
    await main.run();
  } catch (err) {
    mainWindow?.webContents.send('log', `❌ 致命错误: ${err.message}`);
    console.error(err);
  }
});

// IPC: 用户点击继续
ipcMain.on('pause-continue', () => {
  if (pauseResolve) {
    pauseResolve();
    pauseResolve = null;
  }
});

// IPC: 用户点击退出
ipcMain.on('pause-quit', () => {
  if (pauseResolve) {
    pauseResolve();
    pauseResolve = null;
  }
  app.quit();
});

// IPC: 打开 Excel 所在目录
ipcMain.handle('open-data-folder', () => {
  const { shell } = require('electron');
  shell.openPath(path.join(appData, 'data'));
});

// IPC: 打开配置目录（可在此创建 config.json 覆盖账号密码）
ipcMain.handle('open-config-folder', () => {
  const { shell } = require('electron');
  shell.openPath(appData);
});
