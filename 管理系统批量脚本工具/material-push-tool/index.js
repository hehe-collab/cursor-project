/**
 * 素材推送工具 - 主入口
 */

const { readTasks, printTaskSummary } = require('./lib/excel');
const { launchBrowser, navigateToMaterialPage, executeTask } = require('./lib/automation');
const { runTasksInParallel } = require('./lib/parallel');
const { log, pauseForUser } = require('./lib/utils');
const config = require('./config');

async function main() {
  console.log('\n');
  log('╔═══════════════════════════════════════════════════════════╗', 'STEP');
  log('║           🚀 素材推送自动化工具 v1.0                         ║', 'STEP');
  log('╚═══════════════════════════════════════════════════════════╝', 'STEP');
  console.log('\n');
  
  try {
    // Step 1: 读取Excel任务
    const tasks = readTasks();
    
    if (tasks.length === 0) {
      log('❌ 没有找到任何任务，请检查Excel文件', 'ERROR');
      return;
    }
    
    // Step 2: 打印任务摘要
    printTaskSummary(tasks);
    
    // Step 3: 启动浏览器
    const { context, page } = await launchBrowser();
    
    // Step 4: 导航到素材页面，等待用户准备就绪
    try {
      await navigateToMaterialPage(page);
      await pauseForUser('请确认已登录，准备好后按 Enter 开始执行任务');
    } catch (err) {
      log(`页面准备出错: ${err.message}`, 'ERROR');
      await pauseForUser('请手动打开素材页面，准备好后按 Enter');
    }
    
    // Step 5: 执行任务（并行或串行）
    let successCount = 0;
    let failCount = 0;
    let failedTasks = []; // 收集失败的任务

    if (config.parallel.enabled) {
      // ===== 并行模式 =====
      const results = await runTasksInParallel(context, tasks);
      successCount = results.successCount;
      failCount = results.failCount;
      failedTasks = results.failedTasks || [];
    } else {
      // ===== 串行模式（原有逻辑） =====
      for (let i = 0; i < tasks.length; i++) {
        const task = tasks[i];
        try {
          await executeTask(page, task, i, tasks.length);
          successCount++;
        } catch (err) {
          // 🔔 任务失败：发出铃声报警
          process.stdout.write('\x07');
          
          log(`❌ 任务 ${task.taskId} 执行出错: ${err.message}`, 'ERROR');
          failCount++;
          failedTasks.push({
            taskId: task.taskId,
            error: err.message,
            taskLabel: `任务 ${task.taskId}`
          });
          await pauseForUser(`任务 ${task.taskId} 失败，处理后按 Enter 继续下一个任务`);
        }
      }
    }
    
    // Step 6: 完成汇总
    console.log('\n');
    log('╔═══════════════════════════════════════════════════════════╗', 'STEP');
    log('║                    👉 执行完毕' + (config.parallel.enabled ? '（并行模式）' : '（串行模式）') + '                              ║', 'STEP');
    log('╚═══════════════════════════════════════════════════════════╝', 'STEP');
    log(`✅ 成功: ${successCount} 个`, 'OK');
    if (failCount > 0) {
      // 🔔 有失败任务：发出铃声提醒
      process.stdout.write('\x07');
      
      log(`❌ 失败: ${failCount} 个`, 'ERROR');
      
      // 显示失败任务详情
      if (failedTasks.length > 0) {
        console.log('\n');
        log('╔═══════════════════════════════════════════════════════════╗', 'ERROR');
        log('║                  ⚠️  失败任务详情                          ║', 'ERROR');
        log('╚═══════════════════════════════════════════════════════════╝', 'ERROR');
        console.log('');
        failedTasks.forEach((task, idx) => {
          log(`  ${idx + 1}. ${task.taskLabel || task.taskId}`, 'ERROR');
          log(`     失败原因：${task.error}`, 'ERROR');
          console.log('');
        });
      }
    }
    log(`📋 总计: ${tasks.length} 个`);
    console.log('\n');
    
    // 保持浏览器打开，用户可以手动检查
    await pauseForUser('所有任务已完成，按 Enter 关闭浏览器');
    await context.close();
    
  } catch (err) {
    log(`❌ 致命错误: ${err.message}`, 'ERROR');
    console.error(err);
    process.exit(1);
  }
}

// 运行
main().catch(err => {
  console.error('致命错误:', err);
  process.exit(1);
});

