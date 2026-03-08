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
  
  // ⭐ 方案A：DOM稳定性检测
  await waitForDOMStable(page);
  
  // ⭐ 方案D：提取搜索结果总数
  const totalMaterialCount = await extractTotalMaterialCount(page);
  
  log(`✅ 搜索完成${totalMaterialCount ? `（共找到 ${totalMaterialCount} 个素材）` : ''}`, 'OK');
  
  return totalMaterialCount; // 返回总素材数，用于后续验证
}

/**
 * 等待DOM稳定（素材列表完全加载）
 * 方案A核心：连续检测3次素材数量，确保页面不再变化
 */
async function waitForDOMStable(page) {
  log(`  等待页面DOM稳定...`);
  
  const maxAttempts = 3;
  const checkInterval = config.delay.domStableCheck || 500;
  const maxWaitTime = config.delay.domStableMaxWait || 5000;
  let previousCount = -1;
  let stableCount = 0;
  
  const maxIterations = Math.ceil(maxWaitTime / checkInterval);
  
  for (let i = 0; i < maxIterations; i++) {
    try {
      // 获取当前页面素材行数
      const currentCount = await page.locator('tbody tr').count();
      
      if (currentCount === previousCount && currentCount > 0) {
        stableCount++;
        if (stableCount >= maxAttempts) {
          log(`  ✅ DOM已稳定（连续${maxAttempts}次检测，素材数: ${currentCount}）`);
          return;
        }
      } else {
        stableCount = 0; // 重置计数器
      }
      
      previousCount = currentCount;
      await page.waitForTimeout(checkInterval);
    } catch (err) {
      log(`  ⚠️ DOM检测出错: ${err.message}`, 'WARN');
      break;
    }
  }
  
  log(`  ⚠️ DOM稳定性检测超时，继续执行`, 'WARN');
}

/**
 * 提取搜索结果的总素材数
 * 方案D核心：从页面中读取"共XX条"素材信息
 */
async function extractTotalMaterialCount(page) {
  try {
    // 策略1: 查找包含"共"和"条"的文字（Element UI分页器格式）
    // 例如："共 30 条"
    const paginationText = await page.locator('.el-pagination__total').textContent().catch(() => '');
    
    const match = paginationText.match(/共\s*(\d+)\s*条/);
    if (match) {
      const total = parseInt(match[1]);
      log(`  📊 检测到搜索结果总数: ${total} 个`);
      return total;
    }
    
    // 策略2: 如果找不到，返回null（不影响主流程）
    log(`  ⚠️ 未能检测到搜索结果总数`, 'WARN');
    return null;
    
  } catch (err) {
    log(`  ⚠️ 提取素材总数出错: ${err.message}`, 'WARN');
    return null;
  }
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
  
  // ⭐ 新增：二次验证选中数量是否合理
  const totalRowsOnPage = await page.locator('tbody tr').count();
  const minSelectionRatio = config.validation?.minSelectionRatio || 0.8;
  
  if (selectedCount < totalRowsOnPage) {
    log(`  ⚠️ 注意：页面有 ${totalRowsOnPage} 行，但只选中了 ${selectedCount} 个`, 'WARN');
    
    // 如果差距明显，抛出错误触发重试
    if (selectedCount < totalRowsOnPage * minSelectionRatio) {
      throw new Error(`选中数量异常：应选${totalRowsOnPage}个，实际只选${selectedCount}个`);
    }
  }
  
  log(`✅ 已选中 ${selectedCount} 个素材（共${totalRowsOnPage}行）`, 'OK');
  
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
 * 提取分页信息（当前页/总页数）
 * 方案C核心：读取分页器显示的页码
 */
async function extractPageInfo(page) {
  try {
    // ===== 策略1：读取"共XX条"和"XX条/页"，计算总页数 =====
    const totalText = await page.locator('.el-pagination__total').textContent().catch(() => '');
    const sizeText = await page.locator('.el-pagination__sizes .el-input__inner, .el-select .el-input__inner').first().getAttribute('value').catch(() => '');
    
    // 提取数字："共30条" → 30
    const totalMatch = totalText.match(/共\s*(\d+)\s*条/);
    const sizeMatch = sizeText ? sizeText.match(/(\d+)/) : null;
    
    if (totalMatch) {
      const totalRecords = parseInt(totalMatch[1]);
      const pageSize = sizeMatch ? parseInt(sizeMatch[1]) : 10; // 默认10条/页
      const totalPages = Math.ceil(totalRecords / pageSize);
      
      // 读取当前页码
      const currentPageText = await page.locator('.el-pager .number.active, .el-pager .number.is-active').textContent().catch(() => '1');
      const currentPage = parseInt(currentPageText) || 1;
      
      return {
        current: currentPage,
        total: totalPages,
        totalRecords: totalRecords,
        pageSize: pageSize
      };
    }
    
    // ===== 策略2：直接读取页码按钮 =====
    const currentPageText = await page.locator('.el-pager .number.active, .el-pager .number.is-active').textContent().catch(() => '');
    const allPageTexts = await page.locator('.el-pager .number').allTextContents();
    
    if (currentPageText && allPageTexts.length > 0) {
      const current = parseInt(currentPageText);
      const total = Math.max(...allPageTexts.map(p => parseInt(p) || 0));
      
      return {
        current: current,
        total: total
      };
    }
    
    return null;
    
  } catch (err) {
    return null;
  }
}

/**
 * 检查是否有下一页（方案C：多重验证）
 */
async function hasNextPage(page) {
  try {
    // ===== 验证1：下一页按钮状态 =====
    const nextBtn = page.locator('.btn-next, button.btn-next, li.btn-next').first();
    
    const isVisible = await nextBtn.isVisible();
    if (!isVisible) {
      log(`  📍 验证1: 下一页按钮不可见`, 'INFO');
      return false;
    }
    
    // 检查是否有disabled类
    const hasDisabledClass = await nextBtn.evaluate(el => {
      return el.classList.contains('disabled') || 
             el.classList.contains('is-disabled') ||
             el.hasAttribute('disabled');
    }).catch(() => true); // 出错时保守判断为disabled
    
    if (hasDisabledClass) {
      log(`  📍 验证1: 下一页按钮已禁用`, 'INFO');
      return false;
    }
    
    // ===== 验证2：读取当前页码和总页数 =====
    const pageInfo = await extractPageInfo(page);
    if (pageInfo) {
      log(`  📍 验证2: 当前第 ${pageInfo.current} 页，共 ${pageInfo.total} 页`, 'INFO');
      
      if (pageInfo.current >= pageInfo.total) {
        log(`  📍 验证2: 已是最后一页`, 'INFO');
        return false;
      }
      
      // 如果页码显示还有下一页，就相信它
      log(`  ✅ 多重验证通过：确认有下一页`, 'OK');
      return true;
    }
    
    // ===== 验证3：对比当前高亮页码与最大页码 =====
    const currentPageText = await page.locator('.el-pager .number.active, .el-pager .number.is-active')
      .textContent().catch(() => '');
    const allPageButtons = await page.locator('.el-pager .number').allTextContents();
    
    if (currentPageText && allPageButtons.length > 0) {
      const current = parseInt(currentPageText);
      const maxPage = Math.max(...allPageButtons.map(p => parseInt(p) || 0));
      
      log(`  📍 验证3: 当前页 ${current}，最大页码 ${maxPage}`, 'INFO');
      
      if (current >= maxPage) {
        log(`  📍 验证3: 当前页已是最大页码，无下一页`, 'INFO');
        return false;
      }
    }
    
    // 默认：相信按钮状态
    log(`  ✅ 检测通过：确认有下一页`, 'OK');
    return true;
    
  } catch (err) {
    log(`  ⚠️ 检查下一页状态出错: ${err.message}`, 'WARN');
    // 出错时保守判断：假设没有下一页
    return false;
  }
}

/**
 * 翻到下一页（适配Element UI数字分页器）
 */
async function goToNextPage(page) {
  log(`翻到下一页`, 'STEP');
  
  // 1. 记录翻页前的第一个素材ID
  const oldFirstMaterialId = await page.locator('tbody tr').first()
    .locator('td').nth(1).textContent()
    .catch(() => '');
  
  log(`  翻页前第一个素材ID: ${oldFirstMaterialId.substring(0, 20)}...`);
  
  // 2. 点击下一页按钮（多种策略）
  let clicked = false;
  
  // 策略1: 使用.btn-next类（不限定标签）
  try {
    const nextBtn = page.locator('.btn-next').first();
    await nextBtn.waitFor({ state: 'visible', timeout: 3000 });
    await nextBtn.click();
    clicked = true;
    log(`  已点击下一页按钮（策略1: .btn-next）`);
  } catch (e) {
    log(`  策略1失败，尝试策略2...`, 'WARN');
  }
  
  // 策略2: 使用箭头符号定位
  if (!clicked) {
    try {
      const nextBtn = page.locator('.el-pagination button, .el-pagination li').filter({ hasText: '>' }).first();
      await nextBtn.click();
      clicked = true;
      log(`  已点击下一页按钮（策略2: 箭头符号）`);
    } catch (e) {
      log(`  策略2失败，尝试策略3...`, 'WARN');
    }
  }
  
  // 策略3: 读取当前页码，点击下一个页码按钮
  if (!clicked) {
    const currentPageNum = await page.locator('.el-pager .number.active, .el-pager .number.is-active')
      .textContent().catch(() => '');
    
    if (currentPageNum) {
      const nextPageNum = parseInt(currentPageNum) + 1;
      const nextPageBtn = page.locator('.el-pager .number').filter({ hasText: `${nextPageNum}` }).first();
      await nextPageBtn.click();
      clicked = true;
      log(`  已点击页码按钮 ${nextPageNum}（策略3: 页码按钮）`);
    }
  }
  
  if (!clicked) {
    throw new Error('无法找到下一页按钮');
  }
  
  // 3. 等待页面内容真正改变
  try {
    await page.waitForFunction((oldId) => {
      const newFirstRow = document.querySelector('tbody tr');
      if (!newFirstRow) return false;
      
      const newFirstIdCell = newFirstRow.querySelector('td:nth-child(2)');
      if (!newFirstIdCell) return false;
      
      const newFirstId = newFirstIdCell.textContent || '';
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
  
  // 4. 确保DOM完全稳定
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
  
  // Step 3: 搜索素材（返回预期总数）
  let expectedTotalCount = null;
  await withRetry(
    async () => {
      expectedTotalCount = await searchMaterial(page, task.materialKeyword);
    },
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
  
  // ⭐ 方案D：验证推送结果
  log(`\n✅ 任务 ${task.taskId} 完成！`, 'OK');
  log(`  总计推送: ${totalPushed} 个素材`);
  log(`  目标账户: ${task.targetAccountIds.length} 个`);
  log(`  总页数: ${pageNum} 页`);
  
  // 返回验证结果（不暂停、不报警）
  if (config.validation?.enabled && expectedTotalCount !== null) {
    const matched = totalPushed === expectedTotalCount;
    
    if (matched) {
      log(`  ✅ 验证：预期${expectedTotalCount}个，实际${totalPushed}个，一致`, 'OK');
    } else {
      log(`  ⚠️ 验证：预期${expectedTotalCount}个，实际${totalPushed}个，不匹配`, 'WARN');
    }
    
    // 返回验证结果供汇总使用
    return {
      taskId: task.taskId,
      expected: expectedTotalCount,
      actual: totalPushed,
      matched: matched,
      pages: pageNum
    };
  }
  
  // 未启用验证时返回null
  return null;
}

module.exports = {
  launchBrowser,
  navigateToMaterialPage,
  executeTask,
};

