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
  /** 3.0：项目弹窗内「商品库」开关；开=true 则广告组/广告侧不选商品库，关=false 则必选 */
  '项目商品库': 'projectProductLibrary',
  '广告名称': 'adName',
  '推广链接关键词': 'linkKeyword',
  '素材关键词': 'materialKeyword',
  '素材ID': 'materialIds',
  '标题': 'titles',
  '优化目标': 'optimizationTarget',
  '出价': 'bid',
  /** 3.0：项目「每日预算」；2.0 曾用于广告组与出价并列，3.0 仅项目层 */
  '预算': 'budget',
  '开始日期': 'startDate',
  '开始时间': 'startTime',
  '年龄': 'age',
  '认证身份': 'identity',
  '商品库': 'productStore',
  '商品': 'product',
  '启用': 'enable',
  /** 3.0：项目弹窗「基于目标增加预算」开关；开/关/true/false，空则继承首行，再空默认关 */
  '基于目标增加预算': 'increaseBudgetByGoal',
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

  // 辅助：格式化日期（支持 Excel 序列号）
  function formatStartDate(val) {
    let s = String(val || '').trim();
    if (!s) return '';
    if (val instanceof Date) return val.toISOString().split('T')[0];
    if (/^\d{5}$/.test(s)) {
      const date = XLSX.SSF.parse_date_code(Number(s));
      return `${date.y}-${String(date.m).padStart(2, '0')}-${String(date.d).padStart(2, '0')}`;
    }
    return s;
  }

  // 辅助：格式化时间（支持 Excel 小数）
  // 注意：Excel 将「0:00:00」存为数字 0；若写 val||'' 会把 0 当成空（falsy），必须用 ?? 或显式判断
  function formatStartTime(val) {
    let s = String(val ?? '').trim();
    if (!s) return '';
    if (s.includes(':')) {
      const parts = s.split(':').map((x) => x.trim()).filter(Boolean);
      if (parts.length === 2) {
        const h = parseInt(parts[0], 10);
        const m = parseInt(parts[1], 10);
        if (!Number.isNaN(h) && !Number.isNaN(m)) {
          return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`;
        }
      }
      if (parts.length >= 3) {
        const h = parseInt(parts[0], 10);
        const m = parseInt(parts[1], 10);
        const sec = parseInt(parts[2], 10);
        if (!Number.isNaN(h) && !Number.isNaN(m) && !Number.isNaN(sec)) {
          return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
        }
      }
      if (/^\d{1,2}:\d{2}$/.test(s)) return `${s}:00`;
      return s;
    }
    const timeValue = Number(s);
    if (!isNaN(timeValue) && timeValue >= 0 && timeValue < 1) {
      const totalSeconds = Math.round(timeValue * 86400);
      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;
      return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }
    return s;
  }

  // 辅助：解析素材ID列表
  function parseMaterialIds(val) {
    const str = String(val || '').trim();
    if (!str) return [];
    return str
      .replace(/[\r\n]+/g, '\n')
      .replace(/^[;；\s]+|[;；\s]+$/g, '')
      .split(/[\n;；,，]+/)
      .map(id => id.trim())
      .filter(id => id.length > 0);
  }

  /** 解析项目商品库开关：开/关/true/false，空则关 */
  function parseProjectProductLibrary(val) {
    if (val === true || val === 1) return true;
    const raw = String(val ?? '').trim().toLowerCase();
    if (!raw) return false;
    return ['true', '1', '开', '是', 'yes', 'on'].includes(raw);
  }

  /** 解析「基于目标增加预算」：与项目商品库相同语义，空则关 */
  function parseIncreaseBudgetByGoal(val) {
    return parseProjectProductLibrary(val);
  }

  // 处理继承逻辑：每组内，后续行从首行继承空值字段；所有列支持逐行独立配置
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

      let pplVal = row.projectProductLibrary;
      if (pplVal === undefined || pplVal === '') pplVal = firstRow.projectProductLibrary;
      const projectProductLibraryEnabled = parseProjectProductLibrary(pplVal);

      let ibgVal = row.increaseBudgetByGoal;
      if (ibgVal === undefined || ibgVal === '') ibgVal = firstRow.increaseBudgetByGoal;
      const increaseBudgetByGoalEnabled = parseIncreaseBudgetByGoal(ibgVal);

      const account = {
        accountId: String(row.accountId).trim(),
        pixel: String(row.pixel || firstRow.pixel || '').trim(),
        linkKeyword: String(row.linkKeyword || '').trim(),
        titles,
        enable,
        // 以下字段全部支持逐行独立配置，空则继承首行
        projectName: String(row.projectName || firstRow.projectName || '').trim(),
        /** 3.0：项目弹窗是否开启商品库（也可按行覆盖） */
        projectProductLibraryEnabled,
        /** 3.0：项目弹窗「基于目标增加预算」开关（按行覆盖，空继承首行） */
        increaseBudgetByGoalEnabled,
        /** 3.0：广告设置里「名称」，空则用项目名称 */
        adName: String(row.adName || firstRow.adName || '').trim(),
        materialKeyword: String(row.materialKeyword || firstRow.materialKeyword || '').trim(),
        materialIds: parseMaterialIds(row.materialIds || firstRow.materialIds),
        optimizationTarget: String(row.optimizationTarget || firstRow.optimizationTarget || '价值').trim(),
        bid: String(row.bid || firstRow.bid || '').trim(),
        budget: String(row.budget || firstRow.budget || '').trim(),
        startDate: formatStartDate(row.startDate || firstRow.startDate),
        startTime: formatStartTime(row.startTime || firstRow.startTime),
        age: String(row.age || firstRow.age || '18+').trim(),
        identity: String(row.identity || firstRow.identity || '').trim(),
        productStore: String(row.productStore || firstRow.productStore || '').trim(),
        product: String(row.product || firstRow.product || '').trim(),
      };

      if (!account.adName) {
        account.adName = account.projectName;
      }

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

    // 解析提交次数（任务级，默认1次）
    let submitCount = parseInt(firstRow.submitCount) || 1;
    if (submitCount < 1) submitCount = 1;
    if (submitCount > 10) submitCount = 10;

    const taskGroup = {
      taskId,
      entity: String(firstRow.entity).trim(),
      submitCount,
      accounts,
    };

    // 验证必填字段（以首行/首账户为准）
    const firstAcc = accounts[0];
    const missing = [];
    if (!taskGroup.entity) missing.push('主体');
    if (!firstAcc.projectName) missing.push('项目名称');
    if (!firstAcc.materialKeyword && firstAcc.materialIds.length === 0) missing.push('素材关键词或素材ID（至少填写一项）');
    if (!firstAcc.bid) missing.push('出价');
    if (!firstAcc.budget) missing.push('预算');
    if (!firstAcc.startDate) missing.push('开始日期');
    if (!firstAcc.startTime) missing.push('开始时间');

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
    const first = group.accounts[0];
    console.log(`\n🔹 任务 ${group.taskId}: ${group.entity} / ${first?.projectName || '-'}`);
    console.log(
      `   优化目标: ${first?.optimizationTarget || '-'} | 出价: ${first?.bid || '-'} | 预算: ${first?.budget || '-'} | 基于目标增加预算: ${first?.increaseBudgetByGoalEnabled ? '开' : '关'}`
    );
    console.log(`   开始时间: ${first?.startDate || '-'} ${first?.startTime || '-'} | 年龄: ${first?.age || '18+'}`);
    
    if (first?.materialKeyword) console.log(`   素材检索: 关键词 "${first.materialKeyword}"`);
    if (first?.materialIds?.length > 0) console.log(`   素材检索: ID (${first.materialIds.length}个) ${first.materialIds.slice(0, 3).join(', ')}${first.materialIds.length > 3 ? '...' : ''}`);
    
    console.log(`   账户 (${group.accounts.length} 个):`);
    for (const acc of group.accounts) {
      console.log(`     - ${acc.accountId} | Pixel: ${acc.pixel} | 启用: ${acc.enable ? '是' : '否'} | 链接: ${acc.linkKeyword}`);
      console.log(`       标题: ${acc.titles.join(', ')}`);
    }
  }

  console.log('\n' + '='.repeat(70) + '\n');
}

module.exports = { readTasks, printTaskSummary };

