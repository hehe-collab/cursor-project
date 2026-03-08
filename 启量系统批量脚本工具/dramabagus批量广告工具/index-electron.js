/**
 * Electron 入口 - 导出 main 供 electron-main 调用
 */
const { readTasks, printTaskSummary } = require('./lib/excel');
const { launchBrowser, preparePage, executeTaskGroup, closeBrowserGracefully } = require('./lib/automation');
const { runParallelMode } = require('./lib/parallel');
const { log, pauseForUser } = require('./lib/utils');
const config = require('./config');

async function run() {
  console.log('');
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║         DramaBagus 批量广告工具                  ║');
  console.log('╚══════════════════════════════════════════════════╝');
  console.log('');

  let taskGroups;
  try {
    taskGroups = readTasks(config.excelPath);
  } catch (err) {
    log(`读取 Excel 失败: ${err.message}`, 'ERROR');
    log('请先填写任务数据文件（点击下方「打开数据目录」）', 'INFO');
    throw err;
  }

  if (taskGroups.length === 0) {
    log('没有找到有效任务，请检查 Excel 数据', 'ERROR');
    throw new Error('没有有效任务');
  }

  printTaskSummary(taskGroups);

  let context, page;
  try {
    const result = await launchBrowser();
    context = result.context;
    page = result.page;
  } catch (err) {
    log(`启动浏览器失败: ${err.message}`, 'ERROR');
    log('首次运行将自动下载 Chromium，请确保网络畅通', 'INFO');
    throw err;
  }

  try {
    await preparePage(page);
  } catch (err) {
    log(`页面准备出错: ${err.message}`, 'ERROR');
    await pauseForUser('请手动打开批量工具页面，准备好后点击「继续」');
  }

  if (config.parallel.enabled) {
    log('✅ 准备页面将保持打开（守护标签页），维持登录状态', 'INFO');
  }

  let successCount = 0;
  let failCount = 0;
  let failedTasks = [];

  if (config.parallel.enabled) {
    console.log('\n');
    log('🚀 并行模式已启用', 'STEP');
    log(`📋 并发数: ${config.parallel.maxConcurrent}`, 'STEP');
    log(`📋 任务总数: ${taskGroups.length}`, 'STEP');
    console.log('');

    try {
      const stats = await runParallelMode(context, taskGroups);
      successCount = stats.success;
      failCount = stats.failed;
      failedTasks = stats.failedTasks || [];
    } catch (err) {
      log(`并行执行失败: ${err.message}`, 'ERROR');
      failCount = taskGroups.length;
    }
  } else {
    console.log('\n');
    log('📋 串行模式（逐个执行）', 'STEP');
    console.log('');

    for (let i = 0; i < taskGroups.length; i++) {
      const taskGroup = taskGroups[i];
      try {
        await executeTaskGroup(page, taskGroup, i, taskGroups.length, i === 0);
        successCount++;
      } catch (err) {
        process.stdout.write('\x07');
        log(`任务 ${taskGroup.taskId} 执行出错: ${err.message}`, 'ERROR');
        failCount++;
        failedTasks.push({
          taskId: taskGroup.taskId,
          error: err.message,
          taskLabel: `任务 ${taskGroup.taskId}`,
        });
        await pauseForUser(`任务 ${taskGroup.taskId} 失败，处理后点击「继续」`);
      }
    }

    console.log('\n');
    console.log('╔══════════════════════════════════════════════════╗');
    console.log('║                  执行完毕                        ║');
    console.log('╚══════════════════════════════════════════════════╝');
    log(`✅  成功: ${successCount} 个`);
    if (failCount > 0) {
      process.stdout.write('\x07');
      log(`❌  失败: ${failCount} 个`, 'ERROR');
      if (failedTasks.length > 0) {
        console.log('');
        failedTasks.forEach((task, idx) => {
          log(`  ${idx + 1}. ${task.taskLabel || task.taskId}`, 'ERROR');
          log(`     失败原因：${task.error}`, 'ERROR');
          console.log('');
        });
      }
    }
    log(`📋  总计: ${taskGroups.length} 个`);
  }

  await pauseForUser('所有任务已完成，点击「继续」关闭浏览器');

  await closeBrowserGracefully(context);
}

module.exports = { run };
