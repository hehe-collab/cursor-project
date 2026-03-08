/**
 * 测试Excel数据读取 - 推广链接复制工具
 * ⭐ 新格式：一行一个新链接名称
 */

const { readTasks, displayTasks } = require('./lib/excel');
const { log } = require('./lib/utils');

try {
  log('开始测试Excel数据读取...', 'STEP');
  log('⭐ 使用新格式：一行一个新链接名称', 'INFO');
  
  const tasks = readTasks();
  
  if (tasks.length === 0) {
    log('⚠️ 没有找到有效任务', 'WARN');
    process.exit(0);
  }
  
  displayTasks(tasks);
  
  // 验证数据
  log('\n正在验证数据...', 'STEP');
  
  let hasError = false;
  
  tasks.forEach((task, index) => {
    // 验证必填字段
    if (!task.templateLinkName) {
      log(`❌ 任务 ${index + 1}: 模板链接名称不能为空`, 'ERROR');
      hasError = true;
    }
    
    if (!task.dramaName) {
      log(`⚠️ 任务 ${index + 1}: 剧名为空`, 'WARN');
    }
    
    if (task.freeEpisodes === 0) {
      log(`⚠️ 任务 ${index + 1}: 免费/预览集数为0`, 'WARN');
    }
    
    if (task.newLinkNames.length === 0) {
      log(`❌ 任务 ${index + 1}: 新链接名称列表为空`, 'ERROR');
      hasError = true;
    }
  });
  
  if (hasError) {
    log('\n❌ 数据验证失败，请修正后重试', 'ERROR');
    process.exit(1);
  } else {
    log('\n✅ 数据验证通过！', 'OK');
  }
  
} catch (error) {
  log(`❌ 测试失败: ${error.message}`, 'ERROR');
  console.error(error);
  process.exit(1);
}

