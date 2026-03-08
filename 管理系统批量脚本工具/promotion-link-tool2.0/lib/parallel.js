/**
 * 并行执行模块 - 推广链接复制工具 2.0
 * 支持：分批并行、批间延迟、每批完成即时日志
 */

const config = require('../config');
const { log } = require('./utils');
const { executeTask } = require('./automation');

const delay = (ms) => new Promise((r) => setTimeout(r, ms));

/**
 * 并行执行多个任务（分批，每批最多 maxConcurrent 个）
 * @param {import('playwright').BrowserContext} context
 * @param {Array} tasks
 * @returns {Promise<{ results: Array }>}
 */
async function runTasksInParallel(context, tasks) {
  const maxConcurrent = config.parallel?.maxConcurrent ?? 10;
  const batchDelay = config.parallel?.batchDelay ?? 2000;
  const promotionUrl = config.promotionUrl || 'https://adminxyz.dramahub8.com/promotion/manage';
  const totalBatches = Math.ceil(tasks.length / maxConcurrent);

  log(`\n🚀 并行模式已启用`, 'STEP');
  log(`  最大并发数: ${maxConcurrent}`, 'INFO');
  log(`  批间延迟: ${batchDelay / 1000} 秒`, 'INFO');
  log(`  共 ${totalBatches} 批任务\n`, 'INFO');
  log('═══════════════════════════════════════════════════════════\n', 'STEP');

  const allResults = [];

  for (let i = 0; i < tasks.length; i += maxConcurrent) {
    const batch = tasks.slice(i, i + maxConcurrent);
    const batchIndex = Math.floor(i / maxConcurrent) + 1;

    log(`📋 第 ${batchIndex}/${totalBatches} 批（${batch.length} 个任务）`, 'STEP');

    // 先批量创建所有标签页并加载（domcontentloaded 比 networkidle 更快，减轻并发压力）
    const pageInfos = await Promise.all(
      batch.map(async (task, index) => {
        const taskPage = await context.newPage();
        await taskPage.goto(promotionUrl, { waitUntil: 'domcontentloaded' });
        await taskPage.waitForTimeout(1500); // 等待页面稳定
        return { page: taskPage, task, globalIndex: i + index };
      })
    );
    log(`  ✅ ${batch.length} 个标签页已创建并加载完成`, 'INFO');

    // 批内错峰：每个任务错开启动，减轻瞬时请求压力
    const staggerInterval = config.parallel?.staggerInterval ?? 300;

    // 然后并行执行任务
    const taskPromises = pageInfos.map(async ({ page, task, globalIndex }, indexInBatch) => {
      await delay(indexInBatch * staggerInterval);
      try {
        await executeTask(page, task, globalIndex, tasks.length);
        return { success: true, taskId: task.taskId, message: '✅ 成功' };
      } catch (err) {
        process.stdout.write('\x07');
        return {
          success: false,
          taskId: task.taskId,
          message: `❌ 失败: ${err.message}`,
          error: err.message,
        };
      } finally {
        await page.close().catch(() => {});
      }
    });

    const batchResults = await Promise.allSettled(taskPromises);
    allResults.push(...batchResults);

    // 每批完成即时日志
    const batchSuccess = batchResults.filter(
      (r) => r.status === 'fulfilled' && r.value?.success
    ).length;
    const batchFail = batchResults.length - batchSuccess;

    log(`\n────────────────────────────────────────────────────────────`, 'INFO');
    log(
      `👉 第 ${batchIndex} 批完成：✅ ${batchSuccess} 成功, ❌ ${batchFail} 失败`,
      batchFail > 0 ? 'WARN' : 'OK'
    );

    if (batchFail > 0) {
      const batchFailedItems = [];
      batchResults.forEach((r, idx) => {
        if (r.status === 'rejected') {
          batchFailedItems.push({
            taskId: batch[idx].taskId,
            error: r.reason?.message || String(r.reason),
          });
        } else if (r.status === 'fulfilled' && !r.value?.success) {
          batchFailedItems.push({
            taskId: r.value.taskId,
            error: r.value.error || r.value.message,
          });
        }
      });
      batchFailedItems.forEach((item, idx) => {
        log(`   ${idx + 1}. 任务 ${item.taskId}: ${item.error}`, 'ERROR');
      });
    }
    log(`────────────────────────────────────────────────────────────\n`, 'INFO');

    // 批间延迟（最后一批不等待）
    if (i + maxConcurrent < tasks.length && batchDelay > 0) {
      log(`⏳ 等待 ${batchDelay / 1000} 秒后开始下一批...\n`, 'INFO');
      await delay(batchDelay);
    }
  }

  return { results: allResults };
}

module.exports = {
  runTasksInParallel,
};
