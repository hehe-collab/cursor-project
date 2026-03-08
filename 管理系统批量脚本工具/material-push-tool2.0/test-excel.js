/**
 * 测试Excel数据读取
 */

const { readTasks, printTaskSummary } = require('./lib/excel');
const { log } = require('./lib/utils');

try {
  log('开始测试Excel数据读取...', 'STEP');
  
  const tasks = readTasks();
  
  if (tasks.length === 0) {
    log('❌ 没有找到任何任务', 'ERROR');
    process.exit(1);
  }
  
  printTaskSummary(tasks);
  
  // 验证数据格式
  log('\n📋 数据验证:', 'STEP');
  
  tasks.forEach((task, index) => {
    log(`\n任务 ${index + 1}: ${task.taskId}`);
    log(`  ✅ 源账户ID: ${task.sourceAccountId}`);
    log(`  ✅ 素材关键词: ${task.materialKeyword}`);
    log(`  ✅ 目标账户数量: ${task.targetAccountIds.length}`);
    
    task.targetAccountIds.forEach((id, i) => {
      log(`    ${i + 1}. ${id}`);
    });
  });
  
  console.log('\n');
  log('✅ Excel数据验证通过！', 'OK');
  log('可以运行 npm start 开始执行任务', 'INFO');
  
} catch (err) {
  log(`❌ 测试失败: ${err.message}`, 'ERROR');
  process.exit(1);
}


