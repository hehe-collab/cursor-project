/**
 * 并行执行模块
 */

const config = require('../config');
const { log, delay } = require('./utils');
const { executeTask } = require('./automation');

/**
 * 并行执行多个推送任务
 */
async function runTasksInParallel(context, tasks) {
  const maxConcurrent = config.parallel.maxConcurrent;
  const batchDelay = config.parallel.batchDelay;
  const totalTasks = tasks.length;
  let successCount = 0;
  let failCount = 0;
  const failedTasks = []; // 收集失败的任务信息

  log(`🚀 并行模式已启用`, 'STEP');
  log(`  最大并发数: ${maxConcurrent}`);
  log(`  每批任务延迟: ${batchDelay / 1000} 秒`);

  // 分批执行任务
  for (let i = 0; i < totalTasks; i += maxConcurrent) {
    const batch = tasks.slice(i, i + maxConcurrent);
    const batchNumber = Math.floor(i / maxConcurrent) + 1;
    const totalBatches = Math.ceil(totalTasks / maxConcurrent);

    log(`\n--- 开始执行第 ${batchNumber}/${totalBatches} 批任务 (${batch.length} 个) ---`, 'STEP');

    // 🔧 优化：先批量创建所有标签页，让用户能看到同时打开
    const pageCreationPromises = batch.map(async (task, indexInBatch) => {
      const taskNumber = i + indexInBatch + 1;
      const taskLabel = `[批次${batchNumber}-任务${taskNumber}] ${task.taskId}`;
      
      log(`  🔷 ${taskLabel} - 正在创建新标签页...`);
      const page = await context.newPage();
      
      log(`  ✅ ${taskLabel} - 标签页已创建，正在加载...`);
      await page.goto(config.materialUrl, {
        waitUntil: 'domcontentloaded',
        timeout: 30000
      });
      
      log(`  ✅ ${taskLabel} - 页面加载完成`);
      await page.waitForTimeout(1500);
      
      return { page, task, taskNumber, taskLabel };
    });

    log(`  ⏳ 等待所有标签页创建完成...`);
    const pageInfos = await Promise.all(pageCreationPromises);
    log(`  ✅ ${batch.length} 个标签页已全部创建并加载完成！\n`);

    // 🔧 优化：然后并行执行所有任务
    const taskPromises = pageInfos.map(async ({ page, task, taskNumber, taskLabel }) => {
      try {
        log(`  🚀 ${taskLabel} - 开始执行推送任务...`);
        // 传递 isParallel=true，避免重复刷新页面
        await executeTask(page, task, taskNumber - 1, totalTasks, true);

        log(`✅ ${taskLabel} - 完成`, 'OK');
        return { success: true, taskId: task.taskId };
      } catch (err) {
        // 🔔 任务失败：发出铃声报警
        process.stdout.write('\x07'); // 终端铃声
        
        log(`❌ ${taskLabel} - 失败: ${err.message}`, 'ERROR');
        return { 
          success: false, 
          taskId: task.taskId, 
          error: err.message,
          taskLabel: taskLabel // 保存任务标签便于显示
        };
      } finally {
        // 确保关闭标签页（无论成功失败）
        if (page) {
          try {
            await page.close();
            log(`  📋 ${taskLabel} - 标签页已关闭`);
          } catch (closeErr) {
            log(`  ⚠️ ${taskLabel} - 关闭标签页失败: ${closeErr.message}`, 'WARN');
          }
        }
      }
    });

    const results = await Promise.all(taskPromises);

    // 统计并收集失败任务
    const batchFailedTasks = [];
    results.forEach(result => {
      if (result.success) {
        successCount++;
      } else {
        failCount++;
        failedTasks.push(result); // 收集失败任务
        batchFailedTasks.push(result); // 当前批次的失败任务
      }
    });

    log(`\n────────────────────────────────────────────────────────────`);
    log(`👉 第 ${batchNumber} 批完成：✅ ${results.filter(r => r.success).length} 成功, ❌ ${results.filter(r => !r.success).length} 失败`);
    
    // 如果有失败任务，显示详细信息
    if (batchFailedTasks.length > 0) {
      log(`\n   ⚠️  本批失败任务详情：`, 'ERROR');
      batchFailedTasks.forEach((task, idx) => {
        log(`      ${idx + 1}. ${task.taskLabel}`, 'ERROR');
        log(`         原因：${task.error}`, 'ERROR');
      });
    }
    
    log(`────────────────────────────────────────────────────────────`);

    if (i + maxConcurrent < totalTasks) {
      log(`📋 ⏱️  等待 ${batchDelay / 1000} 秒后开始下一批任务...`);
      await delay(batchDelay);
    }
  }

  return { successCount, failCount, failedTasks };
}

module.exports = {
  runTasksInParallel,
};

