/**
 * 工具函数 - 延时、日志、暂停等
 */
const readline = require('readline');

/**
 * 固定延时（提高执行速度）
 */
function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * 短延时（简单操作间）
 */
async function shortDelay() {
  return delay(200);
}

/**
 * 中等延时（弹窗操作）
 */
async function mediumDelay() {
  return delay(400);
}

/**
 * 长延时（搜索等待）
 */
async function longDelay() {
  return delay(800);
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
  console.log(`[${time}] ${prefix}  ${message}`);
}

/**
 * 暂停脚本，等待用户按 Enter 继续
 */
function pauseForUser(message) {
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

module.exports = {
  delay,
  shortDelay,
  mediumDelay,
  longDelay,
  log,
  pauseForUser,
  safeClick,
  safeType,
};

