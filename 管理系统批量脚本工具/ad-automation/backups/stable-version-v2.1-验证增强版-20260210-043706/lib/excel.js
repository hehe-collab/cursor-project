/**
 * Excel 读取和解析 - 分组继承模式
 */
const XLSX = require('xlsx');
const { log } = require('./utils');

// Excel 列名映射
const COLUMN_MAP = {
  '任务': 'taskId',
  '主体': 'entity',
  '账户ID': 'accountId',
  'Pixel': 'pixel',
  '项目名称': 'projectName',
  '推广链接关键词': 'linkKeyword',
  '素材关键词': 'materialKeyword',
  '素材ID': 'materialIds',
  '标题': 'titles',
  '优化目标': 'optimizationTarget',
  '出价': 'bid',
  '预算': 'budget',
  '开始日期': 'startDate',
  '开始时间': 'startTime',
  '年龄': 'age',
  '认证身份': 'identity',
  '商品库': 'productStore',
  '商品': 'product',
  '启用': 'enable',
  '提交次数': 'submitCount',
};

/**
 * 读取 Excel 文件并解析为任务组列表
 * @param {string} filePath Excel 文件路径
 * @returns {Array} 任务组列表
 */
function readTasks(filePath) {
  log(`读取 Excel: ${filePath}`);

  const workbook = XLSX.readFile(filePath);
  const sheetName = workbook.SheetNames[0];
  const sheet = workbook.Sheets[sheetName];
  const rawData = XLSX.utils.sheet_to_json(sheet, { defval: '' });

  if (rawData.length === 0) {
    throw new Error('Excel 文件为空，请先填写任务数据');
  }

  // 映射列名
  const data = rawData.map(row => {
    const mapped = {};
    for (const [cnName, enName] of Object.entries(COLUMN_MAP)) {
      if (row[cnName] !== undefined) {
        mapped[enName] = row[cnName];
      }
    }
    return mapped;
  });

  // 按任务编号分组
  const groups = {};
  for (const row of data) {
    const taskId = String(row.taskId).trim();
    if (!taskId) continue;

    if (!groups[taskId]) {
      groups[taskId] = [];
    }
    groups[taskId].push(row);
  }

  // 处理继承逻辑：每组内，后续行从首行继承空值字段
  const taskGroups = [];
  for (const [taskId, rows] of Object.entries(groups)) {
    const firstRow = rows[0];
    const accounts = [];

    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      // 解析"启用"字段：支持 true/false/"是"/"否"/空(默认true)
      const enableRaw = String(row.enable !== '' ? row.enable : (firstRow.enable !== '' ? firstRow.enable : 'true')).trim().toLowerCase();
      const enable = !['false', '否', '0', 'no'].includes(enableRaw);

      // 解析标题：如果当前行有值则用当前行的，否则继承首行
      const titlesStr = String(row.titles || firstRow.titles || '').trim();
      const titles = titlesStr ? titlesStr.split(',').map(t => t.trim()).filter(Boolean) : [];

      const account = {
        accountId: String(row.accountId).trim(),
        // Pixel: 如果当前行有值则用当前行的，否则继承首行
        pixel: String(row.pixel || firstRow.pixel).trim(),
        // 推广链接关键词：每行必填（不继承）
        linkKeyword: String(row.linkKeyword).trim(),
        // 标题：每个账户可以有自己的标题，或继承首行
        titles,
        // 启用：可逐行覆盖，默认 true
        enable,
      };

      if (!account.accountId) {
        log(`任务 ${taskId} 第 ${i + 1} 行缺少账户ID，跳过`, 'WARN');
        continue;
      }
      if (!account.linkKeyword) {
        log(`任务 ${taskId} 账户 ${account.accountId} 缺少推广链接关键词，跳过`, 'WARN');
        continue;
      }
      if (titles.length === 0) {
        log(`任务 ${taskId} 账户 ${account.accountId} 缺少标题，跳过`, 'WARN');
        continue;
      }

      accounts.push(account);
    }

    if (accounts.length === 0) {
      log(`任务 ${taskId} 没有有效账户，跳过`, 'WARN');
      continue;
    }

    // 格式化日期
    let startDate = String(firstRow.startDate || '').trim();
    if (startDate instanceof Date) {
      startDate = startDate.toISOString().split('T')[0];
    }
    // 处理 Excel 日期序列号
    if (/^\d{5}$/.test(startDate)) {
      const date = XLSX.SSF.parse_date_code(Number(startDate));
      startDate = `${date.y}-${String(date.m).padStart(2, '0')}-${String(date.d).padStart(2, '0')}`;
    }

    // 格式化时间（处理 Excel 时间序列号）
    let startTime = String(firstRow.startTime || '').trim();
    
    // Excel 时间值是 0-1 之间的小数（0.5 = 12:00:00）
    if (startTime && !startTime.includes(':')) {
      const timeValue = Number(startTime);
      if (!isNaN(timeValue) && timeValue >= 0 && timeValue < 1) {
        // 转换小数为 HH:MM:SS
        const totalSeconds = Math.round(timeValue * 86400); // 一天86400秒
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        startTime = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
      }
    }
    
    // 如果已经是时间格式（HH:MM:SS 或 HH:MM），保持不变
    // 如果只有 HH:MM，补充 :00
    if (startTime && /^\d{1,2}:\d{2}$/.test(startTime)) {
      startTime = startTime + ':00';
    }

    // 解析提交次数（默认1次）
    let submitCount = parseInt(firstRow.submitCount) || 1;
    if (submitCount < 1) submitCount = 1;
    if (submitCount > 10) submitCount = 10; // 限制最大10次

    // 解析素材ID列表（支持多种分隔符：换行、分号、逗号）
    let materialIds = [];
    const materialIdsStr = String(firstRow.materialIds || '').trim();
    if (materialIdsStr) {
      // 1. 替换 Excel 换行符（\r\n, \n, \r）为统一的换行符
      // 2. 去除首尾的分号和空格
      // 3. 按换行、分号、逗号分隔
      materialIds = materialIdsStr
        .replace(/[\r\n]+/g, '\n')           // 统一换行符
        .replace(/^[;；\s]+|[;；\s]+$/g, '') // 去除首尾的分号和空格
        .split(/[\n;；,，]+/)                 // 按换行、中英文分号、中英文逗号分隔
        .map(id => id.trim())                // 去除每个ID的前后空格
        .filter(id => id.length > 0);        // 去除空字符串
    }

    const taskGroup = {
      taskId,
      entity: String(firstRow.entity).trim(),
      projectName: String(firstRow.projectName).trim(),
      optimizationTarget: String(firstRow.optimizationTarget || '价值').trim(),
      bid: String(firstRow.bid).trim(),
      budget: String(firstRow.budget).trim(),
      startDate,
      startTime,
      materialKeyword: String(firstRow.materialKeyword).trim(),
      materialIds,  // 素材ID数组
      age: String(firstRow.age || '18+').trim(),
      identity: String(firstRow.identity || '').trim(),
      productStore: String(firstRow.productStore || '').trim(),
      product: String(firstRow.product || '').trim(),
      enable: true, // 默认启用
      submitCount, // 提交次数
      accounts,
    };

    // 验证必填字段
    const missing = [];
    if (!taskGroup.entity) missing.push('主体');
    if (!taskGroup.projectName) missing.push('项目名称');
    // 素材关键词和素材ID至少要有一个
    if (!taskGroup.materialKeyword && taskGroup.materialIds.length === 0) {
      missing.push('素材关键词或素材ID（至少填写一项）');
    }
    // 标题已移至账户级别，在上面的循环中已验证
    if (!taskGroup.bid) missing.push('出价');
    if (!taskGroup.budget) missing.push('预算');
    if (!taskGroup.startDate) missing.push('开始日期');
    if (!taskGroup.startTime) missing.push('开始时间');

    if (missing.length > 0) {
      log(`任务 ${taskId} 缺少必填字段: ${missing.join(', ')}`, 'WARN');
    }

    taskGroups.push(taskGroup);
  }

  log(`共解析 ${taskGroups.length} 个任务组，${taskGroups.reduce((s, g) => s + g.accounts.length, 0)} 个账户`);
  return taskGroups;
}

/**
 * 打印任务组摘要（预览用）
 */
function printTaskSummary(taskGroups) {
  console.log('\n' + '='.repeat(70));
  console.log('📊 任务摘要');
  console.log('='.repeat(70));

  for (const group of taskGroups) {
    console.log(`\n🔹 任务 ${group.taskId}: ${group.entity} / ${group.projectName}`);
    console.log(`   优化目标: ${group.optimizationTarget} | 出价: ${group.bid} | 预算: ${group.budget}`);
    console.log(`   开始时间: ${group.startDate} ${group.startTime} | 年龄: ${group.age}`);
    
    // 显示素材检索方式
    if (group.materialKeyword) {
      console.log(`   素材检索: 关键词 "${group.materialKeyword}"`);
    }
    if (group.materialIds.length > 0) {
      console.log(`   素材检索: ID (${group.materialIds.length}个) ${group.materialIds.slice(0, 3).join(', ')}${group.materialIds.length > 3 ? '...' : ''}`);
    }
    
    console.log(`   账户 (${group.accounts.length} 个):`);
    for (const acc of group.accounts) {
      console.log(`     - ${acc.accountId} | Pixel: ${acc.pixel} | 启用: ${acc.enable ? '是' : '否'} | 链接: ${acc.linkKeyword}`);
      console.log(`       标题: ${acc.titles.join(', ')}`);
    }
  }

  console.log('\n' + '='.repeat(70) + '\n');
}

module.exports = { readTasks, printTaskSummary };

