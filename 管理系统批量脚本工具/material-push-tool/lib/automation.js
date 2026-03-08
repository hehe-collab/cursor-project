/**
 * 浏览器自动化逻辑
 */

// ⚠️ 重要：必须在require('playwright')之前设置浏览器路径
const path = require('path');
process.env.PLAYWRIGHT_BROWSERS_PATH = path.resolve(__dirname, '..', 'pw-browsers');

const { chromium } = require('playwright');
const config = require('../config');
const { log, delay, pauseForUser, withRetry } = require('./utils');

/**
 * 启动浏览器（持久化上下文，保持登录状态）
 */
async function launchBrowser() {
  log('正在启动浏览器...', 'STEP');
  
  const context = await chromium.launchPersistentContext(config.browserDataDir, {
    headless: config.headless,
    slowMo: config.slowMo,
    viewport: { width: 1920, height: 1080 },
    args: [
      '--start-maximized',
      '--disable-blink-features=AutomationControlled',
    ],
  });
  
  const page = context.pages()[0] || await context.newPage();
  
  log('✅ 浏览器启动成功', 'OK');
  
  return { context, page };
}

/**
 * 导航到素材页面
 */
async function navigateToMaterialPage(page) {
  log(`导航到素材页面: ${config.materialUrl}`, 'STEP');
  
  await page.goto(config.materialUrl, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout.navigation,
  });
  
  // 等待页面加载完成
  await page.waitForTimeout(2000);
  
  log('✅ 页面加载完成', 'OK');
}

/**
 * 选择源账户
 */
async function selectSourceAccount(page, accountId) {
  log(`选择源账户: ${accountId}`, 'STEP');
  
  // 定位账户ID输入框（placeholder="请选择"）
  const accountInput = page.locator('input[placeholder="请选择"]').first();
  
  // 清空并输入 ⚡ 优化：300ms → 100ms
  await accountInput.click();
  await page.waitForTimeout(100);
  await accountInput.fill('');
  await accountInput.fill(accountId);
  
  log(`  账户ID已输入，等待下拉列表...`);
  
  // 等待下拉列表出现 ⚡ 优化：400ms → 200ms
  await page.waitForTimeout(200);
  
  // 🔧 简化：直接使用最稳定的选择器（策略2）
  const option = page.locator('.el-select-dropdown__item, .el-autocomplete-suggestion li').filter({ hasText: accountId }).first();
  await option.waitFor({ state: 'visible', timeout: 5000 });
  
  // 点击选项
  await option.click();
  log(`  已点击下拉选项`);
  
  await page.waitForTimeout(200); // ⚡ 优化：500ms → 200ms
  
  log(`✅ 源账户已选择: ${accountId}`, 'OK');
}

/**
 * 搜索素材
 */
async function searchMaterial(page, keyword) {
  log(`搜索素材关键词: ${keyword}`, 'STEP');
  
  // 定位素材名称输入框（尝试多种placeholder）
  let keywordInput;
  
  // 策略1: placeholder="支持模糊搜索"
  try {
    keywordInput = page.locator('input[placeholder*="模糊"]').first();
    await keywordInput.waitFor({ state: 'visible', timeout: 3000 });
    log(`  找到素材名称输入框（模糊搜索）`);
  } catch (e) {
    // 策略2: placeholder包含"素材"
    try {
      keywordInput = page.locator('input[placeholder*="素材"]').filter({ hasNotText: 'ID' }).first();
      await keywordInput.waitFor({ state: 'visible', timeout: 3000 });
      log(`  找到素材名称输入框（包含"素材"）`);
    } catch (e2) {
      // 策略3: 第三个输入框
      keywordInput = page.locator('.el-input__inner').nth(2);
      await keywordInput.waitFor({ state: 'visible', timeout: 3000 });
      log(`  使用第三个输入框`);
    }
  }
  
  // 激活输入框（防止Vue组件未初始化）⚡ 优化：减少等待时间
  await keywordInput.click();
  await keywordInput.fill(' ');
  await page.waitForTimeout(100);
  await keywordInput.fill('');
  await page.waitForTimeout(100);
  
  // 输入关键词
  await keywordInput.fill(keyword);
  log(`  关键词已输入: ${keyword}`);
  
  // 点击搜索按钮
  const searchBtn = page.locator('button').filter({ hasText: '搜索' }).first();
  await searchBtn.click();
  log(`  已点击搜索按钮`);
  
  // 等待搜索结果加载
  await page.waitForTimeout(config.delay.afterSearch);
  
  log(`✅ 搜索完成`, 'OK');
}

/**
 * 全选当前页素材
 */
async function selectAllMaterials(page) {
  log(`全选当前页素材`, 'STEP');
  
  // 定位表头的全选checkbox
  const selectAllCheckbox = page.locator('thead .el-checkbox__input').first();
  
  // 检查是否已选中
  const isChecked = await selectAllCheckbox.locator('input[type="checkbox"]').isChecked();
  
  if (!isChecked) {
    await selectAllCheckbox.click();
    await page.waitForTimeout(500);
  }
  
  // 验证是否选中了素材
  const selectedCount = await page.locator('tbody .el-checkbox__input.is-checked').count();
  
  if (selectedCount === 0) {
    throw new Error('未选中任何素材，请检查页面状态');
  }
  
  log(`✅ 已选中 ${selectedCount} 个素材`, 'OK');
  
  return selectedCount;
}

/**
 * 批量同步素材
 */
async function batchSync(page, targetAccountIds) {
  log(`批量同步到 ${targetAccountIds.length} 个账户`, 'STEP');
  
  // 点击"批量同步"按钮
  const syncBtn = page.locator('button').filter({ hasText: '批量同步' }).first();
  await syncBtn.click();
  log(`  已点击批量同步按钮`);
  
  // 等待弹窗出现 ⚡ 优化：1500ms → 800ms
  await page.waitForTimeout(800);
  
  // 简单直接：在可见的弹窗中找textarea
  const accountInput = page.locator('.el-dialog:visible textarea').first();
  await accountInput.waitFor({ state: 'visible', timeout: 5000 });
  log(`  找到账户输入框`);
  
  // 清空并输入账户ID ⚡ 优化：减少等待时间
  await accountInput.click();
  await page.waitForTimeout(150);
  await accountInput.fill('');
  await page.waitForTimeout(100);
  
  const accountIdsText = targetAccountIds.join('\n');
  await accountInput.fill(accountIdsText);
  log(`  已填入 ${targetAccountIds.length} 个账户ID: ${targetAccountIds.join(', ')}`);
  
  await page.waitForTimeout(300);
  
  // 点击确定按钮（多种策略）
  let clicked = false;
  
  // 策略1: 在可见弹窗中找"确定"按钮
  try {
    const confirmBtn = page.locator('.el-dialog:visible button.el-button--primary').first();
    await confirmBtn.waitFor({ state: 'visible', timeout: 3000 });
    await confirmBtn.click();
    clicked = true;
    log(`  已点击确定按钮（策略1）`);
  } catch (e) {
    log(`  策略1失败，尝试策略2...`, 'WARN');
  }
  
  // 策略2: 直接找包含"确定"文字的按钮
  if (!clicked) {
    try {
      const confirmBtn = page.locator('button').filter({ hasText: /^确定$/ }).first();
      await confirmBtn.waitFor({ state: 'visible', timeout: 3000 });
      await confirmBtn.click();
      clicked = true;
      log(`  已点击确定按钮（策略2）`);
    } catch (e) {
      log(`  策略2失败，尝试策略3...`, 'WARN');
    }
  }
  
  // 策略3: 强制点击
  if (!clicked) {
    const confirmBtn = page.locator('.el-dialog__footer button').filter({ hasText: '确定' }).first();
    await confirmBtn.click({ force: true });
    log(`  已点击确定按钮（策略3-强制）`);
  }
  
  // 等待弹窗消失 ⚡ 优化：3000ms → 使用配置的延迟
  await page.waitForTimeout(config.delay.afterPush);
  
  log(`✅ 批量同步完成`, 'OK');
}

/**
 * 检查是否有下一页
 */
async function hasNextPage(page) {
  try {
    // 定位下一页按钮
    const nextPageBtn = page.locator('button.btn-next').first();
    
    // 检查是否存在且可点击
    const isVisible = await nextPageBtn.isVisible();
    if (!isVisible) {
      return false;
    }
    
    const isDisabled = await nextPageBtn.isDisabled();
    return !isDisabled;
  } catch (err) {
    log(`  检查下一页状态出错: ${err.message}`, 'WARN');
    return false;
  }
}

/**
 * 翻到下一页
 */
async function goToNextPage(page) {
  log(`翻到下一页`, 'STEP');
  
  // ⭐ 方案A：翻页前记录当前第一个素材ID，翻页后等待内容真正改变
  
  // 1. 记录翻页前的第一个素材ID（作为标记）
  const oldFirstMaterialId = await page.locator('tbody tr').first()
    .locator('td').nth(1).textContent()
    .catch(() => '');
  
  log(`  翻页前第一个素材ID: ${oldFirstMaterialId.substring(0, 20)}...`);
  
  // 2. 点击下一页按钮
  const nextPageBtn = page.locator('button.btn-next').first();
  await nextPageBtn.click();
  
  // 3. ⭐ 关键：等待页面内容真正改变（而不是固定等待时间）
  try {
    await page.waitForFunction((oldId) => {
      const newFirstRow = document.querySelector('tbody tr');
      if (!newFirstRow) return false;  // 表格还没加载
      
      const newFirstIdCell = newFirstRow.querySelector('td:nth-child(2)');
      if (!newFirstIdCell) return false;  // 单元格还没加载
      
      const newFirstId = newFirstIdCell.textContent || '';
      
      // 确保：1) 内容变了  2) 不是空的
      return newFirstId !== oldId && newFirstId.trim() !== '';
    }, oldFirstMaterialId, { timeout: 10000 });
    
    const newFirstMaterialId = await page.locator('tbody tr').first()
      .locator('td').nth(1).textContent()
      .catch(() => '');
    
    log(`  翻页后第一个素材ID: ${newFirstMaterialId.substring(0, 20)}...`);
    log(`  ✅ 页面内容已改变`);
    
  } catch (err) {
    log(`  ⚠️ 等待页面改变超时，继续执行`, 'WARN');
  }
  
  // 4. 再等一小段时间确保DOM完全渲染和稳定
  await page.waitForTimeout(800);
  
  log(`✅ 已翻页`, 'OK');
}

/**
 * 执行单个推送任务
 * @param {Page} page - Playwright页面对象
 * @param {Object} task - 任务对象
 * @param {number} taskIndex - 任务索引
 * @param {number} totalTasks - 总任务数
 * @param {boolean} isParallel - 是否并行模式（并行模式下不需要刷新页面）
 */
async function executeTask(page, task, taskIndex, totalTasks, isParallel = false) {
  console.log('\n');
  log('╔═══════════════════════════════════════════════════════════╗', 'STEP');
  log(`║  任务 ${taskIndex + 1}/${totalTasks}：${task.taskId}`, 'STEP');
  log('╚═══════════════════════════════════════════════════════════╝', 'STEP');
  
  // Step 1: 导航到素材页面（首次或刷新）
  // 注意：并行模式下，每个任务在独立标签页中运行，不需要刷新
  if (!isParallel && taskIndex > 0) {
    await withRetry(
      () => navigateToMaterialPage(page),
      '导航到素材页面'
    );
  }
  
  // Step 2: 选择源账户
  await withRetry(
    () => selectSourceAccount(page, task.sourceAccountId),
    '选择源账户'
  );
  
  // Step 3: 搜索素材
  await withRetry(
    () => searchMaterial(page, task.materialKeyword),
    '搜索素材'
  );
  
  // Step 4: 循环处理每一页
  let pageNum = 1;
  let totalPushed = 0;
  
  while (true) {
    log(`\n────── 处理第 ${pageNum} 页 ──────`, 'STEP');
    
    // Step 4a: 全选当前页
    const selectedCount = await withRetry(
      () => selectAllMaterials(page),
      `全选第${pageNum}页素材`
    );
    
    totalPushed += selectedCount;
    
    // Step 4b: 批量同步
    await withRetry(
      () => batchSync(page, task.targetAccountIds),
      `批量同步第${pageNum}页`
    );
    
    // Step 4c: 检查是否有下一页
    const hasNext = await hasNextPage(page);
    
    if (!hasNext) {
      log(`✅ 已完成所有页面推送（共 ${pageNum} 页，${totalPushed} 个素材）`, 'OK');
      break;
    }
    
    // Step 4d: 翻页
    await withRetry(
      () => goToNextPage(page),
      '翻页'
    );
    
    pageNum++;
  }
  
  log(`\n✅ 任务 ${task.taskId} 完成！`, 'OK');
  log(`  总计推送: ${totalPushed} 个素材`);
  log(`  目标账户: ${task.targetAccountIds.length} 个`);
  log(`  总页数: ${pageNum} 页`);
}

module.exports = {
  launchBrowser,
  navigateToMaterialPage,
  executeTask,
};

