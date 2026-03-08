/**
 * 主入口 - 推广链接复制工具（支持并行/串行执行）
 */

const { readTasks, displayTasks } = require('./lib/excel');
const { launchBrowser, navigateToPromotionPage, executeTask, closeBrowserGracefully } = require('./lib/automation');
const { runTasksInParallel } = require('./lib/parallel');
const { log, pauseForUser } = require('./lib/utils');
const config = require('./config');

async function main() {
  try {
    log('╔═══════════════════════════════════════════════════════════╗', 'STEP');
    log('║                                                           ║', 'STEP');
    log('║       🎉 TikTok推广链接复制工具 2.0 (精确定位版)           ║', 'STEP');
    log('║                                                           ║', 'STEP');
    log('╚═══════════════════════════════════════════════════════════╝', 'STEP');
    console.log('\n');

    // Step 1: 读取Excel任务
    const tasks = readTasks();
    if (tasks.length === 0) {
      log('❌ 没有找到有效任务，请检查Excel文件', 'ERROR');
      process.exit(1);
    }

    // Step 2: 显示任务清单
    displayTasks(tasks);
    await pauseForUser('请确认任务清单，按 Enter 继续');

    // Step 3: 启动浏览器（第一个页面用于登录验证）
    const { context, page } = await launchBrowser();

    // Step 4: 导航到推广链接页面，等待用户准备就绪
    await navigateToPromotionPage(page);
    await pauseForUser('请确认已登录，准备好后按 Enter 开始执行任务');

    const startTime = Date.now();
    let results = [];

    if (config.parallel?.enabled !== false) {
      // ===== 并行模式 =====
      const stats = await runTasksInParallel(context, tasks);
      results = stats.results;
    } else {
      // ===== 串行模式（便于调试和接口不稳时使用）=====
      log(`\n📋 串行模式（逐个执行 ${tasks.length} 个任务）`, 'STEP');
      log('═══════════════════════════════════════════════════════════\n', 'STEP');

      for (let i = 0; i < tasks.length; i++) {
        const task = tasks[i];
        log(`📋 任务 ${i + 1}/${tasks.length}: ${task.taskId}`, 'STEP');
        try {
          await executeTask(page, task, i, tasks.length);
          results.push({
            status: 'fulfilled',
            value: { success: true, taskId: task.taskId, message: '✅ 成功' },
          });
          log(`任务 ${task.taskId}: ✅ 成功`, 'OK');
        } catch (err) {
          process.stdout.write('\x07');
          results.push({
            status: 'fulfilled',
            value: {
              success: false,
              taskId: task.taskId,
              message: `❌ 失败: ${err.message}`,
              error: err.message,
            },
          });
          log(`任务 ${task.taskId}: ❌ 失败: ${err.message}`, 'ERROR');
        }
      }
    }

    const endTime = Date.now();
    const totalTime = ((endTime - startTime) / 1000).toFixed(1);

    // Step 6: 统计结果
    let successCount = 0;
    let failCount = 0;
    const failedTasks = []; // 收集失败的任务信息

    console.log('\n');
    log('╔═══════════════════════════════════════════════════════════╗', 'STEP');
    log('║                   📊 任务执行结果                          ║', 'STEP');
    log('╚═══════════════════════════════════════════════════════════╝', 'STEP');
    console.log('\n');

    results.forEach((result, index) => {
      if (result.status === 'fulfilled') {
        const taskResult = result.value;
        if (taskResult.success) {
          successCount++;
          log(`任务 ${taskResult.taskId}: ${taskResult.message}`, 'OK');
        } else {
          failCount++;
          failedTasks.push({
            taskId: taskResult.taskId,
            error: taskResult.error || taskResult.message
          });
          log(`任务 ${taskResult.taskId}: ${taskResult.message}`, 'ERROR');
        }
      } else {
        failCount++;
        const errMsg = result.reason?.message ?? String(result.reason);
        failedTasks.push({
          taskId: tasks[index].taskId,
          error: errMsg
        });
        log(`任务 ${tasks[index].taskId}: ❌ 失败: ${errMsg}`, 'ERROR');
      }
    });

    // Step 7: 显示完成统计
    console.log('\n');
    log('╔═══════════════════════════════════════════════════════════╗', 'STEP');
    log('║                                                           ║', 'STEP');
    log('║                    🎉 全部任务完成！                       ║', 'STEP');
    log('║                                                           ║', 'STEP');
    log('╚═══════════════════════════════════════════════════════════╝', 'STEP');
    console.log('\n');
    log(`✅ 成功: ${successCount} 个任务`, 'OK');
    if (failCount > 0) {
      // 🔔 有失败任务：发出铃声提醒
      process.stdout.write('\x07');
      
      log(`❌ 失败: ${failCount} 个任务`, 'ERROR');
      
      // 显示失败任务详情
      if (failedTasks.length > 0) {
        console.log('\n');
        log('╔═══════════════════════════════════════════════════════════╗', 'ERROR');
        log('║                  ⚠️  失败任务详情                          ║', 'ERROR');
        log('╚═══════════════════════════════════════════════════════════╝', 'ERROR');
        console.log('');
        failedTasks.forEach((task, idx) => {
          log(`  ${idx + 1}. 任务 ${task.taskId}`, 'ERROR');
          log(`     失败原因：${task.error}`, 'ERROR');
          console.log('');
        });
      }
    }
    log(`⏱️  总耗时: ${totalTime} 秒`, 'INFO');
    log(`⚡️ 平均每个任务: ${(totalTime / tasks.length).toFixed(1)} 秒`, 'INFO');
    console.log('\n');

    await pauseForUser('按 Enter 关闭浏览器');
    await closeBrowserGracefully(context);

  } catch (error) {
    log(`❌ 程序执行出错: ${error.message}`, 'ERROR');
    console.error(error);
    process.exit(1);
  }
}

// 运行主函数
main();

