/**
 * Excel 读取和解析 - 分组继承模式
 */
const XLSX = require('xlsx');
const { log } = require('./utils');

// Excel 列名映射
/**
 * 与 automation.js 中 selectBiddingStrategy 一致的策略归一化（用于校验与规则）
 */
function normalizeBiddingStrategy(optimizationTarget, biddingStrategy) {
  const target = String(optimizationTarget || '').trim();
  let strategy = String(biddingStrategy || '').trim();
  if (!strategy) {
    strategy = target === '转化' ? '成本上限(CPA)' : '目标ROAS';
  } else {
    const lower = strategy.replace(/\s/g, '').toLowerCase();
    if (lower.includes('目标') && lower.includes('roas')) strategy = '目标ROAS';
    else if (lower.includes('最高价值')) strategy = '最高价值';
    else if (lower.includes('最大化投放')) strategy = '最大化投放';
    else if (lower.includes('成本上限') || lower.includes('cpa')) strategy = '成本上限(CPA)';
  }
  return strategy;
}

/**
 * 允许出价为空白（脚本跳过填出价）的组合：
 * - 转化 + 最大化投放
 * - 价值 + 最高价值
 * 其余情况必须填出价
 */
function allowsEmptyBid(optimizationTarget, biddingStrategy) {
  const opt = String(optimizationTarget || '').trim();
  const strat = normalizeBiddingStrategy(optimizationTarget, biddingStrategy);
  if (opt === '转化' && strat === '最大化投放') return true;
  if (opt === '价值' && strat === '最高价值') return true;
  return false;
}

/** 用于日志：空出价跳过时的组合说明 */
function emptyBidSkipDescription(optimizationTarget, biddingStrategy) {
  if (!allowsEmptyBid(optimizationTarget, biddingStrategy)) return '';
  const opt = String(optimizationTarget || '').trim();
  const strat = normalizeBiddingStrategy(optimizationTarget, biddingStrategy);
  if (opt === '转化' && strat === '最大化投放') return '转化+最大化投放';
  if (opt === '价值' && strat === '最高价值') return '价值+最高价值';
  return '允许空出价';
}

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
  '出价策略': 'biddingStrategy',
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
 * 将多种日期格式统一为 YYYY-MM-DD（供日期选择器使用）
 * 支持：2026/3/5, 2026-02-09, 2026.02.09, 20260209, Excel 序列号(5位数字) 等
 */
function normalizeStartDate(value) {
  if (value == null || value === '') return '';
  if (value instanceof Date) return value.toISOString().split('T')[0];
  const s = String(value).trim();
  if (!s) return '';
  // Excel 日期序列号（5位数字）
  if (/^\d{5}$/.test(s)) {
    try {
      const date = XLSX.SSF.parse_date_code(Number(s));
      return `${date.y}-${String(date.m).padStart(2, '0')}-${String(date.d).padStart(2, '0')}`;
    } catch {
      return s;
    }
  }
  // 2026/3/5 或 2026/03/05
  const slash = s.match(/^(\d{4})[\/\-](\d{1,2})[\/\-](\d{1,2})$/);
  if (slash) {
    return `${slash[1]}-${String(parseInt(slash[2], 10)).padStart(2, '0')}-${String(parseInt(slash[3], 10)).padStart(2, '0')}`;
  }
  // 20260209 或 2026-02-09（已是目标格式）
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;
  if (/^\d{8}$/.test(s)) {
    return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
  }
  // 2026.02.09
  const dot = s.match(/^(\d{4})\.(\d{1,2})\.(\d{1,2})$/);
  if (dot) {
    return `${dot[1]}-${String(parseInt(dot[2], 10)).padStart(2, '0')}-${String(parseInt(dot[3], 10)).padStart(2, '0')}`;
  }
  // 带时间的合并格式：2026-02-09 03:00 或 2026/3/5 0:00 → 只取日期部分
  const withTime = s.match(/^(\d{4})[\/\-]?(\d{1,2})[\/\-]?(\d{1,2})/);
  if (withTime) {
    return `${withTime[1]}-${String(parseInt(withTime[2], 10)).padStart(2, '0')}-${String(parseInt(withTime[3], 10)).padStart(2, '0')}`;
  }
  return s;
}

/**
 * 将多种时间格式统一为 HH:mm:ss（供时间选择器使用）
 * 支持：03:00, 3:00, 03:00:00, Excel 小数(0-1), 02:00 等
 */
function normalizeStartTime(value) {
  if (value == null || value === '') return '';
  if (value instanceof Date) {
    const h = value.getHours(), m = value.getMinutes(), s = value.getSeconds();
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  const s = String(value).trim();
  if (!s) return '';
  // Excel 时间：纯时间小数 0-1，或日期+时间序列号（小数部分为时间）
  if (!s.includes(':')) {
    const timeValue = Number(s);
    if (!isNaN(timeValue) && timeValue >= 0) {
      const fraction = timeValue < 1 ? timeValue : timeValue - Math.floor(timeValue);
      const totalSeconds = Math.round(fraction * 86400);
      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;
      return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }
  }
  // HH:mm 或 H:mm → 补 :00
  if (/^\d{1,2}:\d{2}$/.test(s)) return s + ':00';
  // HH:mm:ss 或 H:mm:ss 已满足
  const parts = s.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?/);
  if (parts) {
    const h = String(parseInt(parts[1], 10)).padStart(2, '0');
    const m = String(parseInt(parts[2], 10)).padStart(2, '0');
    const sec = parts[3] != null ? String(parseInt(parts[3], 10)).padStart(2, '0') : '00';
    return `${h}:${m}:${sec}`;
  }
  return s;
}

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
  const rawData = XLSX.utils.sheet_to_json(sheet, { defval: '', cellDates: true });

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

  // 按任务编号分组（支持多账户：首行填完整信息，后续行仅填账户ID即归属同一任务）
  const groups = {};
  let currentTaskId = null;
  for (const row of data) {
    const taskId = String(row.taskId || '').trim();
    const accountId = String(row.accountId || '').trim();

    if (taskId) {
      currentTaskId = taskId;
    }
    if (!currentTaskId) continue; // 尚未遇到任务行，跳过
    if (!accountId) continue;     // 无账户ID，跳过

    if (!groups[currentTaskId]) {
      groups[currentTaskId] = [];
    }
    groups[currentTaskId].push(row);
  }

  // 处理继承逻辑：每组内，后续行从首行继承空值字段
  const taskGroups = [];
  for (const [taskId, rows] of Object.entries(groups)) {
    const firstRow = rows[0];
    const accounts = [];

    // --- 在任务组层面预处理一些只有首行有意义的基础配置，但核心配置全部下放到 account 层 ---
    // (主体、taskId作为基本元数据保留在 group)
    const entity = String(firstRow.entity).trim();

    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      
      // 【核心思想】：逐行独立设置，如果当前行某列为空，则继承首行对应的列

      const accountId = String(row.accountId).trim();
      if (!accountId) {
        log(`任务 ${taskId} 第 ${i + 1} 行缺少账户ID，跳过`, 'WARN');
        continue;
      }

      // --- 文本型字段解析（当前行优先，否则首行） ---
      const pixel = String(row.pixel || firstRow.pixel).trim();
      const projectName = String(row.projectName || firstRow.projectName).trim();
      const linkKeyword = String(row.linkKeyword || firstRow.linkKeyword || '').trim();
      const optimizationTarget = String(row.optimizationTarget || firstRow.optimizationTarget || '价值').trim();
      const biddingStrategy = String(row.biddingStrategy || firstRow.biddingStrategy || '').trim();
      const bid = String(row.bid || firstRow.bid).trim();
      const budget = String(row.budget || firstRow.budget).trim();
      const age = String(row.age || firstRow.age || '18+').trim();
      const identity = String(row.identity || firstRow.identity || '').trim();
      const productStore = String(row.productStore || firstRow.productStore || '').trim();
      const product = String(row.product || firstRow.product || '').trim();

      // --- 特殊字段解析（启用状态） ---
      const enableRaw = String(row.enable !== '' ? row.enable : (firstRow.enable !== '' ? firstRow.enable : 'true')).trim().toLowerCase();
      const enable = !['false', '否', '0', 'no'].includes(enableRaw);

      // --- 数组/列表型字段解析（标题、素材） ---
      const titlesStr = String(row.titles || firstRow.titles || '').trim();
      const titles = titlesStr ? titlesStr.split(',').map(t => t.trim()).filter(Boolean) : [];

      const materialKeyword = String(row.materialKeyword || firstRow.materialKeyword || '').trim();
      
      let materialIds = [];
      const materialIdsStr = String(row.materialIds || firstRow.materialIds || '').trim();
      if (materialIdsStr) {
        materialIds = materialIdsStr
          .replace(/[\r\n]+/g, '\n')
          .replace(/^[;；\s]+|[;；\s]+$/g, '')
          .split(/[\n;；,，]+/)
          .map(id => id.trim())
          .filter(id => id.length > 0);
      }

      // --- 日期和时间解析 ---
      let startDateRaw = row.startDate || firstRow.startDate;
      if (startDateRaw instanceof Date) {
        startDateRaw = startDateRaw.toISOString().split('T')[0];
      }
      const startDate = normalizeStartDate(startDateRaw);
      const startTime = normalizeStartTime(row.startTime !== undefined && row.startTime !== '' ? row.startTime : firstRow.startTime);

      // --- 验证该行的必填字段 ---
      if (!linkKeyword) log(`任务 ${taskId} 账户 ${accountId} 缺少推广链接关键词`, 'WARN');
      if (titles.length === 0) log(`任务 ${taskId} 账户 ${accountId} 缺少标题`, 'WARN');
      if (!projectName) log(`任务 ${taskId} 账户 ${accountId} 缺少项目名称`, 'WARN');
      const bidEmpty = !String(bid || '').trim();
      const budgetEmpty = !String(budget || '').trim();
      if (budgetEmpty) log(`任务 ${taskId} 账户 ${accountId} 缺少预算`, 'WARN');
      if (bidEmpty && !allowsEmptyBid(optimizationTarget, biddingStrategy)) {
        log(`任务 ${taskId} 账户 ${accountId} 缺少出价（仅「转化+最大化投放」或「价值+最高价值」可不填）`, 'WARN');
      }
      if (!materialKeyword && materialIds.length === 0) log(`任务 ${taskId} 账户 ${accountId} 缺少素材检索信息`, 'WARN');

      accounts.push({
        accountId,
        pixel,
        projectName,
        linkKeyword,
        titles,
        optimizationTarget,
        biddingStrategy,
        bid,
        budget,
        startDate,
        startTime,
        materialKeyword,
        materialIds,
        age,
        identity,
        productStore,
        product,
        enable
      });
    }

    if (accounts.length === 0) {
      log(`任务 ${taskId} 没有有效账户，跳过`, 'WARN');
      continue;
    }

    // 提交次数一般是针对整个任务控制的，读取首行即可
    let submitCount = parseInt(firstRow.submitCount) || 1;
    if (submitCount < 1) submitCount = 1;
    if (submitCount > 10) submitCount = 10;

    // 构建组对象 (公共数据 + 各自独立的账户配置数据)
    taskGroups.push({
      taskId,
      entity,
      submitCount,
      accounts
    });
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
    const first = group.accounts && group.accounts[0];
    const projectName = first ? first.projectName : '';
    const optimizationTarget = first ? first.optimizationTarget : '';
    const bid = first ? first.bid : '';
    const budget = first ? first.budget : '';
    const startDate = first ? first.startDate : '';
    const startTime = first ? first.startTime : '';
    const age = first ? first.age : '';
    const materialKeyword = first ? first.materialKeyword : '';
    const materialIds = first && Array.isArray(first.materialIds) ? first.materialIds : [];

    console.log(`\n🔹 任务 ${group.taskId}: ${group.entity} / ${projectName}`);
    console.log(`   优化目标: ${optimizationTarget} | 出价: ${bid} | 预算: ${budget}`);
    console.log(`   开始时间: ${startDate} ${startTime} | 年龄: ${age}`);

    if (materialKeyword) {
      console.log(`   素材检索: 关键词 "${materialKeyword}"`);
    }
    if (materialIds.length > 0) {
      console.log(`   素材检索: ID (${materialIds.length}个) ${materialIds.slice(0, 3).join(', ')}${materialIds.length > 3 ? '...' : ''}`);
    }

    console.log(`   账户 (${group.accounts.length} 个):`);
    for (const acc of group.accounts) {
      const titles = Array.isArray(acc.titles) ? acc.titles : [];
      console.log(`     - ${acc.accountId} | Pixel: ${acc.pixel} | 启用: ${acc.enable ? '是' : '否'} | 链接: ${acc.linkKeyword}`);
      console.log(`       标题: ${titles.join(', ')}`);
    }
  }

  console.log('\n' + '='.repeat(70) + '\n');
}

module.exports = {
  readTasks,
  printTaskSummary,
  normalizeStartDate,
  normalizeStartTime,
  normalizeBiddingStrategy,
  allowsEmptyBid,
  emptyBidSkipDescription,
};

