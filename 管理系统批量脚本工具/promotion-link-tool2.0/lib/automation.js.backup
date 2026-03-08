/**
 * 核心自动化逻辑 - 推广链接复制工具
 */

// ===== 配置Playwright浏览器路径（必须在require playwright之前）=====
const path = require('path');
process.env.PLAYWRIGHT_BROWSERS_PATH = path.resolve(__dirname, '../pw-browsers');

const { chromium } = require('playwright');
const config = require('../config');
const { log, withRetry, pauseForUser } = require('./utils');

/**
 * 启动浏览器
 */
async function launchBrowser() {
  log('正在启动浏览器...', 'STEP');
  
  const context = await chromium.launchPersistentContext(config.userDataDir, {
    headless: config.browser.headless,
    viewport: config.browser.viewport,
    slowMo: config.browser.slowMo,
  });

  const page = context.pages()[0] || await context.newPage();
  log('✅ 浏览器已启动', 'OK');
  
  return { context, page };
}

/**
 * 导航到推广链接页面
 */
async function navigateToPromotionPage(page) {
  log(`正在加载推广链接页面: ${config.promotionUrl}`, 'STEP');
  
  await page.goto(config.promotionUrl, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout.default,
  });
  
  await page.waitForTimeout(500);  // 优化：从1000ms减少到500ms
  log('✅ 页面加载完成', 'OK');
}

/**
 * 搜索链接（模板链接或新创建的链接）
 */
async function searchLink(page, linkName) {
  log(`搜索链接: ${linkName}`, 'STEP');
  
  // 找到"推广名称"输入框（不是"推广ID"）
  // 策略1: 尝试通过 placeholder 包含"名称"定位
  let nameInput;
  try {
    nameInput = page.locator('input[placeholder*="名称"]').first();
    await nameInput.waitFor({ state: 'visible', timeout: 3000 });
    log(`  找到推广名称输入框（策略1）`);
  } catch (e) {
    // 策略2: 通过多个 input 中的第二个（推广名称通常在推广ID后面）
    const inputs = page.locator('input[placeholder*="推广"]');
    const count = await inputs.count();
    log(`  找到${count}个包含"推广"的输入框`);
    
    // 推广名称通常是第二个（第一个是推广ID）
    if (count >= 2) {
      nameInput = inputs.nth(1);
      log(`  使用第2个输入框（推广名称）`);
    } else {
      nameInput = inputs.first();
      log(`  使用第1个输入框`);
    }
  }
  
  await nameInput.waitFor({ state: 'visible', timeout: config.timeout.element });
  
  // 清空并输入链接名称
  await nameInput.click();
  await nameInput.fill('');
  await page.waitForTimeout(config.delay.inputFill);
  await nameInput.fill(linkName);
  
  // 点击搜索按钮
  const searchBtn = page.locator('button').filter({ hasText: '搜索' }).first();
  await searchBtn.click();
  
  await page.waitForTimeout(config.delay.afterSearch);
  log(`✅ 搜索完成`, 'OK');
}

/**
 * 点击表格操作列中的"+ 复制"按钮（不是"复制链接"）
 */
async function clickCopyButton(page) {
  log(`点击操作列中的"+ 复制"按钮`, 'STEP');
  
  // 表格右侧操作列有：
  // - "复制链接"（蓝色链接，功能不同）❌
  // - "+ 复制"（按钮，我们要点这个）✅
  // - "修改"
  
  // 策略1: 查找包含 "+ 复制" 或 "复制" 的按钮（不是链接）
  let copyBtn;
  try {
    // 查找 <button> 标签，包含"复制"文字，但不是"复制链接"
    const buttons = page.locator('button').filter({ hasText: '复制' });
    const count = await buttons.count();
    log(`  找到 ${count} 个"复制"按钮`);
    
    // 尝试找到文字是 "复制" 或 "+ 复制" 的按钮（排除其他文字）
    let found = false;
    for (let i = 0; i < count; i++) {
      const btn = buttons.nth(i);
      const text = await btn.textContent();
      const trimmedText = text.trim();
      
      log(`  按钮${i + 1}文字: "${trimmedText}"`);
      
      // 匹配 "复制" 或 "+ 复制" 或包含这些的
      if (trimmedText === '复制' || trimmedText === '+ 复制' || trimmedText.includes('+ 复制')) {
        copyBtn = btn;
        log(`  找到"复制"按钮（第 ${i + 1} 个）`);
        found = true;
        break;
      }
    }
    
    if (!found) {
      // 如果没找到精确匹配，使用第一个
      copyBtn = buttons.first();
      log(`  使用第1个"复制"按钮（兜底）`);
    }
  } catch (e) {
    log(`  查找失败: ${e.message}`, 'ERROR');
    // 兜底：使用第一个包含"复制"的按钮
    copyBtn = page.locator('button').filter({ hasText: '复制' }).first();
  }
  
  await copyBtn.click();
  
  await page.waitForTimeout(config.delay.dialogOpen);
  log(`✅ "复制"按钮已点击`, 'OK');
}

/**
 * 在复制弹窗中输入新链接名称并确认
 */
async function inputNewLinkName(page, newLinkName) {
  log(`输入新链接名称: ${newLinkName}`, 'STEP');
  
  // 等待弹窗出现
  await page.waitForTimeout(config.delay.dialogOpen);
  
  try {
    // 关键：弹窗里的是 textarea，不是 input
    // 而且容器是 .el-popover（气泡弹窗），不是 .el-dialog
    const textarea = page.locator('textarea').first();
    
    // 使用 JavaScript 直接设置值（最快）
    await textarea.evaluate((el, value) => {
      el.value = value;
      el.dispatchEvent(new Event('input', { bubbles: true }));
      el.dispatchEvent(new Event('change', { bubbles: true }));
    }, newLinkName);
    
    log(`  已填入新链接名称: ${newLinkName}`);
    
    // 等待一下
    await page.waitForTimeout(300);
    
    // 点击确定按钮 - 在 .el-popover 容器内找按钮
    log(`  查找 popover 内的确定按钮...`);
    
    // 找到 textarea 所在的 popover（可能有多个，取最后一个，即最新出现的）
    const popover = page.locator('.el-popover').filter({ has: page.locator('textarea') }).last();
    const buttons = popover.locator('button');
    const buttonCount = await buttons.count();
    
    log(`  在 popover 内找到 ${buttonCount} 个按钮`);
    
    if (buttonCount === 0) {
      throw new Error('popover 内没有找到按钮！');
    }
    
    // 使用 JavaScript 直接点击第一个可见的按钮
    const clickResult = await page.evaluate(() => {
      // 找到所有可见的 popover（aria-hidden="false"）
      const popovers = Array.from(document.querySelectorAll('.el-popover')).filter(p => {
        return p.getAttribute('aria-hidden') === 'false' || 
               window.getComputedStyle(p).display !== 'none';
      });
      
      // 在最后一个可见的 popover 中找按钮
      if (popovers.length === 0) {
        return { success: false, message: '没有找到可见的 popover' };
      }
      
      const lastPopover = popovers[popovers.length - 1];
      const buttons = Array.from(lastPopover.querySelectorAll('button'));
      
      for (const btn of buttons) {
        const rect = btn.getBoundingClientRect();
        if (rect.width > 0 && rect.height > 0) {
          btn.click();
          return { success: true, text: btn.textContent.trim() };
        }
      }
      
      return { success: false, message: '没有找到可见的按钮' };
    });
    
    if (clickResult.success) {
      log(`  已点击按钮（文字: "${clickResult.text}"）`);
    } else {
      throw new Error(clickResult.message || '点击按钮失败');
    }
    
    await page.waitForTimeout(config.delay.afterCopy);
    log(`✅ 新链接创建完成`, 'OK');
    
  } catch (error) {
    log(`  输入失败: ${error.message}`, 'ERROR');
    throw error;
  }
}

/**
 * 点击修改按钮
 */
async function clickModifyButton(page) {
  log(`点击操作列中的"修改"按钮`, 'STEP');
  
  // 关键：修改按钮在表格行的操作列中，就像复制按钮一样
  // 不是顶部的批量修改按钮（那个需要选中链接）
  
  // 策略：查找 <button> 标签，包含"修改"文字
  let modifyBtn;
  try {
    const buttons = page.locator('button').filter({ hasText: '修改' });
    const count = await buttons.count();
    log(`  找到 ${count} 个"修改"按钮`);
    
    // 遍历找到表格行内的修改按钮（通常是第2个，第1个是顶部的批量修改）
    let found = false;
    for (let i = 0; i < count; i++) {
      const btn = buttons.nth(i);
      const text = await btn.textContent();
      const trimmedText = text.trim();
      const isDisabled = await btn.isDisabled().catch(() => true);
      
      log(`  按钮${i + 1}文字: "${trimmedText}", disabled=${isDisabled}`);
      
      // 跳过禁用的按钮（顶部的批量修改按钮）
      // 找到第一个可用的修改按钮（表格行内的）
      if (!isDisabled && trimmedText === '修改') {
        modifyBtn = btn;
        log(`  找到"修改"按钮（第 ${i + 1} 个）`);
        found = true;
        break;
      }
    }
    
    if (!found) {
      throw new Error('没有找到可用的"修改"按钮');
    }
  } catch (e) {
    log(`  查找失败: ${e.message}`, 'ERROR');
    throw e;
  }
  
  await modifyBtn.click();
  
  await page.waitForTimeout(config.delay.dialogOpen);
  log(`✅ "修改"按钮已点击`, 'OK');
}

/**
 * 修改推广链接参数（剧名、免费集数）
 */
async function modifyLinkParams(page, dramaName, freeEpisodes) {
  log(`修改参数 - 剧名: ${dramaName}, 免费/预览集数: ${freeEpisodes}`, 'STEP');
  
  // 等待弹窗出现（标题是"修改推广信息"）
  const dialog = page.locator('.el-dialog').filter({ hasText: /修改推广信息|推广链接|设置/ }).first();
  await dialog.waitFor({ state: 'visible', timeout: config.timeout.element });
  
  log(`  修改推广信息对话框已出现`);
  
  // 等待一下，确保表单加载完成
  await page.waitForTimeout(500);  // 优化：从800ms减少到500ms
  
  // ========== 修改剧名（下拉选择框）==========
  log(`  开始修改剧名...`);
  
  // 找到剧名输入框：在对话框内，找包含 label "剧" 的表单项
  const dramaFormItem = dialog.locator('.el-form-item').filter({
    has: page.locator('label', { hasText: /^剧$/ })
  });
  const dramaInput = dramaFormItem.locator('input').first();
  
  // 输入剧名
  await dramaInput.click();
  await page.waitForTimeout(200);
  await dramaInput.fill('');
  await page.waitForTimeout(config.delay.inputFill);
  await dramaInput.fill(dramaName);
  log(`  已输入剧名: ${dramaName}`);
  
  // 等待下拉列表出现
  await page.waitForTimeout(500);  // 优化：从800ms减少到500ms
  await page.waitForSelector('.el-select-dropdown:visible, .el-autocomplete-suggestion:visible', { 
    timeout: 3000 
  }).catch(() => {
    log(`  下拉列表未出现，继续尝试`, 'WARN');
  });
  
  // 在下拉列表中点击匹配的选项
  try {
    const dropdown = page.locator('.el-select-dropdown:visible, .el-autocomplete-suggestion:visible').first();
    const option = dropdown.locator('.el-select-dropdown__item, li').filter({ hasText: dramaName }).first();
    
    await option.waitFor({ state: 'visible', timeout: 3000 });
    await option.click();
    log(`  已从下拉列表选择剧名`);
  } catch (e) {
    log(`  选择下拉选项失败: ${e.message}，继续执行`, 'WARN');
  }
  
  // 等待一下，确保选择生效
  await page.waitForTimeout(300);  // 优化：从500ms减少到300ms
  
  // ========== 修改免费集数 ==========
  log(`  开始修改免费集数...`);
  const freeFormItem = dialog.locator('.el-form-item').filter({ hasText: '免费集数' });
  const freeEpisodesInput = freeFormItem.locator('input').first();
  
  await freeEpisodesInput.click();
  await freeEpisodesInput.fill('');
  await page.waitForTimeout(config.delay.inputFill);
  await freeEpisodesInput.fill(String(freeEpisodes));
  log(`  免费集数已填入: ${freeEpisodes}`);
  
  // ========== 修改预览集数 ==========
  log(`  开始修改预览集数...`);
  const previewFormItem = dialog.locator('.el-form-item').filter({ hasText: '预览集数' });
  const previewEpisodesInput = previewFormItem.locator('input').first();
  
  await previewEpisodesInput.click();
  await previewEpisodesInput.fill('');
  await page.waitForTimeout(config.delay.inputFill);
  await previewEpisodesInput.fill(String(freeEpisodes));
  log(`  预览集数已填入: ${freeEpisodes}`);
  
  // 等待一下，确保所有输入完成
  await page.waitForTimeout(300);  // 优化：从500ms减少到300ms
  
  // ========== 点击确定按钮 ==========
  log(`  查找确定按钮...`);
  
  // 在对话框内找确定按钮
  // 策略1: 找包含"确定"的按钮
  // 策略2: 找主按钮（el-button--primary）
  
  let clicked = false;
  
  try {
    // 策略1: 找包含"确定"文字的按钮
    const confirmBtn = dialog.locator('button').filter({ hasText: /确.*定/ }).first();
    await confirmBtn.click({ force: true, timeout: 3000 });
    clicked = true;
    log(`  已点击确定按钮（策略1：文字匹配）`);
  } catch (e1) {
    log(`  策略1失败: ${e1.message}`, 'WARN');
    
    try {
      // 策略2: 找主按钮（蓝色的主按钮通常是确定）
      const primaryBtn = dialog.locator('button.el-button--primary').first();
      await primaryBtn.click({ force: true, timeout: 3000 });
      clicked = true;
      log(`  已点击确定按钮（策略2：主按钮）`);
    } catch (e2) {
      log(`  策略2失败: ${e2.message}`, 'WARN');
      
      // 策略3: 找对话框底部的第一个按钮
      try {
        const footerBtn = dialog.locator('.el-dialog__footer button').first();
        await footerBtn.click({ force: true, timeout: 3000 });
        clicked = true;
        log(`  已点击确定按钮（策略3：底部第一个按钮）`);
      } catch (e3) {
        log(`  策略3失败: ${e3.message}`, 'ERROR');
      }
    }
  }
  
  if (!clicked) {
    throw new Error('所有策略都无法点击确定按钮');
  }
  
  await page.waitForTimeout(config.delay.afterModify);
  log(`✅ 参数修改完成`, 'OK');
}

/**
 * 执行单个任务
 */
async function executeTask(page, task, taskIndex, totalTasks) {
  console.log('\n');
  log('╔═══════════════════════════════════════════════════════════╗', 'STEP');
  log(`║  任务 ${taskIndex + 1}/${totalTasks}：${task.taskId}`, 'STEP');
  log('╚═══════════════════════════════════════════════════════════╝', 'STEP');
  
  const { templateLinkName, dramaName, freeEpisodes, newLinkNames } = task;
  
  // ===== 阶段1：从模板创建第一个新链接 =====
  log('\n【阶段1】从模板创建第一个新链接', 'STEP');
  
  // Step 1: 搜索模板链接
  await withRetry(
    () => searchLink(page, templateLinkName),
    '搜索模板链接'
  );
  
  // Step 2: 点击复制按钮
  await withRetry(
    () => clickCopyButton(page),
    '点击复制按钮'
  );
  
  // Step 3: 输入第一个新链接名称
  const firstLinkName = newLinkNames[0];
  await withRetry(
    () => inputNewLinkName(page, firstLinkName),
    '输入第一个新链接名称'
  );
  
  // ===== 阶段2：修改第一个新链接的参数 =====
  log('\n【阶段2】修改第一个新链接的参数', 'STEP');
  
  // Step 4: 搜索第一个新链接
  await withRetry(
    () => searchLink(page, firstLinkName),
    '搜索第一个新链接'
  );
  
  // Step 5: 点击修改按钮
  await withRetry(
    () => clickModifyButton(page),
    '点击修改按钮'
  );
  
  // Step 6: 修改参数
  await withRetry(
    () => modifyLinkParams(page, dramaName, freeEpisodes),
    '修改链接参数'
  );
  
  // ===== 阶段3：批量复制第一个新链接（一次性复制所有变体）=====
  if (newLinkNames.length > 1) {
    log('\n【阶段3】批量复制第一个新链接（已修改好）', 'STEP');
    
    // 准备所有需要复制的链接名称（除了第一个）
    const remainingLinkNames = newLinkNames.slice(1);
    log(`需要批量复制 ${remainingLinkNames.length} 个变体链接`, 'INFO');
    log(`链接列表:\n  ${remainingLinkNames.join('\n  ')}`, 'INFO');
    
    // 当前已在第一个新链接的搜索结果页，直接复制
    log(`当前已在第一个新链接的搜索结果页，直接复制`, 'INFO');
    
    // 点击复制按钮
    await withRetry(
      () => clickCopyButton(page),
      `点击复制按钮`
    );
    
    // 一次性输入所有链接名称（用换行符分隔）
    const allLinkNames = remainingLinkNames.join('\n');
    await withRetry(
      () => inputNewLinkName(page, allLinkNames),
      `批量输入 ${remainingLinkNames.length} 个链接名称`
    );
    
    log(`✅ 已一次性创建 ${remainingLinkNames.length} 个变体链接`, 'OK');
  }
  
  log('\n✅✅✅ 任务完成！✅✅✅', 'OK');
  log(`共创建 ${newLinkNames.length} 个推广链接`, 'OK');
}

module.exports = {
  launchBrowser,
  navigateToPromotionPage,
  executeTask,
};

