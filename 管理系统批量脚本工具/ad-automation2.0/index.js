/**
 * TikTok 广告批量建设自动化脚本 - 主入口
 *
 * 使用方式：
 *   1. 先生成 Excel 模板：  npm run template
 *   2. 填写 data/tasks-v2.0.xlsx
 *   3. 运行脚本：           npm start
 *   4. 浏览器会自动打开，请手动登录并进入批量工具页面
 *   5. 处理完所有弹窗后，回到终端按 Enter 开始执行
 */
const { readTasks, printTaskSummary } = require('./lib/excel');
const { launchBrowser, preparePage, executeTaskGroup, closeBrowserGracefully } = require('./lib/automation');
const { runParallelMode } = require('./lib/parallel');
const { log, pauseForUser } = require('./lib/utils');
const config = require('./config');

async function main() {
  console.log('');
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║    TikTok 广告批量建设自动化脚本  2.0（批间轮询）║');
  console.log('╚══════════════════════════════════════════════════╝');
  console.log('');

  // Step 1: 读取 Excel 数据
  let taskGroups;
  try {
    taskGroups = readTasks(config.excelPath);
  } catch (err) {
    log(`读取 Excel 失败: ${err.message}`, 'ERROR');
    log('请先运行 npm run template 生成模板，然后填写 data/tasks-v2.0.xlsx', 'INFO');
    process.exit(1);
  }

  if (taskGroups.length === 0) {
    log('没有找到有效任务，请检查 Excel 数据', 'ERROR');
    process.exit(1);
  }

  // 打印任务摘要
  printTaskSummary(taskGroups);

  // Step 2: 启动浏览器
  let context, page;
  try {
    const result = await launchBrowser();
    context = result.context;
    page = result.page;
  } catch (err) {
    log(`启动浏览器失败: ${err.message}`, 'ERROR');
    log('请确保已运行 npm run setup 安装浏览器', 'INFO');
    process.exit(1);
  }

  // Step 3: 打开批量工具页面，等待用户准备就绪
  try {
    await preparePage(page);
  } catch (err) {
    log(`页面准备出错: ${err.message}`, 'ERROR');
    await pauseForUser('请手动打开批量工具页面，准备好后按 Enter');
  }
  
  // 🛡️ 保留准备页面作为"守护标签页"（维持 Session，避免批次2需要重新登录）
  if (config.parallel.enabled) {
    log('✅ 准备页面将保持打开（守护标签页），维持登录状态', 'INFO');
  }

  // Step 4: 执行任务（并行或串行）
  let successCount = 0;
  let failCount = 0;
  let failedTasks = []; // 收集失败的任务信息

  if (config.parallel.enabled) {
    // ===== 并行模式 =====
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
    // ===== 串行模式（原有逻辑）=====
    console.log('\n');
    log('📋 串行模式（逐个执行）', 'STEP');
    console.log('');

    for (let i = 0; i < taskGroups.length; i++) {
      const taskGroup = taskGroups[i];

      try {
        // 第一个任务不刷新页面（用户已经准备好了）
        await executeTaskGroup(page, taskGroup, i, taskGroups.length, i === 0);
        successCount++;
      } catch (err) {
        // 🔔 任务失败：发出铃声报警
        process.stdout.write('\x07');
        
        log(`任务 ${taskGroup.taskId} 执行出错: ${err.message}`, 'ERROR');
        failCount++;
        failedTasks.push({
          taskId: taskGroup.taskId,
          error: err.message,
          taskLabel: `任务 ${taskGroup.taskId}`
        });
        await pauseForUser(`任务 ${taskGroup.taskId} 失败，处理后按 Enter 继续下一个任务`);
      }
    }

    // 串行模式完成汇总
    console.log('\n');
    console.log('╔══════════════════════════════════════════════════╗');
    console.log('║                  执行完毕                        ║');
    console.log('╚══════════════════════════════════════════════════╝');
    log(`✅  成功: ${successCount} 个`);
    if (failCount > 0) {
      // 🔔 有失败任务：发出铃声提醒
      process.stdout.write('\x07');
      
      log(`❌  失败: ${failCount} 个`, 'ERROR');
      
      // 显示失败任务详情
      if (failedTasks.length > 0) {
        console.log('');
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
    log(`📋  总计: ${taskGroups.length} 个`);
  }

  // 保持浏览器打开，用户可以手动检查
  await pauseForUser('所有任务已完成，按 Enter 关闭浏览器');
  
  // 优雅关闭浏览器：避免"意外退出"提示
  await closeBrowserGracefully(context);
}

// 运行
main().catch(err => {
  console.error('致命错误:', err);
  process.exit(1);
});
