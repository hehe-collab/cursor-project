/**
 * 工具函数
 */

const config = require('../config');

// ===== 日志工具 =====
const LOG_TYPES = {
  INFO: '\x1b[36m[INFO]\x1b[0m',   // 青色
  OK: '\x1b[32m[OK]\x1b[0m',       // 绿色
  WARN: '\x1b[33m[WARN]\x1b[0m',   // 黄色
  ERROR: '\x1b[31m[ERROR]\x1b[0m', // 红色
  STEP: '\x1b[35m[STEP]\x1b[0m',   // 紫色
};

function log(message, type = 'INFO') {
  const prefix = LOG_TYPES[type] || LOG_TYPES.INFO;
  console.log(`${prefix} ${message}`);
}

// ===== 暂停等待用户 =====
async function pauseForUser(message) {
  log(message, 'WARN');
  const readline = require('readline').createInterface({
    input: process.stdin,
    output: process.stdout,
  });

  return new Promise((resolve) => {
    readline.question('按 Enter 继续...', () => {
      readline.close();
      resolve();
    });
  });
}

// ===== 重试包装器 =====
async function withRetry(fn, stepName, maxAttempts = config.retry.maxAttempts) {
  if (!config.retry.enabled) {
    return await fn();
  }

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await fn();
    } catch (error) {
      if (attempt === maxAttempts) {
        log(`❌ ${stepName} 失败（已重试${maxAttempts}次）: ${error.message}`, 'ERROR');
        throw error;
      }
      log(`⚠️ ${stepName} 失败（第${attempt}次），${config.retry.retryDelay}ms后重试...`, 'WARN');
      await new Promise(resolve => setTimeout(resolve, config.retry.retryDelay));
    }
  }
}

// ===== 清理链接名称列表 =====
function cleanLinkNames(rawText) {
  if (!rawText) return [];
  
  return String(rawText)
    .split(/[\n\r]+/)              // 按换行分割
    .map(name => name.trim())       // 去除首尾空格
    .filter(name => name.length > 0); // 过滤空行
}

module.exports = {
  log,
  pauseForUser,
  withRetry,
  cleanLinkNames,
};

