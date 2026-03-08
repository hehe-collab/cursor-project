/**
 * 素材推送工具配置文件
 */

module.exports = {
  // ===== 基础配置 =====
  materialUrl: 'https://adminxyz.dramahub8.com/advertiseTools/material',
  
  // 浏览器配置
  browserDataDir: './browser-data',
  headless: false,  // 显示浏览器窗口
  slowMo: 50,       // 放慢操作速度（毫秒）
  
  // Excel数据文件路径
  excelPath: './data/push-tasks-v2.0.xlsx',
  
  // ===== 时间配置 =====
  timeout: {
    default: 30000,      // 默认超时30秒
    navigation: 60000,   // 页面导航60秒
    element: 10000,      // 元素查找10秒
  },
  
  // ===== 延迟配置 =====
  delay: {
    afterPush: 1200,        // 推送后等待1.2秒（界面刷新）⚡ 优化
    beforeNextPage: 500,    // 翻页前等待0.5秒 ⚡ 优化
    afterSearch: 800,       // 搜索后等待0.8秒 ⚡ 优化
    domStableCheck: 500,    // ⭐ DOM稳定性检测间隔
    domStableMaxWait: 5000, // ⭐ DOM稳定性检测最大等待时间
  },
  
  // ===== 重试配置 =====
  retry: {
    enabled: true,
    maxAttempts: 3,      // 最多重试3次
    retryDelay: 1000,    // 重试间隔1秒
  },
  
  // ===== 日志配置 =====
  logging: {
    verbose: true,       // 详细日志
    showTimestamp: true, // 显示时间戳
  },
  
  // ===== 并行配置 =====
  parallel: {
    enabled: true,       // ⚡ 是否启用并行（已启用测试）
    maxConcurrent: 10,    // 最大并发数（建议3-5，根据机器性能调整）
    batchDelay: 2000,    // 每批任务之间的延迟（避免服务器压力）
  },
  
  // ===== 验证配置 ===== 
  // ⭐ 新增配置块（方案D/E - 优化版）
  validation: {
    enabled: false,                   // ❌ 禁用推送结果验证（并发环境下提取不可靠）
    warnOnMismatch: false,            // ❌ 不在任务执行时警告（不中断流程）
    displaySummary: false,            // ❌ 不显示汇总报告
    autoRetryOnMismatch: false,       // 数量不匹配时自动重试（暂不启用）
    minSelectionRatio: 0.8,           // 最小选中比例（低于此值则重试）
  },
  
  // ===== 截图配置（暂不启用）=====
  screenshot: {
    enabled: false,
    dir: './screenshots',
  },
};
