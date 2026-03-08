/**
 * 自动化核心逻辑 - 登录 + 20步广告建设流程
 *
 * 基于 Element UI 组件选择器（该平台使用 Element UI 框架）
 */
const path = require('path');
const fs = require('fs');
const config = require('../config');

// 设置 Playwright 浏览器路径（使用项目内的浏览器，不占用系统盘）
const browsersPath = path.resolve(__dirname, '..', config.browser.browsersPath || './pw-browsers');
process.env.PLAYWRIGHT_BROWSERS_PATH = browsersPath;

const { chromium } = require('playwright');
const {
  shortDelay, mediumDelay, longDelay,
  log, pauseForUser, safeClick, safeType,
  waitForElement, withRetry,
} = require('./utils');

// ============================================================
//  浏览器启动 & 登录
// ============================================================

/**
 * 启动浏览器（持久化上下文，保留登录状态）
 */
async function launchBrowser() {
  log('启动浏览器...');
  log(`浏览器路径: ${browsersPath}`);

  // 清除浏览器崩溃标记，防止"恢复页面"弹窗
  const profileDir = path.resolve(__dirname, '..', config.browser.profileDir);
  try {
    const prefsFile = path.join(profileDir, 'Default', 'Preferences');
    if (fs.existsSync(prefsFile)) {
      let prefs = fs.readFileSync(prefsFile, 'utf8');
      // 将 exit_type 标记为正常退出
      prefs = prefs.replace(/"exit_type"\s*:\s*"[^"]*"/, '"exit_type":"Normal"');
      prefs = prefs.replace(/"exited_cleanly"\s*:\s*false/, '"exited_cleanly":true');
      fs.writeFileSync(prefsFile, prefs, 'utf8');
      log('已清除浏览器崩溃标记');
    }
  } catch {
    // 忽略错误
  }

  const context = await chromium.launchPersistentContext(
    path.resolve(__dirname, '..', config.browser.profileDir),
    {
      headless: config.browser.headless,
      slowMo: config.browser.slowMo,
      viewport: { width: 1440, height: 900 },
      locale: 'zh-CN',
      args: [
        '--disable-blink-features=AutomationControlled',
        '--hide-crash-restore-bubble',     // 禁止"恢复页面"弹窗
        '--disable-infobars',              // 禁止信息栏
        '--no-default-browser-check',      // 禁止默认浏览器检查
        '--disable-popup-blocking',        // 禁止弹窗拦截
        '--disable-features=TranslateUI',  // 禁止翻译提示
        '--noerrdialogs',                  // 禁止错误对话框
        '--disable-breakpad',              // 🆕 禁用崩溃报告系统
        '--disable-crash-reporter',        // 🆕 禁用崩溃报告上传
        '--no-crash-upload',               // 🆕 不上传崩溃信息
        '--disable-session-crashed-bubble', // 🆕 禁用"意外退出"弹窗
      ],
    }
  );

  const page = context.pages()[0] || await context.newPage();

  // 将浏览器窗口弹到前台
  await page.bringToFront();

  // 自动处理 JavaScript alert/confirm/prompt 弹窗
  page.on('dialog', async (dialog) => {
    log(`自动关闭弹窗: ${dialog.type()} - ${dialog.message()}`, 'WARN');
    await dialog.accept();
  });

  return { context, page };
}

/**
 * 准备页面 - 导航到批量工具页面并等待用户确认
 * 不再自动登录，由用户自行处理登录和弹窗
 */
async function preparePage(page) {
  log('导航到批量工具页面...');

  // 导航到批量工具页面
  await page.goto(config.taskUrl, { waitUntil: 'domcontentloaded', timeout: 30000 });

  // 等待页面基本加载（优化：使用domcontentloaded代替networkidle）
  if (config.advanced.skipNetworkIdle) {
    await page.waitForLoadState('domcontentloaded').catch(() => {});
    await shortDelay();
  } else {
    await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
    await mediumDelay();
  }

  // 检查是否在登录页（未登录状态）
  const currentUrl = page.url();
  if (!currentUrl.includes('advertiseTools/task')) {
    log('当前不在批量工具页面（可能需要登录或有弹窗）', 'WARN');
  }

  // 暂停等待用户确认页面已就绪
  await pauseForUser(
    '浏览器已打开。请在浏览器中完成以下操作：\n' +
    '   1. 如果需要登录，请手动登录\n' +
    '   2. 关闭所有弹窗（如"更改密码"提示等）\n' +
    '   3. 确保已进入「批量工具」页面\n' +
    '   准备好后按 Enter 开始执行任务'
  );

  // 确认当前是否在批量工具页面
  const finalUrl = page.url();
  if (!finalUrl.includes('advertiseTools/task')) {
    log('当前不在批量工具页面，正在跳转...', 'WARN');
    await page.goto(config.taskUrl, { waitUntil: 'networkidle', timeout: 30000 });
    await mediumDelay();
  }

  log('页面已就绪，开始执行任务', 'OK');
}

// ============================================================
//  验证功能：截图 + 增强日志
// ============================================================

/**
 * 自动截图（保存到项目目录，不占用系统盘）
 * @param {Page} page - Playwright页面对象
 * @param {string} taskId - 任务ID
 * @param {string} stepName - 步骤名称（如 "01-项目设置"）
 * @param {string} accountId - 账户ID（可选）
 */
async function takeScreenshot(page, taskId, stepName, accountId = '') {
  if (!config.validation.enableScreenshot) return;

  try {
    // 创建截图目录：./screenshots/任务ID/
    const screenshotBaseDir = path.resolve(__dirname, '..', config.validation.screenshotDir);
    const taskScreenshotDir = path.join(screenshotBaseDir, String(taskId));
    
    if (!fs.existsSync(taskScreenshotDir)) {
      fs.mkdirSync(taskScreenshotDir, { recursive: true });
    }

    // 生成截图文件名
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
    const accountSuffix = accountId ? `-账户${accountId}` : '';
    const filename = `${stepName}${accountSuffix}_${timestamp}.png`;
    const filepath = path.join(taskScreenshotDir, filename);

    // 截图（全页面，PNG格式）
    await page.screenshot({
      path: filepath,
      fullPage: true,
      // 注意：PNG格式不支持quality参数，如需压缩请改用.jpg格式
    });

    log(`  📸 截图已保存: ${filename}`, 'OK');
  } catch (err) {
    log(`  ⚠️  截图失败: ${err.message}`, 'WARN');
    // 截图失败不影响主流程
  }
}

/**
 * 验证日志：显示 Excel预期值 vs 实际执行值
 * @param {string} field - 字段名称（如 "账户ID"）
 * @param {any} expected - Excel预期值
 * @param {any} actual - 实际执行值
 */
function logValidation(field, expected, actual) {
  if (!config.validation.enableValidationLog) return;

  const expectedStr = String(expected);
  const actualStr = String(actual);
  const isMatch = expectedStr === actualStr;

  console.log(`  📋 Excel配置: ${field} = ${expectedStr}`);
  console.log(`  ✓ 实际执行: ${actualStr}`);
  console.log(`  🔍 是否一致: ${isMatch ? '✅' : '❌'}`);

  if (!isMatch) {
    log(`  ⚠️  数据不一致！字段"${field}"，Excel为"${expectedStr}"，实际为"${actualStr}"`, 'WARN');
  }
}

/**
 * 验证输入框的值
 * @param {Locator} input - Playwright Locator对象
 * @param {string} field - 字段名称
 * @param {any} expected - Excel预期值
 */
async function validateInputValue(input, field, expected) {
  if (!config.validation.enableValidationLog) return;

  try {
    const actual = await input.inputValue();
    logValidation(field, expected, actual);
  } catch (err) {
    log(`  ⚠️  无法验证字段"${field}": ${err.message}`, 'WARN');
  }
}

// ============================================================
//  全局弹窗/提示清除器
// ============================================================

/**
 * 关闭页面上可能出现的各种弹窗、提示、通知
 * 包括：Chrome恢复提示、Element UI 消息框、通知、遮罩层等
 */
async function dismissPopups(page) {
  try {
    // 1. Chrome "恢复页面" 弹窗 - 点击 X 关闭
    const restoreX = page.locator('button:has-text("恢复")').or(
      page.locator('.infobar-close, [aria-label="关闭"], [aria-label="Close"]')
    );
    if (await restoreX.first().isVisible({ timeout: 500 }).catch(() => false)) {
      // 不点恢复，点关闭按钮（X）
      const closeBtn = page.locator('.infobar-close, [aria-label="关闭"]').first();
      if (await closeBtn.isVisible({ timeout: 300 }).catch(() => false)) {
        await closeBtn.click();
        log('关闭了 Chrome 恢复提示', 'WARN');
      }
    }

    // 2. Element UI 消息确认框 (el-message-box)
    const msgBox = page.locator('.el-message-box__wrapper:visible');
    if (await msgBox.isVisible({ timeout: 300 }).catch(() => false)) {
      const closeBtn = msgBox.locator('.el-message-box__close, .el-message-box__headerbtn').first();
      if (await closeBtn.isVisible({ timeout: 300 }).catch(() => false)) {
        await closeBtn.click();
        log('关闭了消息确认框', 'WARN');
      }
    }

    // 3. Element UI 通知 (el-notification)
    const notifications = page.locator('.el-notification:visible');
    const notifCount = await notifications.count().catch(() => 0);
    for (let i = 0; i < notifCount; i++) {
      const closeBtn = notifications.nth(i).locator('.el-notification__closeBtn');
      if (await closeBtn.isVisible({ timeout: 200 }).catch(() => false)) {
        await closeBtn.click();
      }
    }

    // 4. Element UI 消息提示 (el-message) - 这种一般自动消失，不需要处理

    // 5. 遮罩层 (v-modal) - 如果有残留的遮罩层，按 ESC 关闭
    const modal = page.locator('.v-modal:visible');
    if (await modal.isVisible({ timeout: 300 }).catch(() => false)) {
      await page.keyboard.press('Escape');
      await shortDelay();
      log('按 ESC 关闭了遮罩层', 'WARN');
    }

  } catch {
    // 弹窗处理失败不影响主流程
  }
}

// ============================================================
//  Step 1: 选择主体
// ============================================================

async function selectEntity(page, entityName) {
  log(`选择主体: ${entityName}`, 'STEP');

  // 精确定位主体下拉框：排除顶部搜索框（header-search-select），选 el-select--small
  const select = page.locator('.el-select.el-select--small').first();

  await select.click();
  await shortDelay();

  // 在下拉列表中找到目标选项并点击
  const dropdown = page.locator('.el-select-dropdown:visible').last();
  const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: entityName });

  await option.waitFor({ state: 'visible', timeout: 5000 });
  await option.click();
  await mediumDelay();

  log(`主体已选择: ${entityName}`, 'OK');
}

// ============================================================
//  Step 2: 选择账户（通过数字ID检索）
// ============================================================

async function selectAccounts(page, accounts) {
  log(`选择账户 (${accounts.length} 个)`, 'STEP');

  // Element UI 多选下拉结构：
  //   .el-select__tags > input.el-select__input  ← 这是真正的搜索框（始终存在，不受已选项影响）
  //   选第一个账户后 placeholder="可多选" 会消失，所以不能用 placeholder 定位
  //   .el-select__tags 是多选组件独有的，页面上只有账户选择器是多选

  for (let i = 0; i < accounts.length; i++) {
    const accountId = accounts[i].accountId;
    log(`  搜索账户: ${accountId}`);

    // 每次重新定位（选中账户后DOM会变化）
    const searchInput = page.locator('.el-select__tags input.el-select__input').first();

    // 点击搜索输入框
    await searchInput.click();
    await shortDelay();

    // 清空并输入账户ID
    await searchInput.fill('');
    await shortDelay();
    await searchInput.fill(accountId);
    await longDelay(); // 等待搜索结果

    // 在下拉列表中找到匹配项并点击
    const dropdown = page.locator('.el-select-dropdown:visible').last();
    const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: accountId });

    try {
      await option.waitFor({ state: 'visible', timeout: 8000 });
      await option.click();
      await shortDelay();
      log(`  账户已选择: ${accountId}`, 'OK');
      
      // 🔍 验证：账户是否选中
      logValidation(`账户${i + 1}`, accountId, accountId);
    } catch (err) {
      await pauseForUser(`找不到账户: ${accountId}\n请手动选择该账户`);
    }
  }

  // 按 Escape 关闭下拉（不用点击页面，避免误触链接跳转）
  await page.keyboard.press('Escape');
  await mediumDelay();

  // 等待左侧项目列表加载（优化）
  if (config.advanced.skipNetworkIdle) {
    await page.waitForLoadState('domcontentloaded').catch(() => {});
    await shortDelay();
  } else {
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
    await mediumDelay();
  }

  log(`所有账户已选择`, 'OK');
}

// ============================================================
//  Steps 3-7: 推广项目设置
// ============================================================

async function setupProject(page, taskGroup) {
  log('设置推广项目...', 'STEP');

  // 等待左侧列表加载完成（优化：减少等待时间）
  if (config.advanced.skipNetworkIdle) {
    await page.waitForLoadState('domcontentloaded').catch(() => {});
    await mediumDelay(); // 200ms代替1500ms
  } else {
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(1500); // 额外等待 DOM 渲染
  }

  // ✅ 等待 Loading 遮罩消失（页面加载、接口请求完成）
  await waitForLoadingMaskDisappear(page);

  // 调试：打印页面上所有包含"设置"的元素
  const debugElements = await page.locator('*').filter({ hasText: '设置' }).all();
  log(`调试：页面上找到 ${debugElements.length} 个包含"设置"的元素`);
  for (let i = 0; i < Math.min(debugElements.length, 10); i++) {
    try {
      const tagName = await debugElements[i].evaluate(el => el.tagName);
      const className = await debugElements[i].evaluate(el => el.className);
      const text = await debugElements[i].innerText();
      const isVisible = await debugElements[i].isVisible();
      log(`  [${i}] <${tagName}> class="${className}" text="${text.trim()}" visible=${isVisible}`);
    } catch {}
  }

  // 策略1：通过 JavaScript 直接点击第一个包含"设置"的可见元素
  let clickSuccess = false;
  try {
    log('尝试通过 JavaScript 点击项目"设置"...');
    const clicked = await page.evaluate(() => {
      const allElements = Array.from(document.querySelectorAll('*'));
      const settingsElements = allElements.filter(el => {
        const text = el.textContent?.trim();
        return text === '设置' && el.offsetParent !== null;
      });
      
      if (settingsElements.length > 0) {
        settingsElements[0].click();
        return true;
      }
      return false;
    });
    
    if (clicked) {
      await mediumDelay();
      log('项目"设置"已点击（JavaScript）', 'OK');
      clickSuccess = true;
    }
  } catch (err) {
    log(`JavaScript 点击失败: ${err.message}`);
  }

  // 策略2：Playwright 定位器
  if (!clickSuccess) {
    try {
      const settingsLinks = page.locator('.el-link, a, span').filter({ hasText: '设置' });
      const first = settingsLinks.first();
      await first.waitFor({ state: 'attached', timeout: 3000 });
      await first.scrollIntoViewIfNeeded();
      await first.click({ force: true });
      await mediumDelay();
      log('项目"设置"已点击（Playwright force）', 'OK');
      clickSuccess = true;
    } catch (err) {
      log(`Playwright 点击失败: ${err.message}`);
    }
  }

  // 如果点击失败，暂停让用户手动处理
  if (!clickSuccess) {
    await page.screenshot({ path: './debug-setup-project.png', fullPage: true });
    log('已保存调试截图: debug-setup-project.png');
    await pauseForUser('找不到项目"设置"按钮，请手动点击项目区域的"设置"');
  }

  // 等待弹窗出现
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '广告系列' }).or(
    page.locator('.el-dialog:visible').first()
  );
  await dialog.waitFor({ state: 'visible', timeout: 10000 });
  log('项目设置弹窗已打开');

  // 获取表格中的所有行
  const tableRows = dialog.locator('.el-table__body-wrapper .el-table__row');
  const rowCount = await tableRows.count();
  log(`  发现 ${rowCount} 个账户行`);

  // 逐行设置
  for (let i = 0; i < rowCount; i++) {
    const row = tableRows.nth(i);
    const account = taskGroup.accounts[i];
    if (!account) break;

    log(`  设置账户 ${i + 1}/${rowCount}: ${account.accountId}`);

    // --- 选择 Pixel ---
    await selectPixelInRow(page, row, account.pixel);

    // --- 填写项目名称 ---
    await fillProjectNameInRow(row, taskGroup.projectName);

    // --- 开启/关闭 Smart+ ---
    await toggleSmartPlusInRow(row, true);

    // --- 开启/关闭 启用 ---
    const enableValue = account.enable !== undefined ? account.enable : taskGroup.enable;
    await toggleEnableInRow(row, enableValue !== false); // 默认启用
  }

  // 点击确定
  const confirmBtn = dialog.locator('button').filter({ hasText: '确定' }).or(
    dialog.locator('.el-button--primary')
  );
  await confirmBtn.click();
  await mediumDelay();

  // 等待弹窗完全关闭，确保 DOM 更新
  try {
    await dialog.waitFor({ state: 'hidden', timeout: 5000 });
  } catch {}
  await page.waitForTimeout(800); // 额外等待 DOM 更新

  log('推广项目设置完成', 'OK');
}

/**
 * 等待 Loading 遮罩消失
 * 用于处理接口请求时的加载状态，确保页面可交互
 * @param {Page} page - Playwright 页面对象
 * @param {number} maxWaitTime - 最大等待时间（毫秒），默认15秒
 * @returns {Promise<boolean>} - 遮罩是否成功消失
 */
async function waitForLoadingMaskDisappear(page, maxWaitTime = 30000) {
  log(`  等待 Loading 遮罩消失...`, 'STEP');
  const startTime = Date.now();
  const checkInterval = 500;
  
  while (Date.now() - startTime < maxWaitTime) {
    const loadingMask = page.locator('.el-loading-mask:visible');
    const count = await loadingMask.count();
    
    if (count === 0) {
      log(`  ✅ Loading 遮罩已消失`, 'OK');
      return true;
    }
    
    // 显示等待进度
    const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
    log(`  ⏳ 仍有 ${count} 个 Loading 遮罩存在... (已等待 ${elapsed}秒)`, 'INFO');
    
    await page.waitForTimeout(checkInterval);
  }
  
  log(`  ⚠️ Loading 遮罩超时未消失（${maxWaitTime / 1000}秒），可能是接口超时或限流`, 'WARN');
  return false;
}

/**
 * 在行内选择 Pixel
 */
async function selectPixelInRow(page, row, pixelName) {
  try {
    // Pixel 是表格中的一个下拉选择器
    const cells = row.locator('td');
    // Pixel 通常在第3列（索引2），根据截图：账户ID, 账户, Pixel, ...
    const pixelCell = cells.nth(2);
    const pixelSelect = pixelCell.locator('.el-select, .el-input').first();

    // ✅ 等待 Loading 遮罩消失（防止接口限流导致遮罩永久存在）
    const maskDisappeared = await waitForLoadingMaskDisappear(page);
    if (!maskDisappeared) {
      throw new Error('Loading 遮罩未消失，可能是接口超时或后端限流');
    }

    await pixelSelect.click();
    await shortDelay();

    // 在下拉中找到对应选项
    const dropdown = page.locator('.el-select-dropdown:visible').last();
    const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: pixelName });
    await option.waitFor({ state: 'visible', timeout: 5000 });
    await option.click();
    await shortDelay();

    log(`    Pixel: ${pixelName}`);
  } catch (err) {
    await pauseForUser(`选择 Pixel 失败: ${pixelName}\n错误: ${err.message}\n请手动选择`);
  }
}

/**
 * 在行内填写项目名称
 */
async function fillProjectNameInRow(row, projectName) {
  try {
    const cells = row.locator('td');
    // 项目名称通常在第5列（索引4）：账户ID, 账户, Pixel, 已有项目, 项目名称, ...
    const nameCell = cells.nth(4);
    const nameInput = nameCell.locator('.el-input__inner, input').first();

    await nameInput.click();
    await nameInput.fill('');
    await shortDelay();
    await nameInput.fill(projectName);
    await shortDelay();

    log(`    项目名称: ${projectName}`);
  } catch (err) {
    await pauseForUser(`填写项目名称失败: ${projectName}\n错误: ${err.message}\n请手动填写`);
  }
}

/**
 * 在行内开启/关闭 Smart+
 * 从截图看列顺序：账户ID(0), 账户(1), Pixel(2), 已有项目(3), 项目名称(4), 推广目标(5), Smart+(6), 启用(7)
 * Smart+ 是每行的第1个 .el-switch
 */
async function toggleSmartPlusInRow(row, enable) {
  try {
    const switchEl = row.locator('.el-switch').first(); // 第1个开关 = Smart+
    const isChecked = await switchEl.evaluate(el => el.classList.contains('is-checked'));

    if (enable !== isChecked) {
      await switchEl.click();
      await shortDelay();
    }

    log(`    Smart+: ${enable ? '开启' : '关闭'}`);
  } catch (err) {
    await pauseForUser(`切换 Smart+ 失败\n错误: ${err.message}\n请手动切换`);
  }
}

/**
 * 在行内开启/关闭 启用
 * 启用 是每行的第2个 .el-switch
 */
async function toggleEnableInRow(row, enable) {
  try {
    const switchEl = row.locator('.el-switch').nth(1); // 第2个开关 = 启用
    const isChecked = await switchEl.evaluate(el => el.classList.contains('is-checked'));

    if (enable !== isChecked) {
      await switchEl.click();
      await shortDelay();
    }

    log(`    启用: ${enable ? '开启' : '关闭'}`);
  } catch (err) {
    await pauseForUser(`切换启用状态失败\n错误: ${err.message}\n请手动切换`);
  }
}

// ============================================================
//  Steps 8-19: 广告组设置
// ============================================================

async function setupAdGroup(page, taskGroup) {
  log('设置广告组...', 'STEP');

  // 确保项目设置弹窗已完全关闭
  await page.waitForTimeout(1000);

  // ✅ 等待 Loading 遮罩消失（项目设置后页面可能在加载）
  await waitForLoadingMaskDisappear(page);

  // 点击广告组区域的"设置"按钮 - 使用坐标判断法（最可靠）
  let clickSuccess = false;

  try {
    log('查找广告组"设置"按钮（通过坐标判断）...');
    const clicked = await page.evaluate(() => {
      // 找所有精确文本为"设置"的可见链接/按钮
      const allElements = Array.from(document.querySelectorAll('a, span, button'));
      const settingsElements = allElements.filter(el => {
        const text = el.innerText?.trim() || el.textContent?.trim();
        const isVisible = el.offsetParent !== null && el.offsetWidth > 0 && el.offsetHeight > 0;
        return isVisible && text === '设置';
      });
      
      if (settingsElements.length === 0) {
        console.log('没有找到"设置"元素');
        return false;
      }
      
      console.log(`找到 ${settingsElements.length} 个"设置"元素`);
      
      // 获取每个元素的位置信息
      const elementsWithPosition = settingsElements.map(el => {
        const rect = el.getBoundingClientRect();
        return {
          element: el,
          x: rect.x,
          y: rect.y,
          width: rect.width,
          height: rect.height
        };
      });
      
      // 按Y坐标排序（从上到下）
      elementsWithPosition.sort((a, b) => a.y - b.y);
      
      elementsWithPosition.forEach((item, index) => {
        console.log(`  [${index}] Y=${Math.round(item.y)}, X=${Math.round(item.x)}`);
      });
      
      // 广告组的"设置"应该在项目"设置"的下方
      // 选择Y坐标最大的（最下方的）"设置"，如果有多个就选第2个
      let targetElement = null;
      
      if (elementsWithPosition.length === 1) {
        // 只有1个，直接点击
        targetElement = elementsWithPosition[0].element;
        console.log('只有1个"设置"，直接点击');
      } else if (elementsWithPosition.length >= 2) {
        // 有2个或更多，点击第2个（广告组的）
        targetElement = elementsWithPosition[1].element;
        console.log(`点击第2个"设置"（Y=${Math.round(elementsWithPosition[1].y)}）`);
      }
      
      if (targetElement) {
        // 确保元素在视口中
        targetElement.scrollIntoView({ behavior: 'auto', block: 'center' });
        targetElement.click();
        return true;
      }
      
      return false;
    });
    
    if (clicked) {
      await mediumDelay();
      log('广告组"设置"已点击', 'OK');
      clickSuccess = true;
    }
  } catch (err) {
    log(`JavaScript 点击失败: ${err.message}`);
  }

  // 如果点击失败，暂停让用户手动处理
  if (!clickSuccess) {
    await page.screenshot({ path: './debug-setup-adgroup.png', fullPage: true });
    log('已保存调试截图: debug-setup-adgroup.png');
    await pauseForUser('找不到广告组"设置"按钮，请手动点击广告组区域的"设置"');
  }

  // 验证：确保打开的是广告组设置弹窗
  await page.waitForTimeout(600);
  try {
    const dialogTitle = await page.locator('.el-dialog:visible .el-dialog__title').first().innerText();
    log(`弹窗标题: "${dialogTitle}"`);
    
    if (dialogTitle.includes('项目') || dialogTitle.includes('广告系列')) {
      log('⚠️  错误：打开的是项目设置弹窗，不是广告组设置！', 'ERROR');
      await pauseForUser('弹窗打开错误，请手动关闭当前弹窗，然后点击广告组区域的"设置"');
    }
  } catch {
    // 可能没有标题，继续执行
  }

  // 等待广告组设置弹窗
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '广告组设置' }).or(
    page.locator('.el-dialog:visible').last()
  );
  await dialog.waitFor({ state: 'visible', timeout: 10000 });
  log('广告组设置弹窗已打开');

  // 确保在 Smart+ 标签页
  const smartTab = dialog.locator('text=Smart+').first();
  try {
    if (await smartTab.isVisible()) {
      await smartTab.click();
      await shortDelay();
    }
  } catch {}

  // 获取所有账户行
  const tableRows = dialog.locator('.el-table__body-wrapper .el-table__row');
  const rowCount = await tableRows.count();
  log(`  发现 ${rowCount} 个账户行`);

  // 逐行设置广告组
  for (let i = 0; i < rowCount; i++) {
    const row = tableRows.nth(i);
    const account = taskGroup.accounts[i];
    if (!account) break;

    log(`\n  ── 账户 ${i + 1}/${rowCount}: ${account.accountId} ──`);

    // Step 9-11: 选择推广链接（带重试）
    await withRetry(
      () => selectPromotionLink(page, dialog, row, account.linkKeyword),
      `选择推广链接 [${account.linkKeyword}]`
    );

    // Step 12-14: 选择素材（带重试）
    // 第1个账户需要更长等待时间来"预热"状态，其他账户可以极速执行
    const isFirstAccount = (i === 0);
    await withRetry(
      () => selectMaterials(page, dialog, row, taskGroup.materialKeyword, taskGroup.materialIds, isFirstAccount),
      `选择素材 [${taskGroup.materialKeyword || `${taskGroup.materialIds?.length}个ID`}]`
    );

    // Step 15: 选择标题（带重试）
    await withRetry(
      () => selectTitles(page, dialog, row, account.titles),
      `选择标题 [${account.titles.length}个]`
    );

    // Step 16: 选择优化目标（带重试）
    await withRetry(
      () => selectOptimizationTarget(page, dialog, row, taskGroup.optimizationTarget),
      `选择优化目标 [${taskGroup.optimizationTarget}]`
    );

    // Step 17: 输入出价和预算（带重试）
    await withRetry(
      () => inputBidAndBudget(page, dialog, row, taskGroup.bid, taskGroup.budget, taskGroup.optimizationTarget),
      `输入出价和预算 [${taskGroup.bid}/${taskGroup.budget}]`
    );

    // Step 18: 选择开始时间（带重试）
    await withRetry(
      () => selectStartTime(page, dialog, row, taskGroup.startDate, taskGroup.startTime),
      `选择开始时间 [${taskGroup.startDate} ${taskGroup.startTime}]`
    );

    // 可选：设置年龄
    if (taskGroup.age && taskGroup.age !== '18+') {
      await selectAge(page, dialog, row, taskGroup.age);
    }

    // 可选：设置认证身份
    if (taskGroup.identity) {
      await selectIdentity(page, dialog, row, taskGroup.identity);
    }
  }

  // Step 19: 点击确定
  const confirmBtn = dialog.locator('.el-dialog__footer button').filter({ hasText: '确定' }).or(
    dialog.locator('.el-button--primary').last()
  );
  await confirmBtn.click();
  await mediumDelay();

  log('广告组设置完成', 'OK');
}

// ── Step 9-11: 选择推广链接 ──

async function selectPromotionLink(page, adGroupDialog, row, linkKeyword) {
  log(`    选择推广链接: ${linkKeyword}`);

  // ✅ 等待 Loading 遮罩消失（广告组弹窗内的接口加载）
  await waitForLoadingMaskDisappear(page);

  try {
    // 方式1：找"未选择"文字并点击
    let clicked = false;
    try {
      const linkCell = row.locator('td').filter({ hasText: '未选择' }).first();
      await linkCell.click({ timeout: 3000 });
      clicked = true;
    } catch {}

    // 方式2：找链接列的任意可点击元素（按钮、图标等）
    if (!clicked) {
      try {
        // 链接列通常是第3列（索引2），找其中的按钮或可点击元素
        const linkCell = row.locator('td').nth(2);
        const clickableElements = linkCell.locator('button, .el-button, i, svg, span').first();
        await clickableElements.click({ timeout: 3000 });
        clicked = true;
      } catch {}
    }

    // 方式3：直接点击第3列单元格
    if (!clicked) {
      await row.locator('td').nth(2).click();
    }

    await mediumDelay();
    await page.waitForTimeout(800); // 额外等待弹窗出现

    // 等待推广链接选择弹窗（尝试多种标题）
    let linkDialog = page.locator('.el-dialog:visible').filter({ hasText: '选择推广链接' });
    try {
      await linkDialog.waitFor({ state: 'visible', timeout: 3000 });
    } catch {
      // 备选：找任意新出现的弹窗
      linkDialog = page.locator('.el-dialog:visible').last();
      await linkDialog.waitFor({ state: 'visible', timeout: 5000 });
    }

    // 在推广名称输入框中搜索
    const nameInput = linkDialog.locator('input[placeholder*="推广名称"], input[placeholder*="名称"]').or(
      linkDialog.locator('.el-input__inner').nth(1) // 第二个输入框（第一个是推广ID）
    );
    await nameInput.fill(linkKeyword);
    await shortDelay();

    // 点击搜索按钮
    const searchBtn = linkDialog.locator('button').filter({ hasText: '搜索' }).or(
      linkDialog.locator('.el-button--primary').first()
    );
    await searchBtn.click();
    await longDelay(); // 等待搜索结果

    // 点击第一条结果的"+添加"按钮
    const addBtn = linkDialog.locator('text=添加').first().or(
      linkDialog.locator('.el-table__body-wrapper .el-table__row').first().locator('text=添加')
    );
    await addBtn.waitFor({ state: 'visible', timeout: 8000 });
    await addBtn.click();
    await shortDelay();

    // 点击确认
    const confirmBtn = linkDialog.locator('.el-dialog__footer button').filter({ hasText: '确认' }).or(
      linkDialog.locator('.el-dialog__footer .el-button--primary')
    );
    await confirmBtn.click();
    await mediumDelay();

    log(`    推广链接已选择`, 'OK');
  } catch (err) {
    // 重新抛出错误，让外层的 withRetry 能够捕获并自动重试
    throw err;
  }
}

// ── Step 12-14: 选择素材 ──

async function selectMaterials(page, adGroupDialog, row, materialKeyword, materialIds = [], isFirstAccount = false) {
  // 素材关键词优先，如果没有则使用素材ID
  const useKeyword = !!materialKeyword;
  const useIds = !useKeyword && materialIds.length > 0;
  
  // 根据是否是第一个账户，动态调整等待时间
  // 第1个账户：900ms（给予更多时间，失败则自动重试）
  // 其他账户：200ms（状态已就绪，极速执行）
  const syncWaitTime = isFirstAccount ? 900 : 200;
  
  if (useKeyword) {
    log(`    选择素材: 关键词 "${materialKeyword}"`);
  } else if (useIds) {
    log(`    选择素材: ID (${materialIds.length}个)`);
  } else {
    log(`    ⚠️  素材关键词和素材ID都未填写，跳过`, 'WARN');
    return;
  }

  try {
    // 检测并关闭已存在的素材选择弹窗（重试时可能存在）
    try {
      const existingDialog = page.locator('.el-dialog:visible').filter({ hasText: '选择素材' });
      const dialogCount = await existingDialog.count();
      if (dialogCount > 0) {
        log(`    检测到已存在的素材选择弹窗，先关闭...`, 'WARN');
        const closeBtn = existingDialog.first().locator('.el-dialog__headerbtn, .el-dialog__close').first();
        await closeBtn.click({ timeout: 2000 });
        await shortDelay();
      }
    } catch (cleanupErr) {
      // 清理失败不影响主流程
    }
    
    // 方式1：找"已选"文字并点击
    let clicked = false;
    try {
      const materialCell = row.locator('td').filter({ hasText: '已选' }).first();
      await materialCell.click({ timeout: 3000 });
      clicked = true;
    } catch {}

    // 方式2：找素材列的按钮/图标
    if (!clicked) {
      try {
        const materialCell = row.locator('td').nth(3); // 素材列
        const clickableElements = materialCell.locator('button, .el-button, i, svg, span').first();
        await clickableElements.click({ timeout: 3000 });
        clicked = true;
      } catch {}
    }

    // 方式3：直接点击第4列
    if (!clicked) {
      await row.locator('td').nth(3).click();
    }

    await mediumDelay();
    await page.waitForTimeout(800);

    // 等待素材选择弹窗（尝试多种标题）
    let materialDialog = page.locator('.el-dialog:visible').filter({ hasText: '选择素材' });
    try {
      await materialDialog.waitFor({ state: 'visible', timeout: 3000 });
    } catch {
      // 备选：找任意新出现的弹窗
      materialDialog = page.locator('.el-dialog:visible').last();
      await materialDialog.waitFor({ state: 'visible', timeout: 5000 });
    }

    // Step 13: 修改翻页条数为 30条/页
    await changePageSize(page, materialDialog, '30');

    // Step 14: 搜索素材
    if (useKeyword) {
      // 方式1: 通过素材名称检索
      log(`      通过素材名称检索...`);
      const nameInput = materialDialog.locator('input[placeholder*="素材名称"], input[placeholder*="名称"]').first();
      
      // 🔥 关键修复：先激活输入框，确保搜索功能完全就绪
      await nameInput.click(); // 聚焦输入框
      await page.waitForTimeout(300); // 等待输入框激活
      
      // 填入搜索关键词
      await nameInput.fill(materialKeyword);
      
      // 智能等待：第1个账户900ms，其他账户200ms
      await page.waitForTimeout(syncWaitTime);
      log(`        已等待 ${syncWaitTime}ms 确保数据同步${isFirstAccount ? '（第1个账户）' : ''}`);
    } else if (useIds) {
      // 方式2: 通过素材ID检索
      log(`      通过素材ID检索 (${materialIds.length}个ID)...`);
      
      // 尝试多种方式定位素材ID输入框
      let idInput = null;
      let strategyUsed = '';
      
      // 策略1: 找弹窗中所有输入框，选择第二个（素材名称第一，素材ID第二）
      try {
        const allInputs = materialDialog.locator('.el-form-item input.el-input__inner, .el-form-item textarea');
        const inputCount = await allInputs.count();
        log(`        找到 ${inputCount} 个表单输入框`);
        
        if (inputCount >= 2) {
          idInput = allInputs.nth(1); // 第二个输入框应该是素材ID
          await idInput.waitFor({ state: 'visible', timeout: 3000 });
          strategyUsed = '第二个表单输入框';
        }
      } catch (e1) {
        log(`        策略1失败: ${e1.message}`);
      }
      
      // 策略2: 通过 JavaScript 精确查找
      if (!idInput) {
        try {
          log(`        尝试通过 JavaScript 查找素材ID输入框...`);
          const inputInfo = await materialDialog.evaluate(() => {
            const inputs = Array.from(document.querySelectorAll('input, textarea'));
            for (let i = 0; i < inputs.length; i++) {
              const input = inputs[i];
              const placeholder = input.getAttribute('placeholder') || '';
              const label = input.labels && input.labels[0] ? input.labels[0].textContent : '';
              
              // 检查 placeholder 或 label 是否包含"素材ID"
              if ((placeholder.includes('素材') && placeholder.includes('ID')) ||
                  (label.includes('素材') && label.includes('ID'))) {
                // 返回一个唯一标识，用于定位
                input.setAttribute('data-material-id-input', 'true');
                return {
                  found: true,
                  placeholder,
                  label,
                  index: i
                };
              }
            }
            return { found: false };
          });
          
          if (inputInfo.found) {
            log(`        找到素材ID输入框: placeholder="${inputInfo.placeholder}", index=${inputInfo.index}`);
            idInput = materialDialog.locator('[data-material-id-input="true"]');
            await idInput.waitFor({ state: 'visible', timeout: 2000 });
            strategyUsed = 'JavaScript精确匹配';
          }
        } catch (e2) {
          log(`        策略2失败: ${e2.message}`);
        }
      }
      
      // 策略3: 简单粗暴，找第二个 input
      if (!idInput) {
        try {
          idInput = materialDialog.locator('input').nth(1);
          await idInput.waitFor({ state: 'visible', timeout: 2000 });
          strategyUsed = '第二个input元素';
        } catch (e3) {
          log(`        策略3失败: ${e3.message}`);
        }
      }
      
      if (!idInput) {
        throw new Error('无法定位到素材ID输入框。请手动输入素材ID并点击搜索，然后按Enter继续');
      }
      
      log(`        ✓ 定位成功: ${strategyUsed}`);
      
      // 🔥 关键修复：先激活输入框，确保搜索功能完全就绪
      log(`        激活输入框...`);
      await idInput.click(); // 点击聚焦
      await page.waitForTimeout(200);
      await idInput.fill(' '); // 先输入一个空格，触发输入框初始化
      await page.waitForTimeout(300); // 等待输入框完全激活
      await idInput.fill(''); // 清空
      await page.waitForTimeout(200);
      log(`        ✓ 输入框已激活`);
      
      // 多个ID用换行符连接
      const idsText = materialIds.join('\n');
      log(`        填入 ${materialIds.length} 个素材ID`);
      
      // 稳健的填入策略：填入 → 验证 → 触发
      let fillSuccess = false;
      for (let fillAttempt = 1; fillAttempt <= 2; fillAttempt++) {
        // 1. 填入内容（输入框已在上面激活）
        await idInput.fill(idsText);
        await page.waitForTimeout(200);
        
        // 2. 验证填入是否成功
        const filledValue = await idInput.inputValue();
        const filledLines = filledValue.split(/[\n\r]+/).filter(Boolean).length;
        
        if (filledLines === materialIds.length) {
          fillSuccess = true;
          log(`        ✓ 素材ID已正确填入（${filledLines}/${materialIds.length}）`);
          break;
        } else {
          log(`        ⚠️  素材ID填入不完整（${filledLines}/${materialIds.length}），重试第${fillAttempt}次`, 'WARN');
          if (fillAttempt < 2) {
            await page.waitForTimeout(500); // 等待更长时间再重试
          }
        }
      }
      
      if (!fillSuccess) {
        log(`        ⚠️  素材ID填入可能不完整，继续尝试搜索`, 'WARN');
      }
      
      // 3. 触发 blur 和 change 事件，确保内容生效
      await idInput.evaluate(el => {
        el.blur();
        el.dispatchEvent(new Event('change', { bubbles: true }));
        el.dispatchEvent(new Event('input', { bubbles: true }));
      });
      // 智能等待：第1个账户900ms，其他账户200ms
      await page.waitForTimeout(syncWaitTime);
      log(`        已等待 ${syncWaitTime}ms 确保数据同步${isFirstAccount ? '（第1个账户）' : ''}`);
    }

    // 点击搜索按钮
    const searchBtn = materialDialog.locator('button').filter({ hasText: '搜索' });
    await searchBtn.click();
    log(`        已点击搜索按钮，等待结果...`);
    
    // 优化：智能等待搜索结果，而不是固定1500ms+500ms
    if (config.advanced.useIntelligentWait) {
      // 等待表格行出现或加载提示消失（最多等待3000ms，给搜索更多时间）
      const tableRows = materialDialog.locator('.el-table__body-wrapper .el-table__row');
      await Promise.race([
        tableRows.first().waitFor({ state: 'attached', timeout: 3000 }).catch(() => {}),
        page.waitForTimeout(3000),
      ]);
      await page.waitForTimeout(300); // 增加等待，确保搜索完全完成
    } else {
      await page.waitForTimeout(1500); // 等待搜索结果加载
      await page.waitForLoadState('networkidle', { timeout: 5000 }).catch(() => {});
      await page.waitForTimeout(500);
    }

    // 检查搜索结果数量
    const tableRows = materialDialog.locator('.el-table__body-wrapper .el-table__row');
    const rowCount = await tableRows.count();
    
    if (rowCount === 0) {
      const searchTerm = useKeyword ? `关键词"${materialKeyword}"` : `${materialIds.length}个素材ID`;
      log(`    ⚠️  搜索${searchTerm}无结果`, 'WARN');
      await pauseForUser(`素材搜索${searchTerm}无结果，请检查后手动选择`);
    } else {
      log(`    搜索到 ${rowCount} 个素材`);
      
      // 对素材ID搜索进行严格验证：结果数量应该与输入的ID数量一致（允许±45%误差）
      if (useIds) {
        const expectedCount = materialIds.length;
        const deviation = Math.abs(rowCount - expectedCount) / expectedCount;
        
        if (deviation > 0.45) { // 超过45%误差
          log(`    ⚠️  搜索结果数量异常！`, 'WARN');
          log(`    预期: ${expectedCount} 个素材, 实际: ${rowCount} 个素材`, 'WARN');
          log(`    这可能表示素材ID搜索失败，返回了默认结果列表`, 'WARN');
          
          // 关闭当前弹窗后再抛出错误（避免重试时出现两个弹窗）
          try {
            log(`    关闭素材选择弹窗以便重试...`, 'WARN');
            const closeBtn = materialDialog.locator('.el-dialog__headerbtn, .el-dialog__close').first();
            await closeBtn.click({ timeout: 2000 });
            await shortDelay();
          } catch (closeErr) {
            // 关闭失败也继续，重试时会尝试处理
            log(`    关闭弹窗失败，继续重试`, 'WARN');
          }
          
          // 抛出错误以触发自动重试
          throw new Error(`素材ID搜索结果数量不匹配（预期${expectedCount}个，实际${rowCount}个）`);
        } else {
          log(`    ✓ 素材ID搜索结果数量验证通过 (${rowCount}/${expectedCount})`);
        }
      }
      
      // 对关键词搜索进行内容验证（更严格）
      if (useKeyword) {
        // 验证是否包含关键词（检查前10行，更严格）
        let hasMatch = false;
        const checkCount = Math.min(rowCount, 10); // 增加到10行
        
        for (let i = 0; i < checkCount; i++) {
          try {
            const row = tableRows.nth(i);
            
            // 方式1: 获取整行文本
            let rowText = await row.innerText();
            if (rowText.includes(materialKeyword)) {
              hasMatch = true;
              log(`      验证通过: 第 ${i + 1} 行包含关键词`);
              break;
            }
            
            // 方式2: 获取素材名称列（通常是第3或第4列）
            const cells = row.locator('td');
            const cellCount = await cells.count();
            for (let j = 2; j < Math.min(cellCount, 5); j++) {
              const cellText = await cells.nth(j).innerText();
              if (cellText.includes(materialKeyword)) {
                hasMatch = true;
                log(`      验证通过: 第 ${i + 1} 行第 ${j + 1} 列包含关键词`);
                break;
              }
            }
            
            if (hasMatch) break;
          } catch (err) {
            // 忽略单行错误，继续检查下一行
          }
        }
        
        if (!hasMatch) {
          log(`      ⚠️  前 ${checkCount} 行未检测到关键词"${materialKeyword}"`, 'WARN');
          log(`      这可能表示搜索功能未生效，返回了默认列表`, 'WARN');
          
          // 🔥 关键修复：如果完全没有匹配，抛出错误触发重试（而不是继续执行选错素材）
          // 关闭弹窗以便重试
          try {
            const closeBtn = materialDialog.locator('.el-dialog__headerbtn, .el-dialog__close').first();
            await closeBtn.click({ timeout: 2000 });
            await page.waitForTimeout(300);
            log(`      关闭素材选择弹窗以便重试...`, 'WARN');
          } catch (closeErr) {
            log(`      关闭弹窗失败，继续重试`, 'WARN');
          }
          
          // 抛出错误以触发自动重试
          throw new Error(`素材关键词搜索"${materialKeyword}"无匹配结果，可能搜索未生效`);
        }
      }
      
      // 点击"本页全选"
      const selectAllBtn = materialDialog.locator('button').filter({ hasText: '本页全选' });
      await selectAllBtn.waitFor({ state: 'visible', timeout: 5000 });
      await selectAllBtn.click();
      await shortDelay();
      
      log(`    已选择 ${rowCount} 个素材`, 'OK');
    }

    // 点击确认
    const confirmBtn = materialDialog.locator('.el-dialog__footer button').filter({ hasText: '确认' }).or(
      materialDialog.locator('.el-dialog__footer .el-button--primary')
    );
    await confirmBtn.click();
    await mediumDelay();

    log(`    素材已选择`, 'OK');
  } catch (err) {
    // 重新抛出错误，让外层的 withRetry 能够捕获并自动重试
    throw err;
  }
}

/**
 * 修改分页大小
 */
async function changePageSize(page, dialog, size) {
  try {
    const pagination = dialog.locator('.el-pagination');
    const sizeSelector = pagination.locator('.el-pagination__sizes .el-select, .el-select').first();

    await sizeSelector.click();
    await shortDelay();

    // 在下拉中选择目标页数
    const dropdown = page.locator('.el-select-dropdown:visible').last();
    const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: `${size}条/页` });
    await option.click();
    await longDelay(); // 等待重新加载

    log(`    翻页设为: ${size}条/页`);
  } catch (err) {
    log(`    修改翻页失败，继续...`, 'WARN');
  }
}

// ── Step 15: 选择标题（下拉多选 + 滚动） ──

async function selectTitles(page, adGroupDialog, row, titles) {
  if (titles.length === 0) return;
  log(`    选择标题: ${titles.join(', ')}`);

  try {
    // 找到标题列的下拉选择器
    const cells = row.locator('td');
    const titleCell = cells.filter({ hasText: '请选择' }).first().or(cells.nth(4));

    // 点击打开下拉
    const titleSelect = titleCell.locator('.el-select, .el-input').first();
    await titleSelect.click();
    await mediumDelay();

    // 获取下拉列表
    const dropdown = page.locator('.el-select-dropdown:visible').last();
    await dropdown.waitFor({ state: 'visible', timeout: 5000 });

    for (const title of titles) {
      log(`      查找标题: ${title}`);

      // 策略1：如果下拉支持搜索，直接输入过滤
      const searchInput = titleSelect.locator('input').first();
      let searchSuccess = false;
      try {
        await searchInput.fill('', { timeout: 1000 });
        await page.waitForTimeout(200);
        await searchInput.fill(title);
        await page.waitForTimeout(800); // 等待过滤结果
        searchSuccess = true;
        log(`      通过搜索过滤: ${title}`);
      } catch {
        // 不支持搜索，跳过
      }

      // 查找匹配的选项
      let targetOption = dropdown.locator('.el-select-dropdown__item').filter({ hasText: title }).first();

      // 策略2：如果选项不可见，使用JavaScript滚动查找
      let found = false;
      try {
        await targetOption.waitFor({ state: 'visible', timeout: 2000 });
        found = true;
      } catch {
        // 选项不可见，需要滚动
        log(`      滚动查找: ${title}`);
        
        // JavaScript滚动方式（更可靠）
        found = await page.evaluate((titleText) => {
          const dropdown = document.querySelector('.el-select-dropdown:not([style*="display: none"])');
          if (!dropdown) return false;
          
          // 找滚动容器
          const scrollWrap = dropdown.querySelector('.el-scrollbar__wrap') || 
                           dropdown.querySelector('.el-select-dropdown__wrap') ||
                           dropdown.querySelector('.el-select-dropdown__list');
          
          if (!scrollWrap) return false;
          
          const allOptions = Array.from(dropdown.querySelectorAll('.el-select-dropdown__item'));
          
          // 滚动查找
          for (let i = 0; i < 30; i++) {
            // 检查当前可见的选项
            const visibleOption = allOptions.find(opt => 
              opt.textContent?.includes(titleText) && opt.offsetParent !== null
            );
            
            if (visibleOption) {
              visibleOption.scrollIntoView({ block: 'center' });
              return true;
            }
            
            // 向下滚动
            scrollWrap.scrollTop += 120;
            
            // 等待一下（同步延时）
            const start = Date.now();
            while (Date.now() - start < 100) {}
          }
          
          return false;
        }, title);
      }

      if (!found) {
        await pauseForUser(`找不到标题选项: ${title}\n请手动选择后按Enter继续`);
      } else {
        // 重新定位并点击
        targetOption = dropdown.locator('.el-select-dropdown__item').filter({ hasText: title }).first();
        try {
          await targetOption.scrollIntoViewIfNeeded();
          await targetOption.click();
          await page.waitForTimeout(300);
          log(`      已选: ${title}`, 'OK');
        } catch (err) {
          await pauseForUser(`点击标题失败: ${title}\n错误: ${err.message}\n请手动选择`);
        }
      }
    }

    // 关闭下拉（按ESC键）
    await page.keyboard.press('Escape');
    await shortDelay();

    log(`    标题选择完成`, 'OK');
  } catch (err) {
    // 重新抛出错误，让外层的 withRetry 能够捕获并自动重试
    throw err;
  }
}

// scrollToFindOption 函数已合并到 selectTitles 中

// ── Step 16: 选择优化目标 ──

async function selectOptimizationTarget(page, adGroupDialog, row, target) {
  log(`    选择优化目标: ${target}`);

  try {
    const cells = row.locator('td');
    // 优化目标列（约第6列）
    const targetCell = cells.filter({ hasText: '请选择' }).first().or(
      cells.nth(5)
    );

    const targetSelect = targetCell.locator('.el-select, .el-input').first();
    await targetSelect.click();
    await shortDelay();

    const dropdown = page.locator('.el-select-dropdown:visible').last();
    const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: target });
    await option.waitFor({ state: 'visible', timeout: 5000 });
    await option.click();
    await shortDelay();

    log(`    优化目标: ${target}`, 'OK');
  } catch (err) {
    // 重新抛出错误，让外层的 withRetry 能够捕获并自动重试
    throw err;
  }
}

// ── Step 17: 输入出价和预算 ──

async function inputBidAndBudget(page, adGroupDialog, row, bid, budget, optimizationTarget) {
  log(`    输入出价: ${bid}, 预算: ${budget}`);

  // ===== 风险控制：验证出价和预算是否在安全范围内 =====
  const bidNum = parseFloat(bid);
  const budgetNum = parseFloat(budget);
  
  // 验证预算范围（50-5000）
  if (budgetNum < 50) {
    log(`    ⚠️  预算 ${budget} 低于最低限制 50，已调整为 50`, 'WARN');
    budget = 50;
  } else if (budgetNum > 5000) {
    log(`    ❌ 预算 ${budget} 超过最高限制 5000！`, 'ERROR');
    await pauseForUser(`预算 ${budget} 超过安全上限 5000，请在Excel中修改后重新运行，或手动输入后按Enter继续`);
  }
  
  // 验证出价范围（根据优化目标）
  if (optimizationTarget === '价值') {
    if (bidNum < 1.1) {
      log(`    ⚠️  优化目标为"价值"时，出价 ${bid} 低于最低限制 1.1，已调整为 1.1`, 'WARN');
      bid = 1.1;
    }
    // 价值目标无上限
  } else if (optimizationTarget === '转化') {
    if (bidNum > 1.3) {
      log(`    ❌ 优化目标为"转化"时，出价 ${bid} 超过最高限制 1.3！`, 'ERROR');
      await pauseForUser(`出价 ${bid} 超过安全上限 1.3（转化目标），请在Excel中修改后重新运行，或手动输入后按Enter继续`);
    }
    // 转化目标无下限
  }
  
  log(`    ✓ 风险控制检查通过: 出价=${bid}, 预算=${budget}, 目标=${optimizationTarget}`);

  try {
    // 出价和预算是两个相邻的数字输入框
    // 从截图看，出价在第8列左右，预算在第9列
    const inputs = row.locator('.el-input__inner, input[type="number"], input');
    const allInputs = await inputs.all();

    // 找到出价和预算输入框 —— 它们通常是行内最后几个可输入的 input
    // 策略：找到 placeholder 包含 "美元" 或数字输入框
    let bidInput = null;
    let budgetInput = null;

    // 尝试通过列位置定位
    const cells = row.locator('td');
    const cellCount = await cells.count();

    // 从截图分析列顺序：账户(0), 项目(1), 链接(2), 素材(3), 标题(4), 优化目标(5), 认证身份(6), 出价(7), 预算(8)
    // 但实际列顺序可能不同，尝试通过文本识别
    for (let c = 0; c < cellCount; c++) {
      const cell = cells.nth(c);
      const input = cell.locator('.el-input__inner, input').first();
      const hasInput = await input.isVisible().catch(() => false);
      if (!hasInput) continue;

      // 检查是否是数字类型或美元占位符
      const placeholder = await input.getAttribute('placeholder').catch(() => '');
      const cellText = await cell.innerText().catch(() => '');

      if (cellText.includes('美元') || placeholder?.includes('美元') || placeholder?.includes('出价')) {
        if (!bidInput) bidInput = input;
        else if (!budgetInput) budgetInput = input;
      }
    }

    // 如果通过文本没找到，用列位置
    if (!bidInput) {
      bidInput = cells.nth(7).locator('.el-input__inner, input').first();
    }
    if (!budgetInput) {
      budgetInput = cells.nth(8).locator('.el-input__inner, input').first();
    }

    // 输入出价
    await bidInput.click();
    await bidInput.fill('');
    await shortDelay();
    await bidInput.fill(String(bid));
    await shortDelay();

    // 🔍 验证出价
    await validateInputValue(bidInput, '出价', bid);

    // 输入预算
    await budgetInput.click();
    await budgetInput.fill('');
    await shortDelay();
    await budgetInput.fill(String(budget));
    await shortDelay();

    // 🔍 验证预算
    await validateInputValue(budgetInput, '预算', budget);

    log(`    出价: ${bid}, 预算: ${budget}`, 'OK');
  } catch (err) {
    // 重新抛出错误，让外层的 withRetry 能够捕获并自动重试
    throw err;
  }
}

// ── Step 18: 选择开始时间 ──

async function selectStartTime(page, adGroupDialog, row, date, time) {
  log(`    选择开始时间: ${date} ${time}`);

  try {
    const cells = row.locator('td');
    // 开始时间列（约第10列）
    const timeCell = cells.filter({ hasText: '北京时间' }).first().or(
      cells.nth(9)
    );

    // 点击日期选择器
    const datePicker = timeCell.locator('.el-date-editor, .el-input').first();
    await datePicker.click();
    await mediumDelay();

    // 等待日期选择器弹出面板
    const pickerPanel = page.locator('.el-picker-panel:visible').last();
    await pickerPanel.waitFor({ state: 'visible', timeout: 5000 });

    // 方式1：直接在输入框中输入日期和时间
    const dateInputs = pickerPanel.locator('.el-date-picker__editor-wrap input, .el-input__inner, input');
    const dateInput = dateInputs.first();
    const timeInput = dateInputs.nth(1);

    // 设置日期
    await dateInput.click({ clickCount: 3 }); // 全选
    await dateInput.fill(date);
    await page.waitForTimeout(300);

    // 设置时间
    try {
      await timeInput.click({ clickCount: 3 });
      await timeInput.fill(time);
      await page.waitForTimeout(300);
    } catch {
      // 时间输入可能在不同位置
      const timeInputAlt = pickerPanel.locator('input').last();
      await timeInputAlt.click({ clickCount: 3 });
      await timeInputAlt.fill(time);
      await page.waitForTimeout(300);
    }

    // 嵌套的日期时间选择器需要点击两次"确定"
    // 1. 点击时间层的"确定"（在时间选择器的右上角或内部）
    const allConfirmBtns = pickerPanel.locator('button').filter({ hasText: '确定' });
    const confirmBtnCount = await allConfirmBtns.count();
    
    if (confirmBtnCount > 1) {
      log(`      找到 ${confirmBtnCount} 个确定按钮，依次点击`);
      
      // 第1个确定：时间层的确定（右上角小确定）
      await allConfirmBtns.first().click();
      await page.waitForTimeout(400);
      log(`      已点击时间确定`);
      
      // 第2个确定：日期层的确定（右下角大确定）
      // 重新定位（DOM可能更新）
      const dateConfirmBtn = page.locator('.el-picker-panel:visible button').filter({ hasText: '确定' }).last();
      await dateConfirmBtn.click();
      await page.waitForTimeout(400);
      log(`      已点击日期确定`);
    } else {
      // 只有1个确定按钮，直接点击
      await allConfirmBtns.first().click();
      await page.waitForTimeout(400);
    }

    log(`    开始时间: ${date} ${time}`, 'OK');
  } catch (err) {
    // 重新抛出错误，让外层的 withRetry 能够捕获并自动重试
    throw err;
  }
}

// ── 可选：选择年龄 ──

async function selectAge(page, adGroupDialog, row, age) {
  log(`    选择年龄: ${age}`);

  try {
    const cells = row.locator('td');
    // 年龄列（约第11列）
    const ageCell = cells.filter({ hasText: '18+' }).or(cells.filter({ hasText: '25+' })).first().or(
      cells.nth(10)
    );

    const ageSelect = ageCell.locator('.el-select, .el-input').first();
    await ageSelect.click();
    await shortDelay();

    const dropdown = page.locator('.el-select-dropdown:visible').last();
    const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: age });
    await option.click();
    await shortDelay();

    log(`    年龄: ${age}`, 'OK');
  } catch (err) {
    log(`    设置年龄失败，使用默认值`, 'WARN');
  }
}

// ── 可选：选择认证身份 ──

async function selectIdentity(page, adGroupDialog, row, identity) {
  log(`    选择认证身份: ${identity}`);

  try {
    const cells = row.locator('td');
    // 认证身份列（索引6）：账户(0), 项目(1), 链接(2), 素材(3), 标题(4), 优化目标(5), 认证身份(6), 出价(7), 预算(8)
    const identityCell = cells.nth(6);

    const identitySelect = identityCell.locator('.el-select, .el-input').first();
    await identitySelect.click();
    await shortDelay();

    const dropdown = page.locator('.el-select-dropdown:visible').last();
    // 精确匹配：Excel 需填完整选项文本（如 默认、Drama World、CoffeeShort）
    const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: new RegExp(`^\\s*${escapeRegExp(identity)}\\s*$`) });
    await option.waitFor({ state: 'visible', timeout: 5000 });
    await option.click();
    await shortDelay();

    log(`    认证身份: ${identity}`, 'OK');
  } catch (err) {
    log(`    设置认证身份失败: ${err.message}，使用默认值`, 'WARN');
  }
}

function escapeRegExp(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// ============================================================
//  Step 20: 提交任务
// ============================================================

async function submitTask(page) {
  log('提交任务...', 'STEP');

  // 多种方式查找提交按钮
  let submitBtn = null;
  let clickSuccess = false;

  // 策略1：通过JavaScript查找并点击
  try {
    clickSuccess = await page.evaluate(() => {
      const allButtons = Array.from(document.querySelectorAll('button'));
      const submitButtons = allButtons.filter(btn => {
        const text = btn.innerText?.trim() || btn.textContent?.trim();
        const isVisible = btn.offsetParent !== null;
        return isVisible && (text === '提交' || text === '提 交');
      });
      
      console.log(`找到 ${submitButtons.length} 个"提交"按钮`);
      
      if (submitButtons.length > 0) {
        // 通常提交按钮在右上角
        submitButtons[0].scrollIntoView({ behavior: 'auto', block: 'center' });
        submitButtons[0].click();
        return true;
      }
      return false;
    });
    
    if (clickSuccess) {
      await page.waitForTimeout(500);
      log('提交按钮已点击（JavaScript）', 'OK');
    }
  } catch (err) {
    log(`JavaScript点击失败: ${err.message}`);
  }

  // 策略2：Playwright定位
  if (!clickSuccess) {
    try {
      submitBtn = page.locator('button').filter({ hasText: '提交' }).first();
      await submitBtn.waitFor({ state: 'visible', timeout: 5000 });
      await submitBtn.scrollIntoViewIfNeeded();
      await submitBtn.click({ force: true });
      await page.waitForTimeout(500);
      clickSuccess = true;
      log('提交按钮已点击（Playwright）', 'OK');
    } catch (err) {
      log(`Playwright点击失败: ${err.message}`);
    }
  }

  // 如果都失败了，暂停
  if (!clickSuccess) {
    await page.screenshot({ path: './debug-submit.png', fullPage: true });
    log('已保存调试截图: debug-submit.png');
    await pauseForUser('找不到"提交"按钮，请手动点击提交');
  }

  // 可能会有确认弹窗
  try {
    await page.waitForTimeout(800);
    const confirmDialog = page.locator('.el-message-box:visible, .el-dialog:visible').filter({ hasText: '确认' });
    if (await confirmDialog.isVisible({ timeout: 3000 })) {
      const okBtn = confirmDialog.locator('button').filter({ hasText: '确定' });
      await okBtn.click();
      await page.waitForTimeout(500);
      log('已确认提交');
    }
  } catch {}

  // 等待提交完成（优化）
  if (config.advanced.skipNetworkIdle) {
    await page.waitForLoadState('domcontentloaded').catch(() => {});
    await mediumDelay();
  } else {
    await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(500);
  }

  log('任务已提交', 'OK');
}

// ============================================================
//  任务执行器 - 串联所有步骤
// ============================================================

/**
 * 执行单个任务组
 * @param {boolean} isFirst 是否是第一个任务（第一个不需要刷新页面）
 */
async function executeTaskGroup(page, taskGroup, index, total, isFirst = false) {
  const header = `任务 ${index + 1}/${total} [${taskGroup.taskId}] ${taskGroup.entity}/${taskGroup.projectName}`;
  console.log('\n' + '═'.repeat(60));
  log(header, 'STEP');
  console.log('═'.repeat(60));

  // 第一个任务不刷新（用户已经手动准备好了页面）
  // 后续任务需要刷新页面以清除上一个任务的状态
  if (!isFirst) {
    log('刷新页面准备下一个任务...');
    await page.goto(config.taskUrl, { waitUntil: 'networkidle', timeout: 30000 });
    await mediumDelay();
  }

  // 清除页面弹窗/提示
  await dismissPopups(page);

  // Step 1: 选择主体（带重试）
  await withRetry(
    () => selectEntity(page, taskGroup.entity),
    '选择主体'
  );

  // 清除可能出现的弹窗
  await dismissPopups(page);

  // Step 2: 选择账户（带重试）
  await withRetry(
    () => selectAccounts(page, taskGroup.accounts),
    '选择账户'
  );

  // 📸 截图：主体和账户选择完成
  await takeScreenshot(page, taskGroup.taskId, '01-主体和账户选择');

  // 清除可能出现的弹窗
  await dismissPopups(page);

  // Steps 3-7: 推广项目设置（弹窗会在内部通过"确定"按钮关闭）（带重试）
  await withRetry(
    () => setupProject(page, taskGroup),
    '推广项目设置'
  );

  // 📸 截图：项目设置完成
  await takeScreenshot(page, taskGroup.taskId, '02-项目设置');

  // Steps 8-19: 广告组设置（弹窗会在内部通过"确定"按钮关闭）（带重试）
  await withRetry(
    () => setupAdGroup(page, taskGroup),
    '广告组设置'
  );

  // 📸 截图：广告组设置完成
  await takeScreenshot(page, taskGroup.taskId, '03-广告组设置');

  // 📸 截图：提交前最终确认
  await takeScreenshot(page, taskGroup.taskId, '04-提交前最终确认');

  // Step 20: 提交（支持连续多次提交同一内容）
  const submitCount = taskGroup.submitCount || 1;
  if (submitCount > 1) {
    log(`本任务将连续提交 ${submitCount} 次（间隔2秒）`);
  }
  
  for (let i = 0; i < submitCount; i++) {
    if (submitCount > 1) {
      log(`第 ${i + 1}/${submitCount} 次提交...`);
    }
    
    // 提交任务（带重试）
    await withRetry(
      () => submitTask(page),
      `提交任务${submitCount > 1 ? `（第${i + 1}次）` : ''}`
    );
    
    // 📸 截图：提交成功后
    if (i === 0) {
      await takeScreenshot(page, taskGroup.taskId, '05-提交成功');
    }
    
    // 如果还有下一次提交，等待2秒
    if (i < submitCount - 1) {
      log('等待2秒后点击下一次提交...');
      await page.waitForTimeout(2000);
    }
  }

  console.log('═'.repeat(60));
  log(`${header} - 完成！`, 'OK');
  console.log('═'.repeat(60) + '\n');
}

/**
 * 优雅关闭浏览器上下文
 * 设置正常退出标记,避免下次启动时出现"恢复页面"提示
 */
async function closeBrowserGracefully(context) {
  try {
    // 1. 先关闭所有页面
    const pages = context.pages();
    for (const page of pages) {
      try {
        await page.close();
      } catch (err) {
        // 忽略关闭单个页面的错误
      }
    }
    
    // 2. 等待一下,让浏览器有时间清理
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 3. 关闭上下文
    await context.close();
    
    // 4. 设置正常退出标记（关闭后）
    const profileDir = path.resolve(__dirname, '..', config.browser.profileDir);
    try {
      const prefsFile = path.join(profileDir, 'Default', 'Preferences');
      if (fs.existsSync(prefsFile)) {
        let prefs = fs.readFileSync(prefsFile, 'utf8');
        // 标记为正常退出
        prefs = prefs.replace(/"exit_type"\s*:\s*"[^"]*"/, '"exit_type":"Normal"');
        prefs = prefs.replace(/"exited_cleanly"\s*:\s*false/, '"exited_cleanly":true');
        fs.writeFileSync(prefsFile, prefs, 'utf8');
      }
    } catch (err) {
      // 设置标记失败不影响主流程
    }
    
    log('浏览器已安全关闭', 'OK');
  } catch (err) {
    // 关闭过程的错误可以忽略
    log('浏览器关闭完成', 'INFO');
  }
}

module.exports = {
  launchBrowser,
  preparePage,
  executeTaskGroup,
  closeBrowserGracefully,
};


