/**
 * Excel数据处理
 */

const XLSX = require('xlsx');
const path = require('path');
const config = require('../config');
const { log, cleanAccountIds } = require('./utils');

/**
 * 读取推送任务
 */
function readTasks() {
  const excelPath = path.resolve(__dirname, '..', config.excelPath);
  
  log(`读取Excel文件: ${excelPath}`);
  
  try {
    const workbook = XLSX.readFile(excelPath);
    const sheetName = workbook.SheetNames[0];
    const sheet = workbook.Sheets[sheetName];
    const rawData = XLSX.utils.sheet_to_json(sheet);
    
    log(`✅ 读取到 ${rawData.length} 行数据`, 'OK');
    
    // 数据转换和验证
    const tasks = rawData
      .filter((row, index) => {
        // 跳过空行
        if (!row['源账户ID'] && !row['素材关键词']) {
          log(`  跳过第 ${index + 2} 行（空行）`, 'WARN');
          return false;
        }
        return true;
      })
      .map((row, index) => {
        const rowNum = index + 2; // Excel行号（从2开始，因为第1行是表头）
        
        // 必填字段验证
        if (!row['源账户ID']) {
          throw new Error(`第 ${rowNum} 行：缺少"源账户ID"`);
        }
        if (!row['素材关键词']) {
          throw new Error(`第 ${rowNum} 行：缺少"素材关键词"`);
        }
        if (!row['目标账户ID列表']) {
          throw new Error(`第 ${rowNum} 行：缺少"目标账户ID列表"`);
        }
        
        // 清理目标账户ID列表
        const targetAccountIds = cleanAccountIds(String(row['目标账户ID列表']));
        
        if (targetAccountIds.length === 0) {
          throw new Error(`第 ${rowNum} 行："目标账户ID列表"为空或格式错误`);
        }
        
            return {
              taskId: row['任务ID'] ? String(row['任务ID']).trim() : `任务${index + 1}`,
              sourceAccountId: String(row['源账户ID']).trim(),
              materialKeyword: String(row['素材关键词']).trim(),
              targetAccountIds: targetAccountIds,
              remark: row['备注'] || '',
              _rowNum: rowNum,
            };
      });
    
    log(`✅ 解析到 ${tasks.length} 个有效任务`, 'OK');
    
    return tasks;
  } catch (err) {
    log(`❌ 读取Excel失败: ${err.message}`, 'ERROR');
    throw err;
  }
}

/**
 * 打印任务摘要
 */
function printTaskSummary(tasks) {
  console.log('\n');
  log('╔═══════════════════════════════════════════════════════════╗', 'STEP');
  log('║                    📋 任务清单                              ║', 'STEP');
  log('╚═══════════════════════════════════════════════════════════╝', 'STEP');
  
  tasks.forEach((task, index) => {
    console.log('\n');
    log(`任务 ${index + 1}/${tasks.length}：${task.taskId}`);
    log(`  源账户：${task.sourceAccountId}`);
    log(`  素材关键词：${task.materialKeyword}`);
    log(`  目标账户数量：${task.targetAccountIds.length} 个`);
    log(`  目标账户：${task.targetAccountIds.join(', ')}`);
    if (task.remark) {
      log(`  备注：${task.remark}`);
    }
  });
  
  console.log('\n');
  log('─────────────────────────────────────────────────────────', 'STEP');
  log(`📊 总计 ${tasks.length} 个推送任务`, 'STEP');
  log('─────────────────────────────────────────────────────────', 'STEP');
  console.log('\n');
}

module.exports = {
  readTasks,
  printTaskSummary,
};

