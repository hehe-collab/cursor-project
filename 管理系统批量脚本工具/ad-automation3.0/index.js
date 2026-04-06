/**
 * TikTok 广告批量建设自动化脚本 - 主入口
 *
 * 使用方式：
 *   1. 先生成 Excel 模板：  npm run template
 *   2. 填写 data/tasks-v3.0.xlsx
 *   3. 运行脚本：           npm start
 *   4. 浏览器会自动打开，请手动登录并进入批量工具页面
 *   5. 处理完所有弹窗后，回到终端按 Enter 开始执行
 *   6. 一轮结束后：须输入 q 关闭浏览器；输入 r 继续下一轮（重新读取 Excel）
 */
const { readTasks, printTaskSummary } = require('./lib/excel');
const { launchBrowser, preparePage, executeTaskGroup, closeBrowserGracefully } = require('./lib/automation');
const { runParallelMode } = require('./lib/parallel');
const { log, pauseForUser, promptNextRoundOrClose } = require('./lib/utils');
const config = require('./config');

function printRoundSummary(successCount, failCount, total, failedTasks) {
  console.log('\n');
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║                  执行完毕                        ║');
  console.log('╚══════════════════════════════════════════════════╝');
  log(`✅  成功: ${successCount} 个`);
  if (failCount > 0) {
    process.stdout.write('\x07');
    log(`❌  失败: ${failCount} 个`, 'ERROR');
    if (failedTasks && failedTasks.length > 0) {
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
  log(`📋  总计: ${total} 个`);
}

async function main() {
  console.log('');
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║    TikTok 广告批量建设自动化脚本  3.0（批间轮询）║');
  console.log('╚══════════════════════════════════════════════════╝');
  console.log('');

  let context = null;
  let page = null;
  let roundIndex = 0;

  for (;;) {
    roundIndex += 1;
    let taskGroups;

    for (;;) {
      try {
        taskGroups = readTasks(config.excelPath);
        break;
      } catch (err) {
        log(`读取 Excel 失败: ${err.message}`, 'ERROR');
        if (!context) {
          log('请先运行 npm run template 生成模板，然后填写 data/tasks-v3.0.xlsx', 'INFO');
          process.exit(1);
        }
        await pauseForUser('请修复 Excel 后按 Enter 重新读取');
      }
    }

    if (taskGroups.length === 0) {
      log('没有找到有效任务，请检查 Excel 数据', 'ERROR');
      if (!context) {
        process.exit(1);
      }
      await pauseForUser('请修改 Excel 后按 Enter 重试读取');
      continue;
    }

    printTaskSummary(taskGroups);

    if (!context) {
      try {
        const result = await launchBrowser();
        context = result.context;
        page = result.page;
      } catch (err) {
        log(`启动浏览器失败: ${err.message}`, 'ERROR');
        log('请确保已运行 npm run setup 安装浏览器', 'INFO');
        process.exit(1);
      }

      try {
        await preparePage(page);
      } catch (err) {
        log(`页面准备出错: ${err.message}`, 'ERROR');
        await pauseForUser('请手动打开批量工具页面，准备好后按 Enter');
      }

      if (config.parallel.enabled) {
        log('✅ 准备页面将保持打开（守护标签页），维持登录状态', 'INFO');
      }
    } else {
      log(`第 ${roundIndex} 轮：将使用重新读取的 Excel，在当前浏览器中执行`, 'STEP');
      await pauseForUser('请确认仍在批量工具页面、已保存 Excel，准备好后按 Enter 开始本轮');
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
          // 仅「整个进程的第一组任务」跳过刷新；新一轮首组也刷新回到批量工具干净状态
          const skipRefresh = roundIndex === 1 && i === 0;
          await executeTaskGroup(page, taskGroup, i, taskGroups.length, skipRefresh);
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
          await pauseForUser(`任务 ${taskGroup.taskId} 失败，处理后按 Enter 继续下一个任务`);
        }
      }
    }

    printRoundSummary(successCount, failCount, taskGroups.length, failedTasks);

    const next = await promptNextRoundOrClose('本轮任务已结束');
    if (next === 'close') {
      await closeBrowserGracefully(context);
      break;
    }
  }
}

main().catch((err) => {
  console.error('致命错误:', err);
  process.exit(1);
});
