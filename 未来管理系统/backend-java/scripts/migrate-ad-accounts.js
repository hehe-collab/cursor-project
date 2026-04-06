/**
 * 迁移 storage.json.ad_accounts → ad_accounts
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

async function main() {
  const raw = JSON.parse(fs.readFileSync(resolveStoragePath(), 'utf8'));
  const rows = raw.ad_accounts || [];
  if (!rows.length) {
    console.log('没有广告账户需要迁移');
    return;
  }

  const conn = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: process.env.MYSQL_PASSWORD || '',
    database: 'drama_system',
  });

  console.log(`迁移 ${rows.length} 条广告账户...`);
  for (const r of rows) {
    await conn.execute(
      `INSERT INTO ad_accounts (
        id, media, country, subject_name, account_id, account_name, media_alias, account_agent,
        created_by, created_by_name, created_at, updated_at
      ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
      ON DUPLICATE KEY UPDATE
        media=VALUES(media), country=VALUES(country), subject_name=VALUES(subject_name),
        account_id=VALUES(account_id), account_name=VALUES(account_name),
        media_alias=VALUES(media_alias), account_agent=VALUES(account_agent),
        created_by_name=VALUES(created_by_name), updated_at=VALUES(updated_at)`,
      [
        r.id,
        r.media || '',
        r.country || '',
        r.subject_name || '',
        r.account_id || '',
        r.account_name || '',
        r.media_alias || '',
        r.account_agent || '',
        r.created_by ?? null,
        r.created_by_name ?? null,
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
