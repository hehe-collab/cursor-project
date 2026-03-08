/**
 * 工具函数
 */

const readline = require('readline');
const config = require('../config');

/**
 * 带颜色的日志输出
 */
function log(message, type = 'INFO') {
  const colors = {
    INFO: '\x1b[36m',    // 青色
    OK: '\x1b[32m',      // 绿色
    WARN: '\x1b[33m',    // 黄色
    ERROR: '\x1b[31m',   // 红色
    STEP: '\x1b[35m',    // 紫色
  };
  
  const color = colors[type] || '\x1b[0m';
  const reset = '\x1b[0m';
  const prefix = type === 'INFO' ? '  ' : `[${type}] `;
  
  if (config.logging.showTimestamp) {
    const timestamp = new Date().toLocaleTimeString('zh-CN', { hour12: false });
    console.log(`${color}${timestamp} ${prefix}${message}${reset}`);
  } else {
    console.log(`${color}${prefix}${message}${reset}`);
  }
}

/**
 * 延迟函数
 */
async function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * 暂停等待用户按Enter
 */
async function pauseForUser(message = '按 Enter 继续...') {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
  });
  
  return new Promise(resolve => {
    rl.question(`\n${message}\n`, () => {
      rl.close();
      resolve();
    });
  });
}

/**
 * 步骤级重试包装器
 */
async function withRetry(fn, stepName, maxAttempts = config.retry.maxAttempts) {
  if (!config.retry.enabled) {
    return await fn();
  }
  
  let lastError;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      log(`执行: ${stepName}${attempt > 1 ? ` (第${attempt}次尝试)` : ''}`, 'STEP');
      const result = await fn();
      if (attempt > 1) {
        log(`✅ ${stepName} 重试成功`, 'OK');
      }
      return result;
    } catch (err) {
      lastError = err;
      log(`❌ ${stepName} 失败: ${err.message}`, 'ERROR');
      
      if (attempt < maxAttempts) {
        log(`⏳ 等待 ${config.retry.retryDelay / 1000} 秒后重试...`, 'WARN');
        await delay(config.retry.retryDelay);
      } else {
        log(`❌ ${stepName} 达到最大重试次数 (${maxAttempts})`, 'ERROR');
        // 暂停等待用户干预
        await pauseForUser(`步骤"${stepName}"失败，请手动处理后按 Enter 继续`);
      }
    }
  }
  
  throw lastError;
}

/**
 * 清理账户ID列表
 * - 去除前后空格和分号
 * - 过滤空行
 */
function cleanAccountIds(accountIdsText) {
  if (!accountIdsText) return [];
  
  const lines = accountIdsText.split('\n');
  const cleaned = lines
    .map(line => line.trim())
    .map(line => line.replace(/^[;；]+|[;；]+$/g, '')) // 去除前后中英文分号
    .map(line => line.trim()) // 再次去除空格
    .filter(line => line.length > 0); // 过滤空行
  
  return cleaned;
}

module.exports = {
  log,
  delay,
  pauseForUser,
  withRetry,
  cleanAccountIds,
};


