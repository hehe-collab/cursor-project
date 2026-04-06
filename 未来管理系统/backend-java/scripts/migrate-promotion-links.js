/**
 * 迁移 ../backend/data/storage.json → promotion_links
 * 用法：在 backend-java/scripts 下 MYSQL_PASSWORD=xxx node migrate-promotion-links.js
 */
const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

function resolveStoragePath() {
  const legacy = path.join(__dirname, '../../backend/data/storage.json');
  const local = path.join(__dirname, '../data/storage.json');
  if (fs.existsSync(legacy)) return legacy;
  if (fs.existsSync(local)) return local;
  throw new Error('未找到 storage.json，请将文件放到 backend-java/data/storage.json');
}

function toDt(v) {
  if (v == null || v === '') return new Date();
  const d = new Date(v);
  return Number.isNaN(d.getTime()) ? new Date() : d;
}

function toCreatedBy(v) {
  if (v == null || v === '') return null;
  if (typeof v === 'number' && v === 0) return null;
  return String(v);
}

async function main() {
  const raw = JSON.parse(fs.readFileSync(resolveStoragePath(), 'utf8'));
  const rows = raw.promotion_links || [];
  if (!rows.length) {
    console.log('没有投放链接需要迁移');
    return;
  }

  const conn = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: process.env.MYSQL_PASSWORD || '',
    database: 'drama_system',
  });

  console.log(`迁移 ${rows.length} 条投放链接...`);
  for (const r of rows) {
    const promoteId = r.promote_id != null && String(r.promote_id).trim() !== '' ? String(r.promote_id).trim() : `legacy_${r.id}`;
    await conn.execute(
      `INSERT INTO promotion_links (
        id, promote_id, platform, country, promote_name, drama_id, plan_group_id,
        bean_count, free_episodes, preview_episodes, domain, drama_name, status,
        stat, amount, spend, target, created_by, created_at, updated_at
      ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
      ON DUPLICATE KEY UPDATE
        platform=VALUES(platform), country=VALUES(country), promote_name=VALUES(promote_name),
        drama_id=VALUES(drama_id), plan_group_id=VALUES(plan_group_id),
        bean_count=VALUES(bean_count), free_episodes=VALUES(free_episodes),
        preview_episodes=VALUES(preview_episodes), domain=VALUES(domain), drama_name=VALUES(drama_name),
        status=VALUES(status), stat=VALUES(stat), amount=VALUES(amount), spend=VALUES(spend),
        target=VALUES(target), created_by=VALUES(created_by), updated_at=VALUES(updated_at)`,
      [
        r.id,
        promoteId,
        r.platform || '',
        r.country || '',
        r.promote_name || '',
        r.drama_id ?? null,
        r.plan_group_id ?? null,
        r.bean_count ?? r.beans_per_episode ?? 0,
        r.free_episodes ?? 0,
        r.preview_episodes ?? 0,
        r.domain || r.promo_domain || '',
        r.drama_name || '',
        r.status === 'inactive' ? 'inactive' : 'active',
        r.stat ?? null,
        r.amount ?? null,
        r.spend ?? null,
        r.target ?? null,
        toCreatedBy(r.created_by),
        toDt(r.created_at),
        toDt(r.updated_at || r.created_at),
      ]
    );
  }
  await conn.end();
  console.log('完成');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
