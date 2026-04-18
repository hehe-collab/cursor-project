/**
 * 配置文件 - 登录信息、网站URL、延时参数等
 * DramaBagus 批量广告工具
 *
 * 账号密码请勿提交仓库：使用环境变量或复制 config.local.example.js 为 config.local.js 填写。
 * 支持 Electron：global.__electronAppPaths 存在时使用应用数据目录；仍可读 appData/config.json 覆盖账号。
 */
const path = require('path');
const fs = require('fs');

/**
 * @param {Record<string, unknown>} target
 */
function loadCredentialOverrides(target) {
  const localPath = path.join(__dirname, 'config.local.js');
  if (fs.existsSync(localPath)) {
    try {
      const local = require(localPath);
      if (local.username != null) target.username = local.username;
      if (local.password != null) target.password = local.password;
      if (local.adTaskUrl != null) target.adTaskUrl = local.adTaskUrl;
    } catch (e) {
      console.warn('[config] 读取 config.local.js 失败:', e.message);
    }
  }
  if (process.env.DRAMA_BAGUS_USERNAME) target.username = process.env.DRAMA_BAGUS_USERNAME;
  if (process.env.DRAMA_BAGUS_PASSWORD) target.password = process.env.DRAMA_BAGUS_PASSWORD;
  if (process.env.DRAMA_BAGUS_AD_TASK_URL) target.adTaskUrl = process.env.DRAMA_BAGUS_AD_TASK_URL;
}

const baseConfig = {
  // ===== 网站地址（DramaBagus）=====
  baseUrl: 'https://admin.dramabagus.com',
  taskUrl: 'https://admin.dramabagus.com/tools/batch',

  /**
   * 广告任务列表页（用于 parallel.pollAdTaskAfterBatch 批间轮询「进行中」）。
   * 若后台路径不同，请在 config.local.js 或环境变量 DRAMA_BAGUS_AD_TASK_URL 中覆盖。
   */
  adTaskUrl: 'https://admin.dramabagus.com/advertiseTools/adTask',

  // ===== 登录信息（请用 config.local.js 或 DRAMA_BAGUS_USERNAME / DRAMA_BAGUS_PASSWORD）=====
  username: '',
  password: '',

  // ===== Excel 数据文件路径 =====
  excelPath: './data/tasks-dramabagus.xlsx',

  // ===== 浏览器配置 =====
  browser: {
    headless: false,
    profileDir: './browser-data',
    browsersPath: './pw-browsers',
    slowMo: 0,
  },

  delay: {
    short: 100,
    medium: 200,
    long: 400,
  },

  retry: {
    enabled: true,
    maxAttempts: 3,
    retryDelay: 1000,
  },

  parallel: {
    enabled: true,
    maxConcurrent: 10,
    batchDelay: 2000,
    pollAdTaskAfterBatch: false,
    pollInitialDelay: 60000,
    pollInterval: 15000,
    pollMaxWait: 0,
  },

  submit: {
    finalSettleMs: 3000,
    waitForSuccessToast: true,
    successToastTimeoutMs: 8000,
  },

  advanced: {
    useIntelligentWait: true,
    skipNetworkIdle: true,
    verboseLog: false,
  },

  validation: {
    enableValidationLog: true,
  },
};

/** @type {typeof baseConfig & Record<string, unknown>} */
let config = { ...baseConfig };
loadCredentialOverrides(config);

if (global.__electronAppPaths) {
  const { appData } = global.__electronAppPaths;
  config = {
    ...config,
    excelPath: path.join(appData, 'data', 'tasks-dramabagus.xlsx'),
    browser: {
      ...config.browser,
      profileDir: path.join(appData, 'browser-data'),
      browsersPath: path.join(appData, 'pw-browsers'),
    },
  };
  loadCredentialOverrides(config);
  const userConfigPath = path.join(appData, 'config.json');
  if (fs.existsSync(userConfigPath)) {
    try {
      const userConfig = JSON.parse(fs.readFileSync(userConfigPath, 'utf8'));
      if (userConfig.username != null) config.username = userConfig.username;
      if (userConfig.password != null) config.password = userConfig.password;
    } catch {}
  }
}

module.exports = config;
