/**
 * 配置文件 - Hooked Shorts 管理后台（admin.hookedshorts.com）
 */
module.exports = {
  // ===== 网站地址 =====
  baseUrl: 'https://admin.hookedshorts.com',
  taskUrl: 'https://admin.hookedshorts.com/batch-tools',
  // 批间轮询：广告任务页（第一页 0 个「进行中」再开下一批）
  adTaskUrl: 'https://admin.hookedshorts.com/ad-task',
  loginUrl: 'https://admin.hookedshorts.com/login',

  // ===== 登录信息（首次可自动登录，之后靠 browser-data 持久化）=====
  username: 'admin',
  password: 'admin789',

  // ===== 页面路径片段（用于 URL 校验）=====
  paths: {
    task: '/batch-tools',
    adTask: '/ad-task',
    login: '/login',
  },

  // ===== Excel 数据文件路径 =====
  excelPath: './data/tasks-v3.0.xlsx',

  // ===== 浏览器配置 =====
  browser: {
    headless: false,
    profileDir: './browser-data',
    // 复用参考项目已下载的 Chromium（勿 npm run setup）
    // 从本目录 ad-automation3.0 向上两级到 cursor项目，再进参考工具目录
    browsersPath: '../../管理系统批量脚本工具/ad-automation3.0/pw-browsers',
    executablePath: '../../管理系统批量脚本工具/ad-automation3.0/pw-browsers/chromium-1208/chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing',
    slowMo: 0,
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
    pollAdTaskAfterBatch: true,
    pollInitialDelay: 60000,
    pollInterval: 15000,
    pollMaxWait: 0,
  },

  // ===== 高级配置 =====
  advanced: {
    useIntelligentWait: true,
    skipNetworkIdle: true,
    verboseLog: false,
    autoLogin: true,
  },

  // ===== 验证配置 =====
  validation: {
    enableScreenshot: true,
    enableValidationLog: true,
    screenshotDir: './screenshots',
  },
};
