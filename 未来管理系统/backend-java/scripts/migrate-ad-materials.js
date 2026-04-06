/**
 * 迁移 storage.json.ad_materials → ad_materials；
 * material_records → ad_material_records
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
  const materials = raw.ad_materials || [];
  const records = raw.material_records || [];

  const conn = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: process.env.MYSQL_PASSWORD || '',
    database: 'drama_system',
  });

  if (materials.length) {
    console.log(`迁移 ${materials.length} 条广告素材...`);
    for (const r of materials) {
      const mid =
        r.material_id != null && String(r.material_id).trim() !== ''
          ? String(r.material_id).trim()
          : `MAT${r.id}`;
      await conn.execute(
        `INSERT INTO ad_materials (
          id, material_id, material_name, type, entity_name, account_id, video_id, cover_url,
          created_by, created_at, updated_at
        ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
        ON DUPLICATE KEY UPDATE
          material_name=VALUES(material_name), type=VALUES(type), entity_name=VALUES(entity_name),
          account_id=VALUES(account_id), video_id=VALUES(video_id), cover_url=VALUES(cover_url),
          updated_at=VALUES(updated_at)`,
        [
          r.id,
          mid,
          r.material_name || r.name || '',
          r.type || 'image',
          r.entity_name || '',
          r.account_id || r.accountId || '',
          r.video_id || r.videoId || '',
          r.cover_url || r.url || '',
          r.created_by ?? null,
          toDt(r.created_at),
          toDt(r.updated_at || r.created_at),
        ]
      );
    }
  } else {
    console.log('没有广告素材需要迁移');
  }

  if (records.length) {
    console.log(`迁移 ${records.length} 条素材任务记录...`);
    for (const r of records) {
      await conn.execute(
        `INSERT INTO ad_material_records (
          id, account_id, account_name, status, task_type, detail, created_at
        ) VALUES (?,?,?,?,?,?,?)
        ON DUPLICATE KEY UPDATE
          account_id=VALUES(account_id), account_name=VALUES(account_name),
          status=VALUES(status), task_type=VALUES(task_type), detail=VALUES(detail)`,
        [
          r.id,
          r.account_id || r.accountId || '',
          r.account_name || r.accountName || '',
          r.status || 'pending',
          r.task_type || r.taskType || 'upload',
          r.detail != null ? String(r.detail) : '',
          toDt(r.created_at),
        ]
      );
    }
  }

  await conn.end();
  console.log('完成');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
