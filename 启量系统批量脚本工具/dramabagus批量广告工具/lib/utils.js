/**
 * 工具函数 - 延时、日志、暂停、重试等
 * 支持 Electron 模式：通过 setPauseHandler / setLogHandler 注入
 */
const readline = require('readline');
const config = require('../config');

let customPauseHandler = null;
let customLogHandler = null;

function setPauseHandler(handler) {
  customPauseHandler = handler;
}

function setLogHandler(handler) {
  customLogHandler = handler;
}

/**
 * 固定延时
 */
function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * 短延时（简单操作间）- 从配置读取
 */
async function shortDelay() {
  return delay(config.delay.short);
}

/**
 * 中等延时（弹窗操作）- 从配置读取
 */
async function mediumDelay() {
  return delay(config.delay.medium);
}

/**
 * 长延时（搜索等待）- 从配置读取
 */
async function longDelay() {
  return delay(config.delay.long);
}

/**
 * 带时间戳的日志
 */
function log(message, level = 'INFO') {
  const time = new Date().toLocaleTimeString('zh-CN', { hour12: false });
  const prefix = {
    'INFO': '📋',
    'OK': '✅',
    'WARN': '⚠️',
    'ERROR': '❌',
    'STEP': '👉',
  }[level] || '📋';
  const line = `[${time}] ${prefix}  ${message}`;
  if (customLogHandler) customLogHandler(line, level);
  console.log(line);
}

/**
 * 暂停脚本，等待用户按 Enter 继续
 */
function pauseForUser(message) {
  if (customPauseHandler) {
    return customPauseHandler(message);
  }
  return new Promise(resolve => {
    const rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout,
    });
    console.log('');
    log(message, 'WARN');
    rl.question('👆 请手动处理后，按 Enter 继续（输入 q 退出）... ', (answer) => {
      rl.close();
      if (answer.trim().toLowerCase() === 'q') {
        log('用户选择退出', 'ERROR');
        process.exit(0);
      }
      resolve();
    });
  });
}

/**
 * 安全点击 - 等待元素可见并点击，失败时暂停
 */
async function safeClick(page, selector, description, timeout = 10000) {
  try {
    const element = typeof selector === 'string' ? page.locator(selector) : selector;
    await element.waitFor({ state: 'visible', timeout });
    await element.click();
    await shortDelay();
    log(`点击: ${description}`);
  } catch (err) {
    await pauseForUser(`点击失败: ${description}\n   选择器: ${selector}\n   错误: ${err.message}`);
  }
}

/**
 * 安全输入 - 清空后输入文本
 */
async function safeType(page, selector, text, description, timeout = 10000) {
  try {
    const element = typeof selector === 'string' ? page.locator(selector) : selector;
    await element.waitFor({ state: 'visible', timeout });
    await element.click();
    await element.fill('');
    await shortDelay();
    await element.fill(String(text));
    await shortDelay();
    log(`输入: ${description} = "${text}"`);
  } catch (err) {
    await pauseForUser(`输入失败: ${description}\n   选择器: ${selector}\n   错误: ${err.message}`);
  }
}

/**
 * 智能等待元素 - 替代固定延时
 * @param {Locator} locator - Playwright locator
 * @param {Object} options - 等待选项
 * @returns {Promise<boolean>} 是否等待成功
 */
async function waitForElement(locator, options = {}) {
  const {
    state = 'visible',
    timeout = 3000,
    fallbackDelay = 500,
  } = options;

  if (!config.advanced.useIntelligentWait) {
    // 如果未启用智能等待，使用固定延时
    await delay(fallbackDelay);
    return true;
  }

  try {
    await locator.waitFor({ state, timeout });
    return true;
  } catch (err) {
    // 等待失败，使用fallback延时
    if (config.advanced.verboseLog) {
      log(`智能等待失败，使用备用延时: ${err.message}`, 'WARN');
    }
    await delay(fallbackDelay);
    return false;
  }
}

/**
 * 步骤级重试包装函数
 * @param {Function} fn - 要执行的异步函数
 * @param {String} stepName - 步骤名称（用于日志）
 * @param {Object} options - 重试选项
 */
async function withRetry(fn, stepName, options = {}) {
  const {
    maxAttempts = config.retry.maxAttempts,
    retryDelay = config.retry.retryDelay,
    shouldRetry = true,
  } = options;

  // 如果未启用重试或明确不重试，直接执行
  if (!config.retry.enabled || !shouldRetry) {
    return await fn();
  }

  let lastError;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      const result = await fn();
      if (attempt > 1) {
        log(`${stepName} - 重试成功（第${attempt}次尝试）`, 'OK');
      }
      return result;
    } catch (err) {
      lastError = err;
      
      if (attempt < maxAttempts) {
        log(`${stepName} - 失败，${retryDelay}ms后重试（第${attempt}/${maxAttempts}次）`, 'WARN');
        await delay(retryDelay);
      } else {
        log(`${stepName} - 重试${maxAttempts}次后仍失败`, 'ERROR');
      }
    }
  }

  // 所有重试都失败，抛出错误或暂停
  throw lastError;
}

module.exports = {
  delay,
  shortDelay,
  mediumDelay,
  longDelay,
  log,
  pauseForUser,
  setPauseHandler,
  setLogHandler,
  safeClick,
  safeType,
  waitForElement,
  withRetry,
};

