/**
 * Excel数据处理模块
 */

const XLSX = require('xlsx');
const { log } = require('./utils');  // 移除 cleanLinkNames（新格式不需要）
const config = require('../config');

/**
 * 读取Excel任务数据
 * ⭐ 新格式：一行一个新链接名称（按任务ID分组）
 */
function readTasks() {
  try {
    log(`正在读取Excel文件: ${config.excelPath}`);
    
    const workbook = XLSX.readFile(config.excelPath);
    const sheetName = workbook.SheetNames[0];
    const worksheet = workbook.Sheets[sheetName];
    const data = XLSX.utils.sheet_to_json(worksheet);

    log(`读取到 ${data.length} 行数据`);

    // ===== 1. 按任务ID分组 =====
    const groupedByTaskId = {};
    const errors = [];

    for (let i = 0; i < data.length; i++) {
      const row = data[i];
      const rowNum = i + 2; // Excel行号（从第2行开始，第1行是表头）
      
      // 跳过完全空白的行
      if (!row['任务ID'] && !row['新链接名称']) {
        continue;
      }

      // 任务ID必填
      const taskId = row['任务ID'] ? String(row['任务ID']).trim() : '';
      if (!taskId) {
        errors.push(`第${rowNum}行: "任务ID" 为空`);
        continue;
      }

      // 新链接名称必填
      const newLinkName = row['新链接名称'] ? String(row['新链接名称']).trim() : '';
      if (!newLinkName) {
        errors.push(`第${rowNum}行（任务ID: ${taskId}）: "新链接名称" 为空`);
        continue;
      }

      // ⭐ 如果是这个任务ID的第一次出现，创建任务对象
      if (!groupedByTaskId[taskId]) {
        groupedByTaskId[taskId] = {
          taskId: taskId,
          templateLinkName: null,
          dramaName: null,
          freeEpisodes: null,
          newLinkNames: [],
          remark: '',
          firstRow: rowNum  // 记录第一次出现的行号（用于错误提示）
        };
      }

      const task = groupedByTaskId[taskId];

      // ⭐ 收集非空字段（取第一个非空值）
      if (row['模板链接名称'] && !task.templateLinkName) {
        task.templateLinkName = String(row['模板链接名称']).trim();
      }

      if (row['剧名'] && !task.dramaName) {
        task.dramaName = String(row['剧名']).trim();
      }

      if (row['免费/预览集数'] !== undefined && row['免费/预览集数'] !== null && task.freeEpisodes === null) {
        task.freeEpisodes = Number(row['免费/预览集数']);
      }

      if (row['备注'] && !task.remark) {
        task.remark = String(row['备注']).trim();
      }

      // ⭐ 收集新链接名称（每行都要收集）
      task.newLinkNames.push(newLinkName);
    }

    // ===== 2. 验证每个任务的必填字段 =====
    Object.values(groupedByTaskId).forEach(task => {
      const rowNum = task.firstRow;

      if (!task.templateLinkName) {
        errors.push(`任务ID ${task.taskId}（首次出现在第${rowNum}行）: "模板链接名称" 为空`);
      }

      if (!task.dramaName) {
        errors.push(`任务ID ${task.taskId}（首次出现在第${rowNum}行）: "剧名" 为空`);
      }

      if (task.freeEpisodes === null || task.freeEpisodes === undefined) {
        errors.push(`任务ID ${task.taskId}（首次出现在第${rowNum}行）: "免费/预览集数" 为空`);
      } else if (isNaN(task.freeEpisodes) || task.freeEpisodes <= 0) {
        errors.push(`任务ID ${task.taskId}（首次出现在第${rowNum}行）: "免费/预览集数" 必须是大于0的数字（当前值: ${task.freeEpisodes}）`);
      }

      if (task.newLinkNames.length === 0) {
        errors.push(`任务ID ${task.taskId}（首次出现在第${rowNum}行）: 没有"新链接名称"`);
      }

      // 删除临时字段
      delete task.firstRow;
    });

    // ===== 3. 如果有错误，报错并终止 =====
    if (errors.length > 0) {
      console.log('\n');
      log('╔═══════════════════════════════════════════════════════════╗', 'ERROR');
      log('║          ❌ Excel数据验证失败！请修正以下错误：            ║', 'ERROR');
      log('╚═══════════════════════════════════════════════════════════╝', 'ERROR');
      console.log('\n');
      
      errors.forEach(err => {
        log(`  ❌ ${err}`, 'ERROR');
      });
      
      console.log('\n');
      log('提示：', 'ERROR');
      log('  1. 每行必填：任务ID、新链接名称', 'ERROR');
      log('  2. 每个任务的第一行必填：模板链接名称、剧名、免费/预览集数', 'ERROR');
      log('  3. 同一任务的后续行可以只填：任务ID、新链接名称', 'ERROR');
      console.log('\n');
      
      throw new Error(`发现 ${errors.length} 个数据错误，请修正后重新运行`);
    }

    // ===== 4. 转换为数组 =====
    const tasks = Object.values(groupedByTaskId);

    log(`有效任务数: ${tasks.length}`, 'OK');
    return tasks;

  } catch (error) {
    log(`❌ 读取Excel文件失败: ${error.message}`, 'ERROR');
    throw error;
  }
}

/**
 * 显示任务清单
 */
function displayTasks(tasks) {
  console.log('\n');
  log('═══════════════════════════════════════════════════════════', 'STEP');
  log('📋 任务清单', 'STEP');
  log('═══════════════════════════════════════════════════════════', 'STEP');

  tasks.forEach((task, index) => {
    console.log(`\n${index + 1}. 任务ID: ${task.taskId}`);
    console.log(`   模板链接: ${task.templateLinkName}`);
    console.log(`   剧名: ${task.dramaName}`);
    console.log(`   免费/预览集数: ${task.freeEpisodes}`);
    console.log(`   新链接数量: ${task.newLinkNames.length}`);
    console.log(`   新链接列表:`);
    task.newLinkNames.forEach((name, i) => {
      console.log(`     ${i + 1}. ${name}`);
    });
    if (task.remark) {
      console.log(`   备注: ${task.remark}`);
    }
  });

  console.log('\n');
  log(`总计 ${tasks.length} 个任务`, 'OK');
  log('═══════════════════════════════════════════════════════════', 'STEP');
  console.log('\n');
}

module.exports = {
  readTasks,
  displayTasks,
};

