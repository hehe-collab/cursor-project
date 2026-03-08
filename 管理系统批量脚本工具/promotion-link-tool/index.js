/**
 * 主入口 - 推广链接复制工具（支持并行执行）
 */

const { readTasks, displayTasks } = require('./lib/excel');
const { launchBrowser, navigateToPromotionPage, executeTask } = require('./lib/automation');
const { log, pauseForUser } = require('./lib/utils');

async function main() {
  try {
    log('╔═══════════════════════════════════════════════════════════╗', 'STEP');
    log('║                                                           ║', 'STEP');
    log('║       🎉 TikTok推广链接复制工具 v2.0 (并行版)             ║', 'STEP');
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

    // Step 5: 为每个任务创建独立的页面并并行执行
    log(`\n🚀 开始并行执行 ${tasks.length} 个任务...`, 'STEP');
    log('═══════════════════════════════════════════════════════════\n', 'STEP');

    const startTime = Date.now();

    // 为每个任务创建执行函数
    const taskPromises = tasks.map(async (task, index) => {
      let taskPage;
      try {
        // 为每个任务创建独立的页面
        taskPage = await context.newPage();
        
        // 导航到推广链接页面
        await taskPage.goto('https://adminxyz.dramahub8.com/promotion/manage');
        await taskPage.waitForLoadState('networkidle');
        
        // 执行任务
        await executeTask(taskPage, task, index, tasks.length);
        
        return { 
          success: true, 
          taskId: task.taskId,
          message: '✅ 成功'
        };
      } catch (err) {
        // 🔔 任务失败：发出铃声报警
        process.stdout.write('\x07');
        
        return { 
          success: false, 
          taskId: task.taskId,
          message: `❌ 失败: ${err.message}`,
          error: err.message  // 保存详细错误信息
        };
      } finally {
        // 关闭任务专用页面
        if (taskPage) {
          await taskPage.close().catch(() => {});
        }
      }
    });

    // 并行执行所有任务
    const results = await Promise.allSettled(taskPromises);

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
        failedTasks.push({
          taskId: tasks[index].taskId,
          error: result.reason
        });
        log(`任务 ${tasks[index].taskId}: ❌ 失败: ${result.reason}`, 'ERROR');
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
    await context.close();

  } catch (error) {
    log(`❌ 程序执行出错: ${error.message}`, 'ERROR');
    console.error(error);
    process.exit(1);
  }
}

// 运行主函数
main();

