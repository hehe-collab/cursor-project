/**
 * lib/parallel.js
 * 并行处理模块 - 在多个浏览器标签页中同时执行不同任务
 * 2.0：批间轮询广告任务页，第一页 0 个「进行中」再开下一批
 */

const { log } = require('./utils');
const { executeTaskGroup } = require('./automation');
const config = require('../config');

const delay = (ms) => new Promise((r) => setTimeout(r, ms));

/**
 * 打开广告任务页，统计第一页表格中「进行中」的行数
 * @param {BrowserContext} browser
 * @returns {Promise<number>}
 */
async function getInProgressCountOnAdTaskPage(browser) {
  const page = await browser.newPage();
  try {
    await page.goto(config.adTaskUrl || 'https://adminxyz.dramahub8.com/advertiseTools/adTask', {
      waitUntil: 'domcontentloaded',
      timeout: 20000,
    });
    // 等待表格出现（或「正在加载系统资源」消失后的表格）
    await page.waitForSelector('table tbody tr, .el-table__body-wrapper tbody tr', { timeout: 15000 }).catch(() => null);
    await delay(1500);
    const count = await page.locator('tbody tr').filter({ hasText: '进行中' }).count();
    return count;
  } finally {
    await page.close().catch(() => {});
  }
}

/**
 * 轮询广告任务页，直到第一页 0 个「进行中」（无最大等待时间）
 * @param {BrowserContext} browser
 */
async function waitUntilAdTaskPageClear(browser) {
  const initialDelay = config.parallel.pollInitialDelay ?? 5000;
  const interval = config.parallel.pollInterval ?? 10000;
  log(`⏳ 本批已完成，${initialDelay / 1000} 秒后开始轮询广告任务页（第一页 0 个「进行中」再开下一批）...`, 'STEP');
  await delay(initialDelay);
  for (;;) {
    const count = await getInProgressCountOnAdTaskPage(browser);
    if (count === 0) {
      log(`✅ 广告任务页第一页已无「进行中」，开始下一批`, 'OK');
      return;
    }
    log(`📋 广告任务页第一页仍有 ${count} 个「进行中」，${interval / 1000} 秒后重试...`, 'INFO');
    await delay(interval);
  }
}

/**
 * 将任务数组分成批次
 * @param {Array} tasks - 所有任务
 * @param {number} batchSize - 每批任务数量
 * @returns {Array<Array>} - 分批后的任务数组
 */
function splitIntoBatches(tasks, batchSize) {
  const batches = [];
  for (let i = 0; i < tasks.length; i += batchSize) {
    batches.push(tasks.slice(i, i + batchSize));
  }
  return batches;
}

/**
 * 并行执行一批任务
 * @param {BrowserContext} browser - 浏览器上下文
 * @param {Array} batch - 一批任务
 * @param {number} batchIndex - 批次序号（从1开始）
 * @param {number} totalBatches - 总批次数
 * @returns {Promise<Array>} - 执行结果数组
 */
async function executeTaskBatch(browser, batch, batchIndex, totalBatches) {
  console.log('\n' + '═'.repeat(60));
  log(`执行第 ${batchIndex}/${totalBatches} 批任务（${batch.length} 个并发）`, 'STEP');
  console.log('═'.repeat(60) + '\n');

  // 创建Promise数组，每个Promise代表一个任务
  const promises = batch.map(async (taskGroup, index) => {
    const taskNumber = (batchIndex - 1) * batch.length + index + 1;
    const taskLabel = `[批次${batchIndex}-任务${index + 1}] ${taskGroup.taskId}`;
    
    try {
      log(`\n🚀 ${taskLabel} - 开始（标签页 ${index + 1}）`);
      
      // 为每个任务创建新标签页
      const page = await browser.newPage();
      
      try {
        // 导航到任务页面
        await page.goto(config.taskUrl, { 
          waitUntil: config.advanced.skipNetworkIdle ? 'domcontentloaded' : 'networkidle',
          timeout: 30000 
        });
        
        // 执行任务
        // 注意：并行模式下，每个标签页都是新建的，都相当于"第一个"任务
        // 所以都传isFirst=true，避免重复刷新页面
        await executeTaskGroup(page, taskGroup, taskNumber - 1, batch.length * totalBatches, true);
        
        log(`✅ ${taskLabel} - 完成`, 'OK');
        
        return { 
          success: true, 
          taskId: taskGroup.taskId,
          taskNumber: taskNumber,
          batchIndex: batchIndex,
          indexInBatch: index + 1
        };
      } finally {
        // 确保关闭标签页（无论成功失败）
        try {
          await page.close();
          log(`📋 ${taskLabel} - 标签页已关闭`);
        } catch (closeErr) {
          log(`⚠️  ${taskLabel} - 标签页关闭失败: ${closeErr.message}`, 'WARN');
        }
      }
    } catch (error) {
      // 🔔 任务失败：发出铃声报警
      process.stdout.write('\x07');
      
      log(`❌ ${taskLabel} - 失败: ${error.message}`, 'ERROR');
      return { 
        success: false, 
        taskId: taskGroup.taskId,
        taskNumber: taskNumber,
        batchIndex: batchIndex,
        indexInBatch: index + 1,
        error: error.message,
        taskLabel: taskLabel  // 保存任务标签便于显示
      };
    }
  });

  // 等待所有任务完成
  log(`\n⏳ 等待第 ${batchIndex} 批的 ${batch.length} 个任务全部完成...\n`);
  const results = await Promise.all(promises);
  
  // 统计本批次结果
  const successCount = results.filter(r => r.success).length;
  const failedCount = results.filter(r => !r.success).length;
  const batchFailedTasks = results.filter(r => !r.success);
  
  console.log('\n' + '─'.repeat(60));
  log(`第 ${batchIndex} 批完成：✅ ${successCount} 成功, ${failedCount > 0 ? `❌ ${failedCount} 失败` : ''}`, 'STEP');
  
  // 如果有失败任务，显示详细信息
  if (batchFailedTasks.length > 0) {
    log(`\n   ⚠️  本批失败任务详情：`, 'ERROR');
    batchFailedTasks.forEach((task, idx) => {
      log(`      ${idx + 1}. ${task.taskLabel || task.taskId}`, 'ERROR');
      log(`         原因：${task.error}`, 'ERROR');
    });
  }
  
  console.log('─'.repeat(60) + '\n');
  
  return results;
}

/**
 * 并行模式主函数
 * @param {BrowserContext} browser - 浏览器上下文
 * @param {Array} taskGroups - 所有任务组
 * @returns {Promise<Object>} - 执行统计结果
 */
async function runParallelMode(browser, taskGroups) {
  console.log('\n' + '╔'.repeat(60));
  log('🚀 并行模式已启动', 'STEP');
  log(`📋 任务总数: ${taskGroups.length}`, 'STEP');
  log(`📋 最大并发: ${config.parallel.maxConcurrent}`, 'STEP');
  console.log('╚'.repeat(60) + '\n');
  
  // 分批
  const batches = splitIntoBatches(taskGroups, config.parallel.maxConcurrent);
  log(`📋 分为 ${batches.length} 批执行\n`);
  
  let allResults = [];
  const startTime = Date.now();
  
  // 执行每一批
  for (let i = 0; i < batches.length; i++) {
    const batchStartTime = Date.now();
    
    const batchResults = await executeTaskBatch(browser, batches[i], i + 1, batches.length);
    allResults = allResults.concat(batchResults);
    
    const batchDuration = ((Date.now() - batchStartTime) / 1000).toFixed(1);
    log(`⏱️  第 ${i + 1} 批耗时: ${batchDuration} 秒\n`);
    
    // 2.0：若有下一批，先轮询广告任务页直到第一页 0 个「进行中」
    if (i < batches.length - 1) {
      if (config.parallel.pollAdTaskAfterBatch && config.adTaskUrl) {
        await waitUntilAdTaskPageClear(browser);
      } else if (config.parallel.batchDelay > 0) {
        log(`⏳ 等待 ${config.parallel.batchDelay}ms 后执行下一批...\n`);
        await delay(config.parallel.batchDelay);
      }
    }
  }
  
  const totalDuration = ((Date.now() - startTime) / 1000 / 60).toFixed(1);

  const successCount = allResults.filter(r => r.success).length;
  const failedCount = allResults.filter(r => !r.success).length;
  const failedTasks = allResults.filter(r => !r.success);

  // 汇总由 index.js 的 printRoundSummary 统一打印，避免与主流程重复

  return {
    total: taskGroups.length,
    success: successCount,
    failed: failedCount,
    duration: totalDuration,
    results: allResults,
    failedTasks: failedTasks  // 返回失败任务列表
  };
}

module.exports = {
  splitIntoBatches,
  executeTaskBatch,
  runParallelMode
};

