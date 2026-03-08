/**
 * 配置文件 - 登录信息、网站URL、延时参数等
 */
module.exports = {
  // ===== 网站地址 =====
  baseUrl: 'https://adminxyz.dramahub8.com',
  taskUrl: 'https://adminxyz.dramahub8.com/advertiseTools/task',

  // ===== 登录信息 =====
  username: 'chuhai',
  password: '123456',

  // ===== Excel 数据文件路径 =====
  excelPath: './data/tasks.xlsx',

  // ===== 浏览器配置 =====
  browser: {
    headless: false,               // 是否无头模式（false = 显示浏览器窗口）
    profileDir: './browser-data',  // 浏览器用户数据目录（保存登录状态）
    browsersPath: './pw-browsers', // Playwright 浏览器存放路径（项目目录内）
    slowMo: 0,                     // 全局慢动作（毫秒），调试时可设为 50-100
  },

  // ===== 延时配置（毫秒）===== 
  delay: {
    short: 100,      // 简单操作间（点击、输入）- 优化后减少50%
    medium: 200,     // 弹窗打开/关闭 - 优化后减少50%
    long: 400,       // 搜索等待结果 - 优化后减少50%
  },

  // ===== 重试配置 =====
  retry: {
    enabled: true,           // 是否启用自动重试
    maxAttempts: 3,          // 每个步骤最多重试次数
    retryDelay: 1000,        // 重试前等待时间（毫秒）
  },

  // ===== 并发配置 =====
  parallel: {
    enabled: true,           // 是否启用并行（✅ 已开启）
    maxConcurrent: 10,       // 最大并发数（您的M4 Pro可以轻松支持10并发）
    batchDelay: 2000,        // 每批任务之间的延迟（避免服务器压力）
  },

  // ===== 高级配置 =====
  advanced: {
    useIntelligentWait: true,   // 使用智能等待（替代固定延时）
    skipNetworkIdle: true,       // 跳过部分网络空闲等待，改用元素等待
    verboseLog: false,           // 详细日志（调试用）
  },

  // ===== 验证配置 =====
  validation: {
    enableScreenshot: true,         // 是否启用自动截图
    enableValidationLog: true,      // 是否启用增强验证日志
    screenshotDir: './screenshots', // 截图保存目录（项目目录内，不占用系统盘）
    // screenshotQuality: 已移除（PNG格式不支持quality，如需压缩请改用JPEG格式）
  },
};

