/**
 * 配置文件 - 推广链接复制工具
 */

module.exports = {
  // ===== 页面URL =====
  promotionUrl: 'https://adminxyz.dramahub8.com/promotion/manage',

  // ===== 浏览器配置 =====
  browser: {
    headless: false,
    slowMo: 0,  // 移除操作延迟，提速
    viewport: { width: 1920, height: 1080 },
  },

  // ===== Excel路径 =====
  excelPath: './data/link-tasks.xlsx',

  // ===== 浏览器持久化目录（保存登录状态） =====
  userDataDir: './browser-data',

  // ===== 超时配置（毫秒） =====
  timeout: {
    default: 30000,       // 默认超时
    element: 10000,       // 元素查找超时
  },

  // ===== 延迟配置（毫秒） - 已优化 =====
  delay: {
    afterSearch: 500,         // 搜索后等待（从1000ms优化到500ms）
    afterCopy: 500,           // 复制后等待（从800ms优化到500ms）
    afterModify: 800,         // 修改后等待（从1000ms优化到800ms）
    dialogOpen: 300,          // 弹窗打开等待（从500ms优化到300ms）
    inputFill: 100,           // 输入框填充等待（从200ms优化到100ms）
  },

  // ===== 重试配置 =====
  retry: {
    enabled: true,            // 是否启用重试
    maxAttempts: 3,           // 最大重试次数
    retryDelay: 1000,         // 重试间隔（毫秒）
  },
};

