/**
 * 从生产 API 拉取可用账户/素材/链接，生成最小 E2E 测试 Excel
 */
const fs = require('fs');
const path = require('path');
const XLSX = require('xlsx');
const config = require('../config');

async function login() {
  const res = await fetch(`${config.baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: config.username, password: config.password }),
  });
  const json = await res.json();
  if (!json.data?.token) throw new Error(`登录失败: ${json.message || 'unknown'}`);
  return json.data.token;
}

async function apiGet(token, apiPath) {
  const res = await fetch(`${config.baseUrl}/api/${apiPath}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json();
}

function pickList(payload) {
  const d = payload?.data;
  if (Array.isArray(d)) return d;
  if (Array.isArray(d?.list)) return d.list;
  if (Array.isArray(d?.records)) return d.records;
  return [];
}

async function main() {
  const token = await login();
  const accounts = pickList(await apiGet(token, 'accounts/executable-options'));
  if (!accounts.length) throw new Error('无可用 executable 账户');

  const account = accounts.find((a) => a.oauthStatus === 'authorized' || a.oauthStatus === 'active')
    || accounts[0];
  const entity = account.subjectName;
  const accountId = String(account.accountId || account.advertiserId);

  const pixels = pickList(await apiGet(token, 'tiktok/pixels'));
  const pixel = pixels.find((p) => String(p.advertiserId) === accountId)
    || pixels.find((p) => p.status === 'active')
    || pixels[0];
  const pixelName = pixel?.pixelName || 'JYHY-H5-ID-Pixel';

  const links = pickList(await apiGet(token, 'delivery-links?page=1&pageSize=20'));
  const link = links[0];
  const linkKeyword = link?.promo_name || link?.drama_name || 'test';

  const materials = pickList(await apiGet(token, 'ad-material?page=1&pageSize=50'));
  const material = materials.find((m) => String(m.accountId) === accountId) || materials[0];
  const materialKeyword = (material?.materialName || 'test').slice(0, 8);

  const titles = pickList(await apiGet(token, 'title-pack?page=1&pageSize=10'));
  const titleName = titles[0]?.name || '印尼27';

  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const startDate = tomorrow.toISOString().slice(0, 10);

  const row = {
    任务: 1,
    主体: entity,
    账户ID: accountId,
    Pixel: pixelName,
    已有项目: '',
    项目名称: `E2E测试-${Date.now().toString().slice(-6)}`,
    项目商品库: '关',
    广告名称: '',
    推广链接关键词: linkKeyword,
    素材关键词: materialKeyword,
    素材ID: '',
    标题: titleName,
    优化目标: '价值',
    出价: 1.2,
    预算: 100,
    广告组预算: 50,
    个数: 1,
    开始日期: startDate,
    开始时间: '03:00:00',
    年龄: '18+',
    认证身份: '',
    商品库: '',
    商品: '',
    启用: 'true',
    基于目标增加预算: '关',
    提交次数: 1,
  };

  const headers = Object.keys(row);
  const wb = XLSX.utils.book_new();
  const ws = XLSX.utils.json_to_sheet([row], { header: headers });
  XLSX.utils.book_append_sheet(wb, ws, '任务列表');

  const out = path.join(__dirname, '..', 'data', 'tasks-e2e.xlsx');
  XLSX.writeFile(wb, out);

  console.log('✅ E2E Excel 已生成:', out);
  console.log(JSON.stringify(row, null, 2));
}

main().catch((err) => {
  console.error('❌', err.message);
  process.exit(1);
});
