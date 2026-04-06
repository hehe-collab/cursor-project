/**
 * 配置文件 - 登录信息、网站URL、延时参数等
 * DramaBagus 批量广告工具
 * 支持 Electron 打包：当 global.__electronAppPaths 存在时使用应用数据目录
 */
const path = require('path');

const baseConfig = {
  // ===== 网站地址（DramaBagus）=====
  baseUrl: 'https://admin.dramabagus.com',
  taskUrl: 'https://admin.dramabagus.com/tools/batch',

  // ===== 登录信息 =====
  username: 'qiliang',
  password: 'xiaodong',

  // ===== Excel 数据文件路径 =====
  excelPath: './data/tasks-dramabagus.xlsx',

  // ===== 浏览器配置 =====
  browser: {
    headless: false,               // 是否无头模式（false = 显示浏览器窗口）
    profileDir: './browser-data',  // 浏览器用户数据目录（保存登录状态）
    browsersPath: './pw-browsers', // Playwright 浏览器存放路径（项目目录内）
    slowMo: 0,                     // 全局慢动作（毫秒），调试时可设为 50-100
  },

  // ===== 延时配置（毫秒）=====
  delay: {
    short: 100,
    medium: 200,
    long: 400,
  },

  // ===== 重试配置 =====
  retry: {
    enabled: true,
    maxAttempts: 3,
    retryDelay: 1000,
  },

  // ===== 并发配置 =====
  parallel: {
    enabled: true,
    maxConcurrent: 10,
    batchDelay: 2000,
    // 第一版取消 URL 轮询，后续如需再加
    pollAdTaskAfterBatch: false,
    pollInitialDelay: 60000,
    pollInterval: 15000,
    pollMaxWait: 0,
  },

  /**
   * 多次提交（submitCount≥2）时，最后一次点击「提交」之后到结束本任务（并行模式会立刻关标签页）之间的缓冲。
   * - finalSettleMs：固定额外等待，保证请求有时间落地（默认可维护、可调）
   * - waitForSuccessToast：是否先尝试等待 Element Plus 成功提示（超时则忽略，仍执行 finalSettleMs）
   */
  submit: {
    finalSettleMs: 3000,
    waitForSuccessToast: true,
    successToastTimeoutMs: 8000,
  },

  // ===== 高级配置 =====
  advanced: {
    useIntelligentWait: true,
    skipNetworkIdle: true,
    verboseLog: false,
  },

  // ===== 验证配置 =====
  validation: {
    enableScreenshot: true,
    enableValidationLog: true,
    screenshotDir: './screenshots',
  },
};

// Electron 打包时使用用户数据目录（可写）
let config = baseConfig;
if (global.__electronAppPaths) {
  const { appData } = global.__electronAppPaths;
  config = {
    ...baseConfig,
    excelPath: path.join(appData, 'data', 'tasks-dramabagus.xlsx'),
    browser: {
      ...baseConfig.browser,
      profileDir: path.join(appData, 'browser-data'),
      browsersPath: path.join(appData, 'pw-browsers'),
    },
    validation: {
      ...baseConfig.validation,
      screenshotDir: path.join(appData, 'screenshots'),
    },
  };
  // 用户可在 appData/config.json 覆盖账号密码（示例：{"username":"xxx","password":"yyy"}）
  const userConfigPath = path.join(appData, 'config.json');
  if (require('fs').existsSync(userConfigPath)) {
    try {
      const userConfig = JSON.parse(require('fs').readFileSync(userConfigPath, 'utf8'));
      if (userConfig.username != null) config.username = userConfig.username;
      if (userConfig.password != null) config.password = userConfig.password;
    } catch {}
  }
}

module.exports = config;
