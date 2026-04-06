/**
 * 迁移 storage.json.title_packs → title_packs
 */
const fs = require('fs');
const path = require('path');
const mysql = require(path.join(__dirname, '../../backend/node_modules/mysql2/promise'));

function toDt(v) {
  if (v == null || v === '') return new Date();
  const d = new Date(v);
  return Number.isNaN(d.getTime()) ? new Date() : d;
}

async function main() {
  const storagePath = path.join(__dirname, '../../backend/data/storage.json');
  const raw = JSON.parse(fs.readFileSync(storagePath, 'utf8'));
  const rows = raw.title_packs || [];
  if (!rows.length) {
    console.log('没有标题包需要迁移');
    return;
  }

  const conn = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: process.env.MYSQL_PASSWORD || '',
    database: 'drama_system',
  });

  console.log(`迁移 ${rows.length} 条标题包...`);
  for (const r of rows) {
    const name = r.name != null && r.name !== '' ? r.name : r.title || '';
    const content = r.content != null && r.content !== '' ? r.content : r.group || '';
    await conn.execute(
      `INSERT INTO title_packs (id, name, content, created_by, created_by_name, created_at, updated_at)
       VALUES (?,?,?,?,?,?,?)
       ON DUPLICATE KEY UPDATE
         name=VALUES(name), content=VALUES(content), created_by_name=VALUES(created_by_name),
         updated_at=VALUES(updated_at)`,
      [
        r.id,
        name,
        content,
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
