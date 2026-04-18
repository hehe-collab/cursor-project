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
const { normalizeStartDate, normalizeStartTime, allowsEmptyBid, emptyBidSkipDescription } = require('./excel');

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
 * DramaBagus 自动登录（无验证码）
 * 登录页：请输入账号、请输入密码、登录按钮
 */
async function performLogin(page) {
  log('检测到未登录，尝试自动登录...');
  // 等待跳转到登录页并等待表单渲染（SPA 可能稍慢）
  await page.waitForURL(/\/login/, { timeout: 10000 }).catch(() => {});
  await delay(800);

  const username = (config.username || '').trim();
  const password = (config.password || '').trim();
  if (!username || !password) {
    log('未配置用户名或密码，请手动登录', 'WARN');
    return false;
  }

  try {
    // 优先用页面上的 placeholder 精确匹配（短剧出海管理系统登录页）
    const userInput = page.getByPlaceholder('请输入账号').or(
      page.locator('input[name="username"], input[placeholder*="账号"], input[type="text"]').first()
    );
    const pwdInput = page.getByPlaceholder('请输入密码').or(
      page.locator('input[name="password"], input[placeholder*="密码"], input[type="password"]').first()
    );
    const submitBtn = page.getByRole('button', { name: '登录' }).or(
      page.locator('button[type="submit"], button:has-text("登录")').first()
    );

    await userInput.waitFor({ state: 'visible', timeout: 8000 });
    await userInput.click();
    await userInput.fill('');
    await userInput.fill(username);
    await shortDelay();
    await pwdInput.waitFor({ state: 'visible', timeout: 3000 });
    await pwdInput.click();
    await pwdInput.fill('');
    await pwdInput.fill(password);
    await shortDelay();
    await submitBtn.click();
    log('已提交登录表单', 'OK');
    await page.waitForLoadState('domcontentloaded').catch(() => {});
    await delay(1500); // 等待跳转
    return true;
  } catch (err) {
    log(`自动登录失败: ${err.message}，请手动登录`, 'WARN');
    return false;
  }
}

function delay(ms) {
  return new Promise(r => setTimeout(r, ms));
}

/**
 * 准备页面 - 导航到批量工具页面，必要时自动登录，再等待用户确认
 */
async function preparePage(page) {
  log('导航到批量工具页面...');

  await page.goto(config.taskUrl, { waitUntil: 'domcontentloaded', timeout: 30000 });

  if (config.advanced.skipNetworkIdle) {
    await page.waitForLoadState('domcontentloaded').catch(() => {});
    await shortDelay();
  } else {
    await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
    await mediumDelay();
  }

  let currentUrl = page.url();
  const batchPath = 'tools/batch';

  // 若未在批量工具页（可能被重定向到登录页），尝试自动登录
  if (!currentUrl.includes(batchPath)) {
    const loginOk = await performLogin(page);
    if (loginOk) {
      currentUrl = page.url();
      if (!currentUrl.includes(batchPath)) {
        await page.goto(config.taskUrl, { waitUntil: 'domcontentloaded', timeout: 30000 });
        await mediumDelay();
      }
    }
  }

  if (!page.url().includes(batchPath)) {
    log('当前不在批量工具页面（可能需要登录或有弹窗）', 'WARN');
  }

  await pauseForUser(
    '浏览器已打开。请在浏览器中完成以下操作：\n' +
    '   1. 若未登录，请手动登录\n' +
    '   2. 关闭所有弹窗（如"更改密码"提示等）\n' +
    '   3. 确保已进入「批量工具」页面\n' +
    '   准备好后按 Enter 开始执行任务'
  );

  const finalUrl = page.url();
  if (!finalUrl.includes(batchPath)) {
    log('当前不在批量工具页面，正在跳转...', 'WARN');
    await page.goto(config.taskUrl, { waitUntil: config.advanced.skipNetworkIdle ? 'domcontentloaded' : 'networkidle', timeout: 30000 });
    await mediumDelay();
  }

  log('页面已就绪，开始执行任务', 'OK');
}

// ============================================================
//  验证功能：增强日志
// ============================================================

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

  const entityNameTrim = String(entityName || '').trim();
  if (!entityNameTrim) {
    log('主体名为空，跳过选择主体', 'WARN');
    return;
  }

  let select = null;
  let dropdownSelector = null;
  let optionSelector = null;

  // 策略1：Element UI
  const elSelect = page.locator('.el-select.el-select--small').first();
  if (await elSelect.isVisible().catch(() => false)) {
    select = elSelect;
    dropdownSelector = '.el-select-dropdown:visible';
    optionSelector = '.el-select-dropdown__item';
    log('  使用 Element UI 主体下拉', 'INFO');
  }

  // 策略2：Ant Design
  if (!select && (await page.locator('.ant-select').first().isVisible().catch(() => false))) {
    select = page.locator('.ant-select').first();
    dropdownSelector = '.ant-select-dropdown:visible';
    optionSelector = '.ant-select-item';
    log('  使用 Ant Design 主体下拉', 'INFO');
  }

  // 策略3：通过「主体」标签所在行找下拉（先上溯再找 .el-select / .ant-select）
  if (!select) {
    log('  尝试通过「主体」标签定位...', 'INFO');
    try {
      const label = page.getByText('主体', { exact: true }).first();
      if (await label.isVisible().catch(() => false)) {
        const row = label.locator('xpath=..');
        const trigger = row.locator('.el-select, .ant-select, [class*="select"]').first();
        if (await trigger.isVisible().catch(() => false)) {
          select = trigger;
          dropdownSelector = '.el-select-dropdown:visible, .ant-select-dropdown:visible';
          optionSelector = '.el-select-dropdown__item, .ant-select-item, [class*="option"], [class*="item"]';
        }
      }
      if (!select) {
        select = page.locator('[class*="select"]').first();
        dropdownSelector = '[class*="dropdown"]:visible';
        optionSelector = '[class*="option"], [class*="item"], li';
      }
    } catch (e) {
      log(`  标签定位异常: ${e.message}`, 'WARN');
    }
  }

  if (!select || !(await select.isVisible().catch(() => false))) {
    log('未找到主体下拉控件，请检查页面是否为 Element UI / Ant Design 或联系适配', 'ERROR');
    throw new Error('未找到主体下拉控件');
  }

  await select.click();
  await shortDelay();

  const dropdown = page.locator(dropdownSelector).last();
  const option = dropdown.locator(optionSelector).filter({ hasText: new RegExp(entityNameTrim.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')) });

  try {
    await option.waitFor({ state: 'visible', timeout: 10000 });
    await option.click();
    await mediumDelay();
    log(`主体已选择: ${entityNameTrim}`, 'OK');
  } catch (err) {
    log(`选择主体失败: ${err.message}`, 'ERROR');
    log(`请确认 Excel「主体」为页面选项之一（如 启量、鼎河）`, 'WARN');
    throw err;
  }
}

// ============================================================
//  Step 2: 选择账户（通过数字ID检索）
// ============================================================

async function selectAccounts(page, accounts) {
  log(`选择账户 (${accounts.length} 个)`, 'STEP');

  let dropdownSelector = '.el-select-dropdown:visible';
  let optionSelector = '.el-select-dropdown__item';

  // 关键：主体是第一个 .el-select，账户是第二个，用 .first() 会点到主体导致只选到一个
  const accountSelectByLabel = page.getByText('账户', { exact: true }).first();
  let accountInput = page.locator('.el-select').nth(1).locator('.el-select__tags input.el-select__input, input').first();
  if (!(await accountInput.isVisible().catch(() => false))) {
    try {
      const row = accountSelectByLabel.locator('xpath=..');
      accountInput = row.locator('.el-select input, .el-select__tags input').first();
    } catch {}
  }
  if (!(await accountInput.isVisible().catch(() => false))) {
    accountInput = page.getByPlaceholder(/可多选|多选账户|名称\/ID搜索|支持.*ID/).first();
  }
  if (!(await accountInput.isVisible().catch(() => false))) {
    accountInput = page.locator('.el-select__tags input.el-select__input').first();
    log('  未找到第二个下拉，使用第一个多选输入框', 'WARN');
  }
  if (!(await accountInput.isVisible().catch(() => false))) {
    const antSearch = page.locator('.ant-select').nth(1).locator('.ant-select-selection-search-input').first();
    if (await antSearch.isVisible().catch(() => false)) {
      dropdownSelector = '.ant-select-dropdown:visible';
      optionSelector = '.ant-select-item';
      accountInput = antSearch;
      log('  使用 Ant Design 第二个下拉（账户）', 'INFO');
    }
  }
  if (!(await accountInput.isVisible().catch(() => false))) {
    log('未找到账户输入框，请确认已在批量工具页且主体已选', 'ERROR');
    throw new Error('未找到账户多选输入框');
  }
  log('  使用账户下拉（第二个 el-select）', 'INFO');

  for (let i = 0; i < accounts.length; i++) {
    const accountId = String(accounts[i].accountId || '').trim();
    if (!accountId) continue;
    log(`  搜索账户 ${i + 1}/${accounts.length}: ${accountId}`);

    // 按 Escape 关闭下拉（不用点击页面，避免误触链接跳转）
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);

    // 随机错峰延迟 (0~1.5秒)，避免并行模式下多个标签页瞬间并发压垮后端搜索接口
    const randomDelay = Math.floor(Math.random() * 1500);
    if (randomDelay > 0) await page.waitForTimeout(randomDelay);

    let found = false;
    for (let attempt = 1; attempt <= 2; attempt++) {
      if (attempt > 1) {
        log(`  🔄 第 ${attempt} 次尝试搜索账户: ${accountId}`, 'WARN');
      }

      await accountInput.click();
      await page.waitForTimeout(300);
      await accountInput.fill('');
      await page.waitForTimeout(300);
      await accountInput.fill(accountId);
      
      // 触发后端请求后给充足的时间等待第一波网络回包
      await page.waitForTimeout(1000);

      const accountIdRe = new RegExp(accountId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
      let dropdown = page.locator(dropdownSelector).filter({ hasText: accountIdRe }).first();
      if (!(await dropdown.isVisible().catch(() => false))) {
        dropdown = page.locator(dropdownSelector).last();
      }

      // 智能等待：检测加载中状态
      let isLoading = true;
      let waitCount = 0;
      const maxWait = 15; // 最多等待约 30 秒
      
      while (isLoading && waitCount < maxWait) {
        waitCount++;
        // 匹配 Element UI 和 Ant Design 的无数据/加载中提示
        const loadingText = dropdown.locator('.el-select-dropdown__empty, .ant-empty-description').filter({ hasText: /加载|搜索中|Loading|searching/i });
        if (await loadingText.isVisible().catch(() => false)) {
          log(`  ⏳ 账户列表加载中... (等待 ${waitCount * 2}s)`);
          await page.waitForTimeout(2000);
        } else {
          // 再多等一下确保渲染完成
          if (waitCount === 1) await page.waitForTimeout(1500);
          isLoading = false; 
        }
      }

      const option = dropdown.locator(optionSelector).filter({ hasText: accountIdRe }).first();

      try {
        await option.waitFor({ state: 'visible', timeout: 15000 });
        await option.click();
        await page.waitForTimeout(500);
        log(`  账户已选择: ${accountId}`, 'OK');
        logValidation(`账户${i + 1}`, accountId, accountId);
        found = true;
        break; // 成功则跳出重试
      } catch (err) {
        if (attempt === 1) {
          log(`  ⚠️ 第一次搜索未响应，准备重试清空并重新触发...`, 'WARN');
          // 按 ESC 尝试收起下拉，让组件状态重置
          await page.keyboard.press('Escape');
          await page.waitForTimeout(500);
        } else {
          log(`  未找到账户选项: ${accountId} - ${err.message}`, 'WARN');
        }
      }
    }

    if (!found) {
      await pauseForUser(`找不到账户: ${accountId}\n请手动选择该账户后按 Enter 继续`);
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
    await mediumDelay();
  } else {
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(800);
  }

  // ✅ 等待 Loading 遮罩消失（页面加载、接口请求完成）
  await waitForLoadingMaskDisappear(page);

  // 调试：仅 verboseLog 时打印（避免每次遍历 10 个元素）
  if (config.advanced.verboseLog) {
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
    await pauseForUser('找不到项目"设置"按钮，请手动点击项目区域的"设置"');
  }

  // 等待弹窗出现（DramaBagus 标题为「项目设置」，dramahub8 为「广告系列」）
  let dialog = page.locator('.el-dialog:visible').filter({ hasText: /项目设置|广告系列/ }).or(
    page.locator('.el-dialog:visible').first()
  );
  if (!(await dialog.isVisible().catch(() => false))) {
    dialog = page.locator('.ant-modal:visible').filter({ hasText: /项目设置|广告系列/ }).or(
      page.locator('.ant-modal:visible').first()
    );
    log('项目设置弹窗使用 Ant Design (.ant-modal)', 'INFO');
  }
  await dialog.waitFor({ state: 'visible', timeout: 10000 });
  log('项目设置弹窗已打开');

  // 获取表格中的所有行（Element UI .el-table 或 Ant Design .ant-table）
  let tableRows = dialog.locator('.el-table__body-wrapper .el-table__row');
  let rowCount = await tableRows.count();
  if (rowCount === 0) {
    tableRows = dialog.locator('.ant-table-tbody tr');
    rowCount = await tableRows.count();
    if (rowCount > 0) log('  表格使用 Ant Design (.ant-table)', 'INFO');
  }
  if (rowCount === 0) {
    tableRows = dialog.locator('table tbody tr');
    rowCount = await tableRows.count();
  }
  log(`  发现 ${rowCount} 个账户行`);

  const blocks = taskGroup.accountBlocks || [];
  if (blocks.length === 0) {
    throw new Error('任务组缺少 accountBlocks，请使用新版 Excel（含项目每日预算/广告组序号）并重新生成模板');
  }
  if (blocks.length !== rowCount) {
    log(`  ⚠️ Excel 账户数(${blocks.length})与弹窗表格行数(${rowCount})不一致，将按较少一侧处理`, 'WARN');
  }

  const n = Math.min(rowCount, blocks.length);
  for (let i = 0; i < n; i++) {
    const row = tableRows.nth(i);
    const block = blocks[i];

    log(`  设置账户 ${i + 1}/${n}: ${block.accountId}`);

    await selectPixelInRow(page, row, block.pixel);
    await fillProjectNameInRow(row, block.projectName);
    await fillProjectDailyBudgetInRow(page, dialog, row, block.projectDailyBudget);
    await toggleSmartPlusInRow(row, block.smartPlus !== false);
    await toggleEnableInRow(row, block.enable);
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
  await page.waitForTimeout(400); // 额外等待 DOM 更新

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
  const checkInterval = 200; // 200ms 轮询，减少无效等待
  
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
 * 在行内选择 Pixel（兼容 Element UI / Ant Design，DramaBagus 列顺序可能为 账户ID/账户/主体/Pixel）
 */
async function selectPixelInRow(page, row, pixelName) {
  const pixelNameTrim = String(pixelName || '').trim();
  if (!pixelNameTrim) {
    log('    Pixel 为空，跳过', 'WARN');
    return;
  }
  try {
    const cells = row.locator('td');
    // DramaBagus 弹窗列顺序：账户ID(0), 账户(1), 主体(2), Pixel(3)；原版为 账户ID(0), 账户(1), Pixel(2)
    let pixelCell = cells.nth(2);
    let pixelSelect = pixelCell.locator('.el-select, .el-input, input, [class*="select"]').first();
    if (!(await pixelSelect.isVisible().catch(() => false))) {
      pixelCell = cells.nth(3);
      pixelSelect = pixelCell.locator('.el-select, .el-input, input, [class*="select"]').first();
      log('    Pixel 位于第4列（主体在第3列）', 'INFO');
    }
    if (!(await pixelSelect.isVisible().catch(() => false))) {
      pixelSelect = pixelCell.getByPlaceholder('请选择').or(pixelCell.locator('input')).first();
    }
    if (!(await pixelSelect.isVisible().catch(() => false))) {
      await pixelCell.click();
      pixelSelect = pixelCell;
    }

    let dropdownSelector = '.el-select-dropdown:visible';
    let optionSelector = '.el-select-dropdown__item';
    if (await pixelCell.locator('.ant-select').first().isVisible().catch(() => false)) {
      dropdownSelector = '.ant-select-dropdown:visible';
      optionSelector = '.ant-select-item';
    }

    const maskDisappeared = await waitForLoadingMaskDisappear(page);
    if (!maskDisappeared) {
      log('    Loading 遮罩未消失，继续尝试点击 Pixel', 'WARN');
    }

    await pixelSelect.click();
    await shortDelay();

    const dropdown = page.locator(dropdownSelector).last();
    const escaped = pixelNameTrim.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const option = dropdown.locator(optionSelector).filter({
      hasText: new RegExp(escaped, 'i')
    });
    await option.waitFor({ state: 'visible', timeout: 8000 });
    await option.click();
    await shortDelay();

    log(`    Pixel: ${pixelNameTrim}`, 'OK');
  } catch (err) {
    log(`选择 Pixel 失败: ${pixelNameTrim} - ${err.message}`, 'ERROR');
    await pauseForUser(`选择 Pixel 失败: ${pixelNameTrim}\n错误: ${err.message}\n请手动选择后按 Enter`);
  }
}

/**
 * 在行内填写项目名称（DramaBagus 列序：…Pixel(3), 已有项目(4), 项目名称(5)，placeholder 多为「请输入项」）
 */
async function fillProjectNameInRow(row, projectName) {
  const nameTrim = String(projectName || '').trim();
  if (!nameTrim) {
    log('    项目名称为空，跳过', 'WARN');
    return;
  }
  try {
    const cells = row.locator('td');
    const inputSelectors = '.el-input__inner, input[type="text"], input:not([type="hidden"]), textarea';
    let nameInput = null;

    for (const colIndex of [5, 4, 6]) {
      const nameCell = cells.nth(colIndex);
      const inCell = nameCell.locator(inputSelectors).first();
      if (await inCell.isVisible().catch(() => false)) {
        nameInput = inCell;
        if (colIndex !== 4) log(`    项目名称在第${colIndex + 1}列`, 'INFO');
        break;
      }
    }
    if (!nameInput) {
      nameInput = row.getByPlaceholder(/请输入项|项目名称|请输入/).first();
    }
    if (!nameInput || !(await nameInput.isVisible().catch(() => false))) {
      nameInput = row.locator('input[placeholder*="请输入项"], input[placeholder*="项目名称"]').first();
    }
    if (!nameInput || !(await nameInput.isVisible().catch(() => false))) {
      log('未找到项目名称输入框', 'ERROR');
      await pauseForUser(`未找到项目名称输入框（placeholder 多为「请输入项」）\n请手动填写「${nameTrim}」后按 Enter`);
      return;
    }
    await nameInput.click();
    await nameInput.fill('');
    await shortDelay();
    await nameInput.fill(nameTrim);
    await shortDelay();
    log(`    项目名称: ${nameTrim}`, 'OK');
  } catch (err) {
    log(`填写项目名称失败: ${err.message}`, 'ERROR');
    await pauseForUser(`填写项目名称失败: ${projectName}\n错误: ${err.message}\n请手动填写后按 Enter`);
  }
}

/**
 * 项目设置弹窗：填写「每日预算(可空)」列（对应推广系列预算；可空则跳过）
 */
async function fillProjectDailyBudgetInRow(page, dialog, row, value) {
  const v = String(value || '').trim();
  if (!v) {
    log('    项目每日预算为空，跳过填写', 'INFO');
    return;
  }
  let colIdx = await getColumnIndexByHeader(dialog, '每日预算');
  if (colIdx < 0) colIdx = await getColumnIndexByHeader(dialog, '每日预算(可空)');
  if (colIdx < 0) {
    log('    未找到表头「每日预算」，跳过项目预算', 'WARN');
    return;
  }
  try {
    const cell = row.locator('td').nth(colIdx);
    const input = cell.locator('.el-input__inner, input').first();
    await input.scrollIntoViewIfNeeded().catch(() => {});
    await input.click();
    await input.fill('');
    await shortDelay();
    await input.fill(v);
    await shortDelay();
    log(`    项目每日预算: ${v}`, 'OK');
  } catch (err) {
    log(`填写项目每日预算失败: ${err.message}`, 'WARN');
    await pauseForUser(`请手动填写项目每日预算「${v}」后按 Enter`);
  }
}

/**
 * 在行内开启/关闭 Smart+（DramaBagus 需打开此开关）
 * Smart+ 是每行第1个开关，启用是第2个；兼容 .el-switch 与 .ant-switch
 */
async function toggleSmartPlusInRow(row, enable) {
  try {
    let switchEl = row.locator('.el-switch').first();
    if (!(await switchEl.isVisible().catch(() => false))) {
      switchEl = row.locator('.ant-switch').first();
    }
    await switchEl.scrollIntoViewIfNeeded().catch(() => {});
    const isChecked = await switchEl.evaluate(el => el.classList.contains('is-checked') || el.getAttribute('aria-checked') === 'true');

    if (enable !== isChecked) {
      await switchEl.click();
      await shortDelay();
    }

    log(`    Smart+: ${enable ? '开启' : '关闭'}`, 'OK');
  } catch (err) {
    await pauseForUser(`切换 Smart+ 失败\n错误: ${err.message}\n请手动打开 Smart+ 后按 Enter`);
  }
}

/**
 * 在行内开启/关闭 启用（第2个开关）
 */
async function toggleEnableInRow(row, enable) {
  try {
    let switchEl = row.locator('.el-switch').nth(1);
    if (!(await switchEl.isVisible().catch(() => false))) {
      switchEl = row.locator('.ant-switch').nth(1);
    }
    await switchEl.scrollIntoViewIfNeeded().catch(() => {});
    const isChecked = await switchEl.evaluate(el => el.classList.contains('is-checked') || el.getAttribute('aria-checked') === 'true');

    if (enable !== isChecked) {
      await switchEl.click();
      await shortDelay();
    }

    log(`    启用: ${enable ? '开启' : '关闭'}`, 'OK');
  } catch (err) {
    await pauseForUser(`切换启用状态失败\n错误: ${err.message}\n请手动切换`);
  }
}

// ============================================================
//  Steps 8-19: 广告组设置
// ============================================================

/**
 * 点击「+ 添加广告组」（同一账户区块的第 accountIndex 个按钮，0 起）
 */
async function clickAddAdGroupForAccount(page, dialog, accountIndex) {
  const btns = dialog.locator('button').filter({ hasText: /添加广告组/ });
  const n = await btns.count();
  if (accountIndex >= n) {
    await pauseForUser(`未找到第 ${accountIndex + 1} 个「添加广告组」按钮（当前 ${n} 个），请手动添加后按 Enter`);
    return;
  }
  await btns.nth(accountIndex).click();
  await mediumDelay();
  await waitForLoadingMaskDisappear(page).catch(() => {});
}

/**
 * 在广告组表格的一行内完成 Smart+ 下全部字段（单条广告组）
 */
async function fillAdGroupRow(page, dialog, row, ag, block, isFirstAccountRow) {
  const hasProjBudget = String(block.projectDailyBudget || '').trim() !== '';

  await withRetry(
    () => selectPromotionLink(page, dialog, row, ag.linkKeyword),
    `选择推广链接 [${ag.linkKeyword}]`
  );

  await withRetry(
    () => selectMaterials(page, dialog, row, ag.materialKeyword, ag.materialIds, isFirstAccountRow),
    `选择素材 [${ag.materialKeyword || `${ag.materialIds?.length}个ID`}]`
  );

  await withRetry(
    () => selectTitles(page, dialog, row, ag.titles),
    `选择标题 [${ag.titles.length}个]`
  );

  await withRetry(
    () => selectOptimizationTarget(page, dialog, row, ag.optimizationTarget),
    `选择优化目标 [${ag.optimizationTarget}]`
  );

  await withRetry(
    () => selectBiddingStrategy(page, dialog, row, ag.optimizationTarget, ag.biddingStrategy),
    `选择出价策略 [${ag.biddingStrategy || '自动匹配'}]`
  );

  await withRetry(
    () => inputBidAndBudget(page, dialog, row, ag.bid, ag.budget, ag.optimizationTarget, ag.biddingStrategy, {
      allowEmptyAdGroupBudget: hasProjBudget,
    }),
    `输入出价和预算 [${ag.bid || '(空)'}/${ag.budget || '(空)'}]`
  );

  const optTargetCurrent = await getOptimizationTargetValue(dialog, row);
  if (optTargetCurrent && !optTargetCurrent.includes(ag.optimizationTarget)) {
    log(`    ⚠️ 警告: 填完出价后，优化目标被系统弹回成了 "${optTargetCurrent}"，正在修正...`, 'WARN');
    await withRetry(
      () => selectOptimizationTarget(page, dialog, row, ag.optimizationTarget),
      `重新修正优化目标 [${ag.optimizationTarget}]`
    );
  }

  if (ag.age && ag.age !== '18+') {
    await selectAge(page, dialog, row, ag.age);
  }

  await withRetry(
    () => selectStartTime(page, dialog, row, ag.startDate, ag.startTime),
    `选择开始时间 [${ag.startDate} ${ag.startTime}]`
  );
}

async function setupAdGroup(page, taskGroup) {
  log('设置广告组...', 'STEP');

  // 确保项目设置弹窗已完全关闭
  await page.waitForTimeout(500);

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
    await pauseForUser('找不到广告组"设置"按钮，请手动点击广告组区域的"设置"');
  }

  // 验证：确保打开的是广告组设置弹窗
  await page.waitForTimeout(300);
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

  // 确保在 Smart+ 标签页（默认可能是普通广告组，需显式切换）
  const smartTab = dialog.locator('.el-tabs__item, [role="tab"]').filter({ hasText: /Smart\+/ }).first();
  try {
    if (await smartTab.isVisible()) {
      await smartTab.click();
      await page.waitForTimeout(500);
    }
  } catch {}

  const blocks = taskGroup.accountBlocks || [];
  if (blocks.length === 0) {
    throw new Error('任务组缺少 accountBlocks，请更新 Excel 模板');
  }

  let tableRows = dialog.locator('.el-table__body-wrapper .el-table__row');
  let rowCount = await tableRows.count();
  log(`  发现 ${rowCount} 个账户行（首条广告组）`);

  let firstGlobalAdGroup = true;
  const nBlocks = Math.min(blocks.length, rowCount);

  for (let i = 0; i < nBlocks; i++) {
    const block = blocks[i];
    const accountId = block.accountId;

    for (let g = 0; g < block.adGroups.length; g++) {
      const ag = block.adGroups[g];
      let row;
      if (g === 0) {
        row = tableRows.nth(i);
      } else {
        await clickAddAdGroupForAccount(page, dialog, i);
        tableRows = dialog.locator('.el-table__body-wrapper .el-table__row');
        const re = new RegExp(accountId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
        row = tableRows.filter({ hasText: re }).last();
      }

      log(`\n  ── 账户 ${i + 1}/${nBlocks} ${accountId} 广告组序号 ${ag.adGroupSeq} (${g + 1}/${block.adGroups.length}) ──`);
      await fillAdGroupRow(page, dialog, row, ag, block, firstGlobalAdGroup);
      firstGlobalAdGroup = false;
    }
  }

  // Step 19: 点击确定（时间设置完成后直接点确定，不再做其他操作）
  const confirmBtn = dialog.locator('.el-dialog__footer button').filter({ hasText: '确定' }).or(
    dialog.locator('.el-button--primary').last()
  );
  await confirmBtn.click();
  await mediumDelay();

  log('广告组设置完成', 'OK');
}

// ── Step 9-11: 选择推广链接 ──
// DramaBagus：链接列为表格内下拉（第4列 nth(3)），无「选择推广链接」弹窗，需在输入框输入后点下拉项

async function selectPromotionLink(page, adGroupDialog, row, linkKeyword) {
  const keyword = String(linkKeyword || '').trim();
  if (!keyword) {
    log('    推广链接关键词为空，跳过', 'WARN');
    return;
  }
  log(`    选择推广链接: ${keyword}`);

  await waitForLoadingMaskDisappear(page);

  try {
    // 链接列：DramaBagus 为第4列（索引3），dramahub8 为第3列（索引2）
    let linkCell = row.locator('td').nth(3);
    let linkInput = linkCell.locator('input, .el-input__inner').first();
    if (!(await linkInput.isVisible().catch(() => false))) {
      linkCell = row.locator('td').nth(2);
      linkInput = linkCell.locator('input, .el-input__inner').first();
    }
    if (!(await linkInput.isVisible().catch(() => false))) {
      linkCell = row.locator('td').filter({ hasText: /请选择|未选择|已选/ }).first();
      linkInput = linkCell.locator('input, .el-input__inner').first();
    }

    await linkInput.click();
    await shortDelay();
    await linkInput.fill('');
    await shortDelay();
    await linkInput.fill(keyword);
    await shortDelay();

    // 等待下拉出现（DramaBagus 为表格内 el-select 下拉，无二级弹窗）
    const dropdown = page.locator('.el-select-dropdown:visible, .el-autocomplete-suggestion:visible').last();
    const option = dropdown.locator('.el-select-dropdown__item, .el-autocomplete-suggestion__list li').filter({
      hasText: new RegExp(keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i')
    }).first();

    try {
      await option.waitFor({ state: 'visible', timeout: 6000 });
      await option.click();
      await mediumDelay();
      log(`    推广链接已选择`, 'OK');
      return;
    } catch (dropdownErr) {
      // 若没有内联下拉，尝试旧流程：是否弹出「选择推广链接」对话框
      const linkDialog = page.locator('.el-dialog:visible').filter({ hasText: '选择推广链接' });
      if (await linkDialog.isVisible().catch(() => false)) {
        const nameInput = linkDialog.locator('input[placeholder*="推广名称"], input[placeholder*="名称"]').or(
          linkDialog.locator('.el-input__inner').nth(1)
        );
        await nameInput.fill(keyword);
        await shortDelay();
        const searchBtn = linkDialog.locator('button').filter({ hasText: '搜索' }).or(
          linkDialog.locator('.el-button--primary').first()
        );
        await searchBtn.click();
        await longDelay();
        const addBtn = linkDialog.locator('text=添加').first().or(
          linkDialog.locator('.el-table__body-wrapper .el-table__row').first().locator('text=添加')
        );
        await addBtn.waitFor({ state: 'visible', timeout: 8000 });
        await addBtn.click();
        await shortDelay();
        const confirmBtn = linkDialog.locator('.el-dialog__footer button').filter({ hasText: '确认' }).or(
          linkDialog.locator('.el-dialog__footer .el-button--primary')
        );
        await confirmBtn.click();
        await mediumDelay();
        log(`    推广链接已选择（弹窗）`, 'OK');
        return;
      }
      throw dropdownErr;
    }
  } catch (err) {
    throw err;
  }
}

// ── Step 12-14: 选择素材 ──

async function selectMaterials(page, adGroupDialog, row, materialKeyword, materialIds = [], isFirstAccount = false) {
  // 素材关键词优先，如果没有则使用素材ID
  const useKeyword = !!materialKeyword;
  const useIds = !useKeyword && materialIds.length > 0;
  
  // 根据是否是第一个账户，动态调整等待时间（表格行已就绪后，无需长等）
  // 第1个账户：300ms；其他账户：150ms
  const syncWaitTime = isFirstAccount ? 300 : 150;
  
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

    // 方式2：素材列为第5列（索引4）：账户0、主体1、项目2、链接3、素材4
    if (!clicked) {
      try {
        const materialCell = row.locator('td').nth(4);
        const clickableElements = materialCell.locator('button, .el-button, i, svg, span').first();
        await clickableElements.click({ timeout: 3000 });
        clicked = true;
      } catch {}
    }

    // 方式3：直接点击素材列（第5列）
    if (!clicked) {
      await row.locator('td').nth(4).click();
    }

    await mediumDelay();
    await page.waitForTimeout(400);

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

    // 优化：素材列表在绿字出现前就已展示，直接等表格行出现即可，无需等绿字/loading 消失
    try {
      await materialDialog.locator('.el-table__body-wrapper .el-table__row').first().waitFor({ state: 'visible', timeout: 3000 });
      log(`      素材列表已就绪`);
    } catch {
      // 超时也继续，可能列表为空或结构不同
    }

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
      
      // 🔥 关键修复：先激活输入框，确保搜索功能完全就绪（合并多次等待为一次）
      log(`        激活输入框...`);
      await idInput.click();
      await idInput.fill(' ');
      await idInput.fill('');
      await page.waitForTimeout(400); // 合并原 200+300+200ms
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
      await page.waitForTimeout(1000);
      await page.waitForLoadState(config.advanced.skipNetworkIdle ? 'domcontentloaded' : 'networkidle', { timeout: 5000 }).catch(() => {});
      await page.waitForTimeout(300);
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

    // 点击确认（DramaBagus 弹窗文案为「确定」）
    const confirmBtn = materialDialog.locator('.el-dialog__footer button').filter({ hasText: /确认|确定/ }).or(
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
 * 修改分页大小（可选：选择素材弹窗若无分页则快速跳过，避免长时间停滞）
 */
async function changePageSize(page, dialog, size) {
  const quickTimeout = 2500; // 2.5 秒内找不到分页则跳过，避免素材弹窗停滞 30 秒
  try {
    const pagination = dialog.locator('.el-pagination');
    const sizeSelector = pagination.locator('.el-pagination__sizes .el-select, .el-select').first();

    await sizeSelector.click({ timeout: quickTimeout });
    await shortDelay();

    const dropdown = page.locator('.el-select-dropdown:visible').last();
    const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: `${size}条/页` });
    await option.click({ timeout: quickTimeout });
    await longDelay();

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
    // 找到标题列的下拉选择器。表头：账户0 主体1 项目2 链接3 素材4 标题5；不用「请选择」避免点到链接列
    const cells = row.locator('td');
    const titleCell = cells.nth(5).or(cells.nth(4));

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
      try {
        await searchInput.fill('', { timeout: 1000 });
        await page.waitForTimeout(200);
        await searchInput.fill(title);
        await page.waitForTimeout(300); // 等待过滤结果
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
        // 选项不可见，需要滚动。先清空搜索框恢复完整列表（搜索可能把选项过滤掉了）
        try {
          await searchInput.fill('');
          await page.waitForTimeout(400);
        } catch { /* ignore */ }
        log(`      滚动查找: ${title}`);
        
        // JavaScript：先尝试直接 scrollIntoView（选项在 DOM 中但不在视口内）
        found = await page.evaluate((titleText) => {
          const dropdowns = Array.from(document.querySelectorAll('.el-select-dropdown'));
          const dropdown = dropdowns.filter(d => d.offsetParent !== null && getComputedStyle(d).display !== 'none').pop();
          if (!dropdown) return false;
          
          const allOptions = Array.from(dropdown.querySelectorAll('.el-select-dropdown__item'));
          const opt = allOptions.find(o => (o.textContent || '').trim().includes(titleText));
          if (opt) {
            opt.scrollIntoView({ block: 'center', behavior: 'auto' });
            return true;
          }
          
          // 选项可能在虚拟列表外，需滚动容器查找
          const scrollWrap = dropdown.querySelector('.el-scrollbar__wrap') || 
                           dropdown.querySelector('.el-select-dropdown__wrap') ||
                           dropdown.querySelector('.el-select-dropdown__list');
          if (!scrollWrap) return false;
          
          // 先滚到顶部（印尼16 等可能在列表上方）
          scrollWrap.scrollTop = 0;
          const maxScroll = scrollWrap.scrollHeight - scrollWrap.clientHeight;
          
          for (let i = 0; i < 50; i++) {
            const visible = allOptions.find(opt => {
              const rect = opt.getBoundingClientRect();
              const wrapRect = scrollWrap.getBoundingClientRect();
              return (opt.textContent || '').includes(titleText) && 
                     rect.top >= wrapRect.top - 20 && rect.bottom <= wrapRect.bottom + 20;
            });
            if (visible) {
              visible.scrollIntoView({ block: 'center' });
              return true;
            }
            scrollWrap.scrollTop = Math.min(scrollWrap.scrollTop + 100, maxScroll);
            if (scrollWrap.scrollTop >= maxScroll - 5) break;
          }
          
          // 再尝试向上滚（从底部往顶部）
          scrollWrap.scrollTop = maxScroll;
          for (let i = 0; i < 30; i++) {
            const visible = allOptions.find(opt => {
              const rect = opt.getBoundingClientRect();
              const wrapRect = scrollWrap.getBoundingClientRect();
              return (opt.textContent || '').includes(titleText) && 
                     rect.top >= wrapRect.top - 20 && rect.bottom <= wrapRect.bottom + 20;
            });
            if (visible) {
              visible.scrollIntoView({ block: 'center' });
              return true;
            }
            scrollWrap.scrollTop = Math.max(scrollWrap.scrollTop - 100, 0);
            if (scrollWrap.scrollTop <= 5) break;
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

/** 根据表头文案在弹窗表格中解析列索引（避免固定 nth 因列顺序/滚动导致点错列） */
async function getColumnIndexByHeader(dialog, headerText) {
  const selectors = [
    '.el-table thead th',
    '.el-table__header th',
    '.el-table__header-wrapper th'
  ];
  const needle = String(headerText || '').trim();
  for (const sel of selectors) {
    const ths = dialog.locator(sel);
    const n = await ths.count();
    for (let i = 0; i < n; i++) {
      const t = await ths.nth(i).textContent().catch(() => '');
      if (t && t.trim().includes(needle)) return i;
    }
  }
  return -1;
}

// ── 获取当前优化目标显示值（用于回马枪验证） ──
async function getOptimizationTargetValue(adGroupDialog, row) {
  try {
    const cells = row.locator('td');
    let colIndex = await getColumnIndexByHeader(adGroupDialog, '优化目标');
    if (colIndex < 0) colIndex = 6;
    const targetCell = cells.nth(colIndex);
    const input = targetCell.locator('input').first();
    if (await input.isVisible()) {
      return await input.inputValue();
    }
    return await targetCell.innerText();
  } catch {
    return null;
  }
}

// ── Step 16: 选择优化目标 ──

async function selectOptimizationTarget(page, adGroupDialog, row, target) {
  // 仅支持「价值」或「转化」
  let normalized = String(target || '').trim();
  if (normalized !== '价值' && normalized !== '转化') {
    log(`    优化目标仅支持"价值"或"转化"，当前"${target}"已改为"价值"`, 'WARN');
    normalized = '价值';
  }
  log(`    选择优化目标: ${normalized}`);

  try {
    const cells = row.locator('td');
    // 优先按表头「优化目标」解析列索引，避免列顺序/滚动导致点错
    let colIndex = await getColumnIndexByHeader(adGroupDialog, '优化目标');
    if (colIndex < 0) colIndex = 6;
    log(`    优化目标列索引: ${colIndex}`);
    const targetCell = cells.nth(colIndex);

    await targetCell.scrollIntoViewIfNeeded().catch(() => {});
    await page.waitForTimeout(400);

    const targetSelect = targetCell.locator('.el-select, .el-input').first();
    // 先尝试普通点击，失败则用 JS 直接触发点击（避免被遮挡或事件未绑定）
    try {
      await targetSelect.click({ timeout: 3000 });
    } catch {
      await targetSelect.evaluate(el => { if (el) el.click(); });
    }
    await shortDelay();

    // 只认「内容里包含“价值”选项」的下拉，避免误用标题/链接的下拉；若无则用最后一个可见下拉
    let dropdown = page.locator('.el-select-dropdown:visible').filter({ hasText: /价值|转化/ }).first();
    if (!(await dropdown.isVisible().catch(() => false))) {
      dropdown = page.locator('.el-select-dropdown:visible').last();
    }
    await dropdown.waitFor({ state: 'visible', timeout: 6000 }).catch(() => {});
    const option = dropdown.locator('.el-select-dropdown__item').filter({
      hasText: new RegExp(`^\\s*${normalized.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`)
    }).or(dropdown.getByText(normalized, { exact: true })).first();
    
    await option.waitFor({ state: 'visible', timeout: 8000 });
    await option.scrollIntoViewIfNeeded().catch(() => {});
    await page.waitForTimeout(200);
    
    // 强力点击：不仅触发 click，同时派发原生事件，确保 Vue 监听到变化
    try {
      await option.click({ timeout: 3000 });
    } catch {
      await option.evaluate(el => { 
        if (el) {
          el.click();
          // 强制触发更新，防止被吞
          el.dispatchEvent(new Event('mousedown', { bubbles: true }));
          el.dispatchEvent(new Event('mouseup', { bubbles: true }));
        }
      });
    }
    await page.waitForTimeout(500); // 多等一会，让系统联动反应完

    log(`    优化目标: ${normalized}`, 'OK');
  } catch (err) {
    throw err;
  }
}

// ── Step 16b: 选择出价策略（依赖优化目标）──
// 价值 → 目标ROAS / 最高价值；转化 → 最大化投放 / 成本上限(CPA)

async function selectBiddingStrategy(page, adGroupDialog, row, optimizationTarget, biddingStrategy) {
  const target = String(optimizationTarget || '').trim();
  const validForValue = ['目标ROAS', '最高价值'];
  const validForConversion = ['最大化投放', '成本上限(CPA)', '成本上限（CPA）'];

  let strategy = String(biddingStrategy || '').trim();
  if (!strategy) {
    strategy = target === '转化' ? '成本上限(CPA)' : '目标ROAS';
    log(`    选择出价策略: ${strategy}（默认）`);
  } else {
    // 兼容 Excel 多种写法
    const lower = strategy.replace(/\s/g, '').toLowerCase();
    if (lower.includes('目标') && lower.includes('roas')) strategy = '目标ROAS';
    else if (lower.includes('最高价值')) strategy = '最高价值';
    else if (lower.includes('最大化投放')) strategy = '最大化投放';
    else if (lower.includes('成本上限') || lower.includes('cpa')) strategy = '成本上限(CPA)';
    log(`    选择出价策略: ${strategy}`);
  }

  try {
    const cells = row.locator('td');
    // 与优化目标一致：按表头「出价策略」解析列索引，避免固定 nth 导致点错或点不到
    let colIndex = await getColumnIndexByHeader(adGroupDialog, '出价策略');
    if (colIndex < 0) colIndex = 7;
    log(`    出价策略列索引: ${colIndex}`);
    const strategyCell = cells.nth(colIndex);

    await strategyCell.scrollIntoViewIfNeeded().catch(() => {});
    await page.waitForTimeout(400);
    const select = strategyCell.locator('.el-select, .el-input').first();
    try {
      await select.click({ timeout: 3000 });
    } catch {
      await select.evaluate(el => { if (el) el.click(); });
    }
    await shortDelay();

    // 只认包含「目标ROAS/最高价值/成本上限」等选项的下拉，避免误用其他下拉
    let dropdown = page.locator('.el-select-dropdown:visible').filter({
      hasText: /目标ROAS|最高价值|最大化投放|成本上限/
    }).first();
    if (!(await dropdown.isVisible().catch(() => false))) {
      dropdown = page.locator('.el-select-dropdown:visible').last();
    }
    await dropdown.waitFor({ state: 'visible', timeout: 6000 }).catch(() => {});
    const option = dropdown.locator('.el-select-dropdown__item').filter({
      hasText: new RegExp(strategy.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i')
    }).first();
    await option.waitFor({ state: 'visible', timeout: 5000 });
    await option.scrollIntoViewIfNeeded().catch(() => {});
    await page.waitForTimeout(200);
    try {
      await option.click({ timeout: 3000 });
    } catch {
      await option.evaluate(el => { if (el) el.click(); });
    }
    await shortDelay();
    log(`    出价策略: ${strategy}`, 'OK');
  } catch (err) {
    throw err;
  }
}

// ── Step 17: 输入出价和预算 ──
// 界面列名为「出价值」即出价。「转化+最大化投放」「价值+最高价值」可不出价（跳过填格子）；否则价值≥1.1 / 转化已填≤1.3。预算≤5000

async function inputBidAndBudget(page, adGroupDialog, row, bid, budget, optimizationTarget, biddingStrategy, options = {}) {
  const opt = String(optimizationTarget || '').trim();
  const bidStr = String(bid ?? '').trim();
  const bidIsEmpty = bidStr === '';
  const skipBidInput = allowsEmptyBid(opt, biddingStrategy) && bidIsEmpty;
  const skipDesc = emptyBidSkipDescription(opt, biddingStrategy);

  const budgetStr = String(budget ?? '').trim();
  const skipBudgetInput = options.allowEmptyAdGroupBudget === true && budgetStr === '';

  log(`    输入出价: ${skipBidInput ? `(${skipDesc}，跳过)` : bidStr}, 预算: ${skipBudgetInput ? '(项目已填每日预算，广告组预算可空)' : budgetStr}`);

  // ===== 风险控制：验证出价和预算是否在安全范围内 =====
  const bidNum = bidIsEmpty ? NaN : parseFloat(bidStr);
  const budgetNum = skipBudgetInput ? NaN : parseFloat(budgetStr);

  let budgetFill = budgetStr;
  // 预算最高不得高于 5000
  if (!skipBudgetInput && !Number.isNaN(budgetNum) && budgetNum < 50) {
    log(`    ⚠️  预算 ${budgetStr} 低于最低限制 50，已调整为 50`, 'WARN');
    budgetFill = '50';
  } else if (!skipBudgetInput && !Number.isNaN(budgetNum) && budgetNum > 5000) {
    log(`    ❌ 预算 ${budgetStr} 超过最高限制 5000！`, 'ERROR');
    await pauseForUser(`预算 ${budgetStr} 超过安全上限 5000，请在Excel中修改后重新运行，或手动输入后按Enter继续`);
  }
  
  // 验证出价范围（根据优化目标）
  if (opt === '价值') {
    if (skipBidInput) {
      // 价值+最高价值且未填出价：不校验、不填出价
    } else if (bidIsEmpty || Number.isNaN(bidNum)) {
      log(`    ❌ 优化目标为「价值」且非「最高价值」策略时必须填写出价`, 'ERROR');
      await pauseForUser(`优化目标为「价值」且出价策略不是「最高价值」时，必须填写出价。请在 Excel 中填写后重新运行，或手动填写后按 Enter`);
      throw new Error('价值目标（非最高价值策略）必须填写出价');
    } else if (bidNum < 1.1) {
      log(`    ⚠️  优化目标为"价值"时，出价 ${bid} 低于最低限制 1.1，已调整为 1.1`, 'WARN');
      bid = 1.1;
    }
    // 价值目标无上限
  } else if (opt === '转化') {
    if (skipBidInput) {
      // 转化+最大化投放且未填出价：不校验、不填出价
    } else if (bidIsEmpty) {
      log(`    ❌ 「转化」且非「最大化投放」时必须填写出价`, 'ERROR');
      await pauseForUser(`当前为「转化」且出价策略不是「最大化投放」，必须填写出价。请在 Excel 中填写后重试，或手动填写后按 Enter`);
      throw new Error('转化且非最大化投放时必须填写出价');
    } else if (!Number.isNaN(bidNum) && bidNum > 1.3) {
      // 「转化时 bid > 1.3」：指已填写出价时，转化目标下出价不得超过 1.3（脚本安全上限）
      log(`    ❌ 优化目标为"转化"时，出价 ${bid} 超过最高限制 1.3！`, 'ERROR');
      await pauseForUser(`出价 ${bid} 超过安全上限 1.3（转化目标），请在Excel中修改后重新运行，或手动输入后按Enter继续`);
    }
    // 转化目标无下限（已填出价时）
  }
  
  log(`    ✓ 风险控制检查通过: 出价=${skipBidInput ? '(未填)' : bid}, 预算=${skipBudgetInput ? '(未填)' : budgetFill}, 目标=${opt}`);

  try {
    const cells = row.locator('td');
    // 表头顺序：…出价策略(7) 出价值(8) 预算(9)，之前用 nth(7)/nth(8) 导致预算100被填到出价值列
    let bidCol = await getColumnIndexByHeader(adGroupDialog, '出价值');
    if (bidCol < 0) bidCol = await getColumnIndexByHeader(adGroupDialog, '出价');
    if (bidCol < 0) bidCol = 8;
    let budgetCol = await getColumnIndexByHeader(adGroupDialog, '预算');
    if (budgetCol < 0) budgetCol = await getColumnIndexByHeader(adGroupDialog, '每日预算');
    if (budgetCol < 0) budgetCol = 9;

    const bidInput = cells.nth(bidCol).locator('.el-input__inner, input').first();
    const budgetInput = cells.nth(budgetCol).locator('.el-input__inner, input').first();

    if (skipBidInput) {
      log(`    跳过填写出价（页面可留空）`, 'OK');
    } else {
      await cells.nth(bidCol).scrollIntoViewIfNeeded().catch(() => {});
      await page.waitForTimeout(200);

      // 先填出价
      await bidInput.click();
      await page.waitForTimeout(150);
      await bidInput.fill('');
      await page.waitForTimeout(100);
      await bidInput.fill(String(bid));
      await page.waitForTimeout(250);

      await validateInputValue(bidInput, '出价', bid);
    }

    if (skipBudgetInput) {
      log(`    跳过填写广告组预算（已在项目层填写每日预算）`, 'OK');
    } else {
      // 再填预算（稍作间隔，避免焦点/值串列）
      await cells.nth(budgetCol).scrollIntoViewIfNeeded().catch(() => {});
      await page.waitForTimeout(skipBidInput ? 0 : 100);
      await budgetInput.click();
      await page.waitForTimeout(150);
      await budgetInput.fill('');
      await page.waitForTimeout(100);
      await budgetInput.fill(String(budgetFill));
      await page.waitForTimeout(250);

      await validateInputValue(budgetInput, '预算', budgetFill);
    }

    // 填完预算后立即 blur，避免焦点落到下一列「优化事件」
    await page.evaluate(() => { document.activeElement?.blur?.(); });
    // 多等一会，给系统时间执行联动逻辑（比如我们填了出价1.1，系统如果校验不通过，这时候就会把转化强行变回价值）
    await page.waitForTimeout(600);

    log(`    出价: ${skipBidInput ? '(未填)' : bid}, 预算: ${skipBudgetInput ? '(未填)' : budgetFill}`, 'OK');
  } catch (err) {
    // 重新抛出错误，让外层的 withRetry 能够捕获并自动重试
    throw err;
  }
}

// ── Step 18: 选择开始时间 ──

/**
 * 轮询等待输入框值包含预期内容（替代固定延迟）
 * @param {import('playwright').Locator} locator - 输入框定位器
 * @param {string|((v: string) => boolean)} expected - 预期字符串或判断函数
 * @param {number} timeoutMs - 超时毫秒
 * @returns {Promise<boolean>} 是否匹配成功
 */
async function waitForInputToMatch(locator, expected, timeoutMs = 2000) {
  const matcher = typeof expected === 'function' ? expected : (v) => v && v.includes(expected);
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      const v = await locator.inputValue();
      if (matcher(v)) return true;
    } catch {}
    await locator.page().waitForTimeout(50);
  }
  return false;
}

async function selectStartTime(page, adGroupDialog, row, date, time) {
  const dateStr = normalizeStartDate(date);
  const timeStr = normalizeStartTime(time);
  log(`    选择开始时间: ${dateStr} ${timeStr}`);

  try {
    // 若优化事件下拉已打开，先点击弹窗标题关闭，避免后续误触
    const optEventDropdown = page.locator('.el-select-dropdown:visible').filter({ hasText: /付费|加购物车|发起结账|查看内容/ });
    if (await optEventDropdown.isVisible().catch(() => false)) {
      await adGroupDialog.locator('.el-dialog__header').first().click({ force: true }).catch(() => {});
      await page.waitForTimeout(150);
    }

    // 优先用「含 .el-date-editor 且不含 .el-select」的单元格定位，避免点到优化事件列
    let timeCell = row.locator('td').filter({ has: row.locator('.el-date-editor'), hasNot: page.locator('.el-select') }).first();
    if (await timeCell.count().catch(() => 0) === 0) {
      timeCell = row.locator('td').filter({ has: row.locator('.el-date-editor') }).first();
    }
    if (await timeCell.count().catch(() => 0) === 0) {
      const optCol = await getColumnIndexByHeader(adGroupDialog, '优化事件');
      const startCol = await getColumnIndexByHeader(adGroupDialog, '开始时间');
      let colIdx = startCol >= 0 ? startCol : 10;
      if (optCol >= 0 && colIdx <= optCol) colIdx = optCol + 1;
      timeCell = row.locator('td').nth(colIdx);
    }
    const cellText = await timeCell.textContent().catch(() => '');
    if (/付费|加购物车|发起结账|查看内容/.test(cellText)) {
      const startCol = await getColumnIndexByHeader(adGroupDialog, '开始时间');
      const optCol = await getColumnIndexByHeader(adGroupDialog, '优化事件');
      const colIdx = (startCol >= 0 ? startCol : (optCol >= 0 ? optCol + 1 : 11));
      timeCell = row.locator('td').nth(colIdx);
    }
    await timeCell.scrollIntoViewIfNeeded().catch(() => {});

    // 优先尝试直接填写：多数日期时间输入框支持直接输入，比弹窗更可靠
    const combinedValue = `${dateStr} ${timeStr}`;
    for (const sel of ['input', '.el-input__inner', 'input[type="text"]']) {
      const directInput = timeCell.locator(sel).first();
      if (await directInput.isVisible().catch(() => false)) {
        try {
          await directInput.click();
          await directInput.fill('');
          await directInput.fill(combinedValue);
          await page.evaluate(() => { document.activeElement?.blur?.(); });
          await shortDelay();
          const filled = await directInput.inputValue();
          if (filled && (filled.includes(dateStr) && filled.includes(timeStr.split(':')[0]))) {
            log(`    开始时间: ${dateStr} ${timeStr}（直接填写）`, 'OK');
            return;
          }
        } catch {}
        break; // 直接填写失败，改用弹窗
      }
    }

    // 弹窗方式：点击打开选择器
    const datePicker = timeCell.locator('.el-date-editor, .el-input').first();
    const suffixIcon = timeCell.locator('.el-date-editor .el-input__suffix, .el-input__suffix .el-icon-date').first();
    try {
      await datePicker.click({ timeout: 3000 });
    } catch {
      await datePicker.evaluate(el => { if (el) el.click(); });
    }
    await page.waitForTimeout(200);
    let pickerVisible = await page.locator('.el-picker-panel:visible, .el-date-picker:visible').last().waitFor({ state: 'visible', timeout: 3000 }).then(() => true).catch(() => false);
    if (!pickerVisible && await suffixIcon.isVisible().catch(() => false)) {
      await suffixIcon.click();
      await page.waitForTimeout(200);
      pickerVisible = await page.locator('.el-picker-panel:visible, .el-date-picker:visible').last().waitFor({ state: 'visible', timeout: 3000 }).then(() => true).catch(() => false);
    }
    if (!pickerVisible) {
      throw new Error('日期选择器未弹出且直接填写未生效');
    }

    const pickerPanel = page.locator('.el-picker-panel:visible, .el-date-picker:visible').last();

    // 尝试在弹窗内直接填写合并值（部分 Element UI 弹窗有可编辑的合并输入框）
    const pickerInputs = pickerPanel.locator('input[type="text"], .el-input__inner');
    const inputCount = await pickerInputs.count();
    let pickerFillOk = false;
    if (inputCount >= 1) {
      const firstInput = pickerInputs.first();
      try {
        await firstInput.click({ clickCount: 3 });
        await firstInput.fill('');
        await firstInput.fill(combinedValue);
        await waitForInputToMatch(firstInput, dateStr, 1500);
        const v = await firstInput.inputValue();
        if (v && v.includes(dateStr)) {
          const [h] = timeStr.split(':');
          if (v.includes(h)) pickerFillOk = true;
        }
      } catch {}
    }

    if (!pickerFillOk && inputCount >= 2) {
      const dateInput = pickerInputs.first();
      const timeInput = pickerInputs.nth(1);
      const timeShort = timeStr.slice(0, 5); // HH:mm，部分组件只认此时长

      await dateInput.click({ clickCount: 3 });
      await dateInput.fill(dateStr);
      await waitForInputToMatch(dateInput, dateStr, 2000) || page.waitForTimeout(100);

      try {
        await timeInput.click({ clickCount: 3 });
        await timeInput.fill(timeStr);
        let matched = await waitForInputToMatch(timeInput, (v) => v && (v.includes(timeStr.slice(0, 5)) || v.includes(timeStr)), 1500);
        if (!matched) {
          await timeInput.fill(timeShort);
          matched = await waitForInputToMatch(timeInput, (v) => v && v.includes(timeShort), 1000);
        }
        if (!matched) await page.waitForTimeout(100);
      } catch {
        const timeInputAlt = pickerPanel.locator('input').last();
        await timeInputAlt.click({ clickCount: 3 });
        await timeInputAlt.fill(timeShort);
        await waitForInputToMatch(timeInputAlt, (v) => v && v.includes(timeShort), 1500) || page.waitForTimeout(100);
      }
    }

    // 日期时间选择器有两个「确定」：先点时间区右侧的确定，再点底部总确定
    const timeSectionConfirm = pickerPanel.locator('.el-time-panel button').filter({ hasText: '确定' }).first();
    const footerConfirm = pickerPanel.locator('.el-picker-panel__footer button, .el-date-picker__footer button').filter({ hasText: '确定' }).first();
    const anyConfirm = pickerPanel.locator('button').filter({ hasText: '确定' });

    if (await timeSectionConfirm.isVisible().catch(() => false)) {
      log(`      先点击时间区确定`);
      await timeSectionConfirm.click();
      // 等待时间面板关闭或主输入框更新（替代固定 300ms）
      const timePanel = pickerPanel.locator('.el-time-panel');
      await Promise.race([
        timePanel.waitFor({ state: 'hidden', timeout: 2000 }).catch(() => {}),
        page.waitForTimeout(2000),
      ]);
    }
    if (await footerConfirm.isVisible().catch(() => false)) {
      await footerConfirm.click();
    } else {
      const btnCount = await anyConfirm.count();
      if (btnCount > 1) {
        await anyConfirm.last().click();
      } else if (btnCount === 1) {
        await anyConfirm.first().click();
      }
    }

    // 等待选择器完全关闭（替代固定延迟）
    await pickerPanel.waitFor({ state: 'hidden', timeout: 3000 }).catch(() => {});
    await shortDelay();

    // 时间设置完成后不再做 blur/点击预算，直接结束；后续会点击弹窗「确定」，避免误触优化事件
    log(`    开始时间: ${dateStr} ${timeStr}`, 'OK');
  } catch (err) {
    throw err;
  }
}

// ── 可选：选择年龄 ──

async function selectAge(page, adGroupDialog, row, age) {
  log(`    选择年龄: ${age}`);

  try {
    const cells = row.locator('td');
    // 按表头「年龄」解析列索引，避免 nth(10) 误点到「优化事件」列
    let ageCol = await getColumnIndexByHeader(adGroupDialog, '年龄');
    if (ageCol < 0) ageCol = 12;
    const ageCell = cells.filter({ hasText: /18\+|25\+/ }).first().or(cells.nth(ageCol));

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
    await pauseForUser('找不到"提交"按钮，请手动点击提交');
  }

  // 可能会有确认弹窗
  try {
    await page.waitForTimeout(400);
    const confirmDialog = page.locator('.el-message-box:visible, .el-dialog:visible').filter({ hasText: '确认' });
    if (await confirmDialog.isVisible({ timeout: 3000 })) {
      const okBtn = confirmDialog.locator('button').filter({ hasText: '确定' });
      await okBtn.click();
      await page.waitForTimeout(300);
      log('已确认提交');
    }
  } catch {}

  // ⚠️ 修复：有些系统在提交后会跳转页面或刷新状态，导致后续点击失效
  // 这里不再强行等待网络闲置（因为提交可能只是发起异步请求且不刷新页面）
  // 而是等待后端返回（比如出现“提交成功”的通知，或者网络闲置）
  if (config.advanced.skipNetworkIdle) {
    await page.waitForLoadState('domcontentloaded').catch(() => {});
    await mediumDelay();
  } else {
    // 监听一下网络请求结束，但不强制 15 秒，只要没有新的网络请求就过
    await page.waitForLoadState('networkidle', { timeout: 3000 }).catch(() => {});
    await page.waitForTimeout(1000);
  }

  log('任务已提交', 'OK');
}

/**
 * 多次提交时，在最后一次 submitTask 返回之后、本任务收尾（并行下即将关标签页）之前再缓冲。
 * 可配置：先可选等成功 Toast，再执行固定 finalSettleMs（config.submit）。
 */
async function settleAfterFinalSubmit(page, submitCount) {
  if (submitCount < 2) return;

  const cfg = config.submit || {};
  const finalSettleMs = Number(cfg.finalSettleMs);
  const waitToast = cfg.waitForSuccessToast !== false;
  const toastTimeout = Number(cfg.successToastTimeoutMs) || 8000;

  log('末次提交后：等待界面稳定后再结束本任务（避免关标签页过早）', 'STEP');

  if (waitToast) {
    try {
      const toast = page
        .locator('.el-message--success, .el-message.el-message--success')
        .first();
      await toast.waitFor({ state: 'visible', timeout: toastTimeout });
      log('已检测到成功提示（Toast）', 'OK');
    } catch {
      log('未在时限内检测到成功 Toast，将按 finalSettleMs 继续等待', 'WARN');
    }
  }

  const ms = Number.isFinite(finalSettleMs) && finalSettleMs >= 0 ? finalSettleMs : 3000;
  if (ms > 0) {
    log(`末次提交后额外等待 ${ms}ms（config.submit.finalSettleMs）`);
    await page.waitForTimeout(ms);
  }
}

// ============================================================
//  任务执行器 - 串联所有步骤
// ============================================================

/**
 * 执行单个任务组
 * @param {boolean} isFirst 是否是第一个任务（第一个不需要刷新页面）
 */
async function executeTaskGroup(page, taskGroup, index, total, isFirst = false) {
  const header = `任务 ${index + 1}/${total} [${taskGroup.taskId}] ${taskGroup.entity}/${taskGroup.projectName || ''}`;
  console.log('\n' + '═'.repeat(60));
  log(header, 'STEP');
  console.log('═'.repeat(60));

  // 第一个任务不刷新（用户已经手动准备好了页面）
  // 后续任务需要刷新页面以清除上一个任务的状态
  if (!isFirst) {
    log('刷新页面准备下一个任务...');
    await page.goto(config.taskUrl, { waitUntil: config.advanced.skipNetworkIdle ? 'domcontentloaded' : 'networkidle', timeout: 30000 });
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
  const accountPick = (taskGroup.accountBlocks || []).map((b) => ({ accountId: b.accountId }));
  await withRetry(
    () => selectAccounts(page, accountPick),
    '选择账户'
  );

  // 清除可能出现的弹窗
  await dismissPopups(page);

  // Steps 3-7: 推广项目设置（弹窗会在内部通过"确定"按钮关闭）（带重试）
  await withRetry(
    () => setupProject(page, taskGroup),
    '推广项目设置'
  );

  // Steps 8-19: 广告组设置（弹窗会在内部通过"确定"按钮关闭）
  // 注意：不再用 withRetry 包住整段，否则标题/优化目标等后续步骤失败时会重试整段，又从「选择素材」开始
  await setupAdGroup(page, taskGroup);

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

    // 如果还有下一次提交，等待并在原页面继续点击
    if (i < submitCount - 1) {
      log('等待2秒后点击下一次提交...');
      await page.waitForTimeout(2000);
      
      // ⚠️ 修复：有些系统提交后必须关闭出现的确认/成功弹窗，或者重置表单才能再次提交
      await dismissPopups(page);
    }
  }

  await settleAfterFinalSubmit(page, submitCount);

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


