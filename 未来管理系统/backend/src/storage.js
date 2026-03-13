/**
 * JSON 文件存储，用于充值记录、投放链接、充值方案、账户管理等
 * 项目默认使用 JSON 模式，无需 MySQL
 */
const path = require('path');
const fs = require('fs');

const dataDir = path.join(__dirname, '../data');
const storageFile = path.join(dataDir, 'storage.json');

const defaultData = {
  recharge_records: [],
  promotion_links: [],
  recharge_plans: [],
  recharge_plan_groups: [],
  ad_accounts: [],
  callback_config: {},
  callback_logs: [],
  ad_materials: [],
  title_packs: [],
  ad_tasks: [],
};

function load() {
  fs.mkdirSync(dataDir, { recursive: true });
  if (fs.existsSync(storageFile)) {
    try {
      return JSON.parse(fs.readFileSync(storageFile, 'utf8'));
    } catch {}
  }
  return { ...defaultData };
}

function save(data) {
  fs.writeFileSync(storageFile, JSON.stringify(data, null, 0));
}

function getStorage() {
  const d = load();
  if (!d.recharge_records) d.recharge_records = [];
  if (!d.promotion_links) d.promotion_links = [];
  if (!d.recharge_plans) d.recharge_plans = [];
  if (!d.recharge_plan_groups) d.recharge_plan_groups = [];
  if (!d.ad_accounts) d.ad_accounts = [];
  if (!d.callback_config) d.callback_config = {};
  if (!d.callback_logs) d.callback_logs = [];
  if (!d.ad_materials) d.ad_materials = [];
  if (!d.title_packs) d.title_packs = [];
  if (!d.ad_tasks) d.ad_tasks = [];
  return d;
}

function getCallbackConfig() {
  return getStorage().callback_config || {};
}
function saveCallbackConfig(obj) {
  const d = getStorage();
  d.callback_config = { ...d.callback_config, ...obj };
  save(d);
}

function getCallbackLogs(filter = {}) {
  let list = [...(getStorage().callback_logs || [])].sort((a, b) => (b.id || 0) - (a.id || 0));
  if (filter.dateStart) list = list.filter(x => (x.created_at || '').slice(0, 10) >= filter.dateStart);
  if (filter.dateEnd) list = list.filter(x => (x.created_at || '').slice(0, 10) <= filter.dateEnd);
  if (filter.status && filter.status !== '全部') list = list.filter(x => (x.status || '') === filter.status);
  if (filter.eventType && filter.eventType !== '全部') list = list.filter(x => (x.event_type || x.event || '').includes(filter.eventType));
  if (filter.orderNo) list = list.filter(x => (x.order_no || x.orderNo || '').includes(filter.orderNo));
  const total = list.length;
  const stats = {
    total,
    success: list.filter(x => (x.status || '') === '成功').length,
    failure: list.filter(x => (x.status || '') === '失败').length,
    pending: list.filter(x => (x.status || '') === '待处理').length,
  };
  const page = parseInt(filter.page, 10) || 1;
  const pageSize = parseInt(filter.pageSize, 10) || 10;
  const offset = (page - 1) * pageSize;
  list = list.slice(offset, offset + pageSize);
  return { list, total, stats };
}
function addCallbackLog(item) {
  const d = getStorage();
  const id = Math.max(0, ...(d.callback_logs || []).map(x => x.id || 0)) + 1;
  (d.callback_logs = d.callback_logs || []).unshift({ id, ...item, created_at: new Date().toISOString() });
  save(d);
}

function getAdMaterials(filter = {}) {
  let list = [...(getStorage().ad_materials || [])].sort((a, b) => (b.id || 0) - (a.id || 0));
  if (filter.name) list = list.filter(r => (r.name || '').includes(filter.name));
  return list;
}
function saveAdMaterial(item) {
  const d = getStorage();
  const id = item.id || Math.max(0, ...(d.ad_materials || []).map(x => x.id || 0)) + 1;
  const idx = (d.ad_materials = d.ad_materials || []).findIndex(x => x.id === id);
  const row = { ...item, id };
  if (idx >= 0) d.ad_materials[idx] = row;
  else d.ad_materials.push({ ...row, created_at: new Date().toISOString() });
  save(d);
  return id;
}
function deleteAdMaterial(id) {
  const d = getStorage();
  d.ad_materials = (d.ad_materials || []).filter(x => x.id !== parseInt(id));
  save(d);
}

function getTitlePacks(filter = {}) {
  let list = [...(getStorage().title_packs || [])].sort((a, b) => (b.id || 0) - (a.id || 0));
  if (filter.title) list = list.filter(r => (r.title || '').includes(filter.title));
  return list;
}
function saveTitlePack(item) {
  const d = getStorage();
  const id = item.id || Math.max(0, ...(d.title_packs || []).map(x => x.id || 0)) + 1;
  const idx = (d.title_packs = d.title_packs || []).findIndex(x => x.id === id);
  const row = { ...item, id };
  if (idx >= 0) d.title_packs[idx] = row;
  else d.title_packs.push({ ...row, created_at: new Date().toISOString() });
  save(d);
  return id;
}
function deleteTitlePack(id) {
  const d = getStorage();
  d.title_packs = (d.title_packs || []).filter(x => x.id !== parseInt(id));
  save(d);
}

function getAdTasks(filter = {}) {
  let list = [...(getStorage().ad_tasks || [])].sort((a, b) => (b.id || 0) - (a.id || 0));
  return list;
}
function saveAdTask(item) {
  const d = getStorage();
  const id = item.id || Math.max(0, ...(d.ad_tasks || []).map(x => x.id || 0)) + 1;
  const idx = (d.ad_tasks = d.ad_tasks || []).findIndex(x => x.id === id);
  const row = { ...item, id };
  if (idx >= 0) d.ad_tasks[idx] = row;
  else d.ad_tasks.push({ ...row, status: row.status || 'stopped', create_time: new Date().toISOString() });
  save(d);
  return id;
}
function deleteAdTask(id) {
  const d = getStorage();
  d.ad_tasks = (d.ad_tasks || []).filter(x => x.id !== parseInt(id));
  save(d);
}

function getRechargePlanGroups(filter = {}) {
  let list = [...getStorage().recharge_plan_groups].sort((a, b) => (b.id || 0) - (a.id || 0));
  if (filter.name) list = list.filter(r => (r.name || '').includes(filter.name));
  return list;
}

function saveRechargePlanGroup(item) {
  const d = getStorage();
  const id = item.id || Math.max(0, ...d.recharge_plan_groups.map(x => x.id || 0)) + 1;
  const idx = d.recharge_plan_groups.findIndex(x => x.id === id);
  const row = { ...item, id };
  if (idx >= 0) d.recharge_plan_groups[idx] = row;
  else d.recharge_plan_groups.push({ ...row, created_at: new Date().toISOString() });
  save(d);
  return id;
}

function deleteRechargePlanGroup(id) {
  const d = getStorage();
  d.recharge_plan_groups = d.recharge_plan_groups.filter(x => x.id !== parseInt(id));
  save(d);
}

// 充值记录
function getRechargeRecords(filter = {}) {
  let list = [...getStorage().recharge_records].sort((a, b) => (b.id || 0) - (a.id || 0));
  if (filter.userId) list = list.filter(r => String(r.user_id) === String(filter.userId));
  if (filter.currency) list = list.filter(r => r.currency === filter.currency);
  if (filter.merchantId) list = list.filter(r => (r.merchant_id || '').includes(filter.merchantId));
  if (filter.orderNo) list = list.filter(r => (r.order_no || '').includes(filter.orderNo));
  if (filter.externalOrderNo) list = list.filter(r => (r.external_order_no || '').includes(filter.externalOrderNo));
  if (filter.promoteId) list = list.filter(r => (r.promote_id || '').includes(filter.promoteId));
  if (filter.payResult) list = list.filter(r => r.pay_status === filter.payResult);
  if (filter.country) list = list.filter(r => r.country === filter.country);
  if (filter.registerStart) list = list.filter(r => (r.created_at || '').slice(0, 10) >= filter.registerStart);
  if (filter.registerEnd) list = list.filter(r => (r.created_at || '').slice(0, 10) <= filter.registerEnd);
  return list;
}

// 投放链接
function getPromotionLinks(filter = {}) {
  let list = [...getStorage().promotion_links].sort((a, b) => (b.id || 0) - (a.id || 0));
  if (filter.promoteId) list = list.filter(r => (r.promote_id || '').includes(filter.promoteId));
  if (filter.media) list = list.filter(r => r.platform === filter.media);
  if (filter.promoteName) list = list.filter(r => (r.promote_name || '').includes(filter.promoteName));
  if (filter.dramaId) list = list.filter(r => r.drama_id == filter.dramaId);
  if (filter.country) list = list.filter(r => r.country === filter.country);
  if (filter.domain) list = list.filter(r => (r.domain || '').includes(filter.domain));
  if (filter.linkKeyword) list = list.filter(r => (r.link_keyword || r.promote_id || r.promote_name || '').toLowerCase().includes((filter.linkKeyword || '').toLowerCase()));
  return list;
}

function savePromotionLink(item) {
  const d = getStorage();
  const id = item.id || Math.max(0, ...d.promotion_links.map(x => x.id || 0)) + 1;
  const idx = d.promotion_links.findIndex(x => x.id === id);
  const existing = idx >= 0 ? d.promotion_links[idx] : {};
  const row = { ...existing, ...item, id, updated_at: new Date().toISOString() };
  if (idx >= 0) d.promotion_links[idx] = row;
  else d.promotion_links.push({ ...row, created_at: row.updated_at, created_by: item.created_by || 0 });
  save(d);
  return id;
}

function deletePromotionLink(id) {
  const d = getStorage();
  d.promotion_links = d.promotion_links.filter(x => x.id !== parseInt(id));
  save(d);
}

// 充值方案
function getRechargePlans(filter = {}) {
  let list = [...getStorage().recharge_plans].sort((a, b) => (b.id || 0) - (a.id || 0));
  if (filter.name) list = list.filter(r => (r.name || '').includes(filter.name));
  if (filter.payPlatform) list = list.filter(r => r.pay_platform === filter.payPlatform);
  return list;
}

function saveRechargePlan(item) {
  const d = getStorage();
  const id = item.id || Math.max(0, ...d.recharge_plans.map(x => x.id || 0)) + 1;
  const idx = d.recharge_plans.findIndex(x => x.id === id);
  const row = { ...item, id };
  if (idx >= 0) d.recharge_plans[idx] = row;
  else d.recharge_plans.push({ ...row, created_at: new Date().toISOString(), created_by: item.created_by || 0 });
  save(d);
  return id;
}

function deleteRechargePlan(id) {
  const d = getStorage();
  d.recharge_plans = d.recharge_plans.filter(x => x.id !== parseInt(id));
  save(d);
}

// 广告账户
function getAdAccounts(filter = {}) {
  let list = [...getStorage().ad_accounts].sort((a, b) => (b.id || 0) - (a.id || 0));
  if (filter.media) list = list.filter(r => r.media === filter.media);
  if (filter.country) list = list.filter(r => r.country === filter.country);
  if (filter.subject) list = list.filter(r => (r.subject_name || '').includes(filter.subject));
  if (filter.accountId) list = list.filter(r => (r.account_id || '').includes(filter.accountId));
  if (filter.accountName) list = list.filter(r => (r.account_name || '').includes(filter.accountName));
  if (filter.keyword) {
    const k = String(filter.keyword).toLowerCase();
    list = list.filter(r =>
      (r.account_id || '').toLowerCase().includes(k) ||
      (r.account_name || '').toLowerCase().includes(k) ||
      (r.subject_name || '').toLowerCase().includes(k)
    );
  }
  return list;
}

function saveAdAccount(item) {
  const d = getStorage();
  const id = item.id || Math.max(0, ...d.ad_accounts.map(x => x.id || 0)) + 1;
  const idx = d.ad_accounts.findIndex(x => x.id === id);
  const row = { ...item, id };
  if (idx >= 0) d.ad_accounts[idx] = row;
  else d.ad_accounts.push({ ...row, created_at: new Date().toISOString(), created_by: item.created_by || 0 });
  save(d);
  return id;
}

function deleteAdAccount(id) {
  const d = getStorage();
  d.ad_accounts = d.ad_accounts.filter(x => x.id !== parseInt(id));
  save(d);
}

// 初始化演示数据
function initDemoData() {
  const d = getStorage();
  if (!d.recharge_plans.length) {
    d.recharge_plans = [
      { id: 1, name: '基础包', bean_count: 100, extra_bean: 10, amount: 6, recharge_info: '100金豆+10赠送', pay_platform: 'stripe', created_at: new Date().toISOString(), created_by: 1 },
      { id: 2, name: '进阶包', bean_count: 500, extra_bean: 80, amount: 28, recharge_info: '500金豆+80赠送', pay_platform: 'stripe', created_at: new Date().toISOString(), created_by: 1 },
    ];
    save(d);
  }
  if (!d.recharge_records.length && d.recharge_plans.length) {
    d.recharge_records = [
      { id: 1, user_id: 1, drama_id: 1, drama_name: '闪婚成宠', amount: 6, pay_status: 'success', is_first: '是', is_callback: '是', promote_id: 'P001', promote_link_id: 'L001', promote_name: '测试推广', order_no: 'ORD001', platform: 'TikTok', country: 'ID', account_id: '248528', consume: 7.2, local_time: new Date().toISOString(), created_at: new Date().toISOString() },
      { id: 2, user_id: 2, drama_id: 1, drama_name: '闪婚成宠', amount: 12, pay_status: 'success', is_first: '否', is_callback: '是', promote_id: 'P001', promote_link_id: 'L001', promote_name: '测试推广', order_no: 'ORD002', platform: 'TikTok', country: 'ID', account_id: '248528', consume: 14.4, local_time: new Date().toISOString(), created_at: new Date().toISOString() },
    ];
    save(d);
  }
  if (!d.recharge_plan_groups.length) {
    d.recharge_plan_groups = [
      { id: 1, name: '默认方案组', plan_ids: [1, 2], created_at: new Date().toISOString() },
    ];
    save(d);
  }
  if (!(d.callback_logs || []).length) {
    d.callback_logs = [
      { id: 1, order_no: 'M1773102059894', event_type: '完成支付', pixel_id: 'D5FJNEBC77UE3OGHS', status: '成功', error_message: '', retry_count: 0, created_at: new Date().toISOString(), send_time: new Date().toISOString() },
      { id: 2, order_no: 'M1773102059895', event_type: '下单', pixel_id: 'D5FJNEBC77UE3OGHS', status: '成功', error_message: '', retry_count: 0, created_at: new Date().toISOString(), send_time: new Date().toISOString() },
    ];
    save(d);
  }
  if (!d.ad_accounts.length) {
    d.ad_accounts = [
      { id: 1, media: 'Meta', country: 'US', subject_name: '启量', account_id: '7610716893074407425', account_name: '广告账户1', pixel: '', created_at: new Date().toISOString(), created_by: 1 },
    ];
    save(d);
  }
  if (!d.promotion_links.length) {
    d.promotion_links = [
      { id: 1, promote_id: 'P001', platform: 'Facebook', country: 'US', promote_name: '测试推广', drama_id: 1, plan_group_id: 1, bean_count: 100, free_episodes: 3, domain: 'chuhai1x3x4x5x8d', link_keyword: 'chuhai', created_at: new Date().toISOString(), created_by: 1 },
    ];
    save(d);
  }
}

// DramaBagus 对齐：获取主体/实体列表（用于批量工具下拉）
function getEntities() {
  const accounts = getStorage().ad_accounts || [];
  const set = new Set(accounts.map(a => (a.subject_name || '').trim()).filter(Boolean));
  return Array.from(set).sort();
}

module.exports = {
  getEntities,
  getRechargeRecords,
  getPromotionLinks,
  savePromotionLink,
  deletePromotionLink,
  getRechargePlanGroups,
  saveRechargePlanGroup,
  deleteRechargePlanGroup,
  getCallbackConfig,
  saveCallbackConfig,
  getCallbackLogs,
  addCallbackLog,
  getAdMaterials,
  saveAdMaterial,
  deleteAdMaterial,
  getTitlePacks,
  saveTitlePack,
  deleteTitlePack,
  getAdTasks,
  saveAdTask,
  deleteAdTask,
  getRechargePlans,
  saveRechargePlan,
  deleteRechargePlan,
  getAdAccounts,
  saveAdAccount,
  deleteAdAccount,
  initDemoData,
};
