/**
 * 配置文件 - 推广链接复制工具 2.0
 * 2.0 优化：阶段2 按链接名精确定位「修改」行，避免改到原链接、避免第一个目标账户剧名错
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

  // ===== Excel路径（2.0 与 1.0 区分）=====
  excelPath: './data/link-tasks-v2.0.xlsx',

  // ===== 浏览器持久化目录（保存登录状态） =====
  userDataDir: './browser-data',

  // ===== 超时配置（毫秒） =====
  timeout: {
    default: 30000,       // 默认超时
    element: 10000,       // 元素查找超时
  },

  // ===== 延迟配置（毫秒） - 已优化 =====
  delay: {
    afterSearch: 500,         // 表格加载后的额外等待
    tableWaitAfterSearch: 10000,  // 搜索后等待表格加载的最大时间
    afterCopy: 500,           // 复制后等待（从800ms优化到500ms）
    afterModify: 800,         // 修改后等待（从1000ms优化到800ms）
    dialogOpen: 300,          // 弹窗打开等待（从500ms优化到300ms）
    inputFill: 100,           // 输入框填充等待（从200ms优化到100ms）
  },

  // ===== 并发配置 =====
  parallel: {
    enabled: true,        // 是否启用并行（false = 串行模式，便于调试和接口不稳时使用）
    maxConcurrent: 10,    // 最大并发数（每批同时执行的任务数）
    batchDelay: 2000,     // 每批任务之间的延迟 ms（减轻接口压力）
    staggerInterval: 300, // 批内错峰间隔 ms（每个任务错开启动，减轻瞬时请求）
  },

  // ===== 重试配置 =====
  retry: {
    enabled: true,            // 是否启用重试
    maxAttempts: 3,           // 最大重试次数
    retryDelay: 1000,         // 重试间隔（毫秒）
  },
};

