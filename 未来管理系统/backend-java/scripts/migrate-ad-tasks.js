/**
 * 迁移 storage.json.ad_tasks → ad_tasks
 */
const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

function resolveStoragePath() {
  const legacy = path.join(__dirname, '../../backend/data/storage.json');
  const local = path.join(__dirname, '../data/storage.json');
  if (fs.existsSync(legacy)) return legacy;
  if (fs.existsSync(local)) return local;
  throw new Error(
    '未找到 storage.json：请将原 backend/data/storage.json 复制到 backend-java/data/storage.json',
  );
}

function toDt(v) {
  if (v == null || v === '') return new Date();
  const d = new Date(v);
  return Number.isNaN(d.getTime()) ? new Date() : d;
}

function taskIdOf(t) {
  if (t.task_id != null && String(t.task_id).trim() !== '') return String(t.task_id).trim();
  if (t.id != null) return String(t.id);
  return `T${Date.now()}`;
}

async function main() {
  const raw = JSON.parse(fs.readFileSync(resolveStoragePath(), 'utf8'));
  const tasks = raw.ad_tasks || [];
  if (!tasks.length) {
    console.log('没有广告任务需要迁移');
    return;
  }

  const conn = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: process.env.MYSQL_PASSWORD || '',
    database: 'drama_system',
  });

  console.log(`迁移 ${tasks.length} 条广告任务...`);
  for (const task of tasks) {
    const tid = taskIdOf(task);
    let configJson = null;
    if (task.config !== undefined && task.config !== null) {
      configJson = typeof task.config === 'string' ? task.config : JSON.stringify(task.config);
    }
    let status = task.status || 'running';
    await conn.execute(
      `INSERT INTO ad_tasks (
        task_id, account_ids, account_names, promotion_type, status, created_by, created_at, config_json
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        account_ids = VALUES(account_ids),
        account_names = VALUES(account_names),
        promotion_type = VALUES(promotion_type),
        status = VALUES(status),
        created_by = VALUES(created_by),
        config_json = VALUES(config_json)`,
      [
        tid,
        String(task.account_ids ?? ''),
        String(task.account_names ?? task.name ?? ''),
        String(task.promotion_type ?? ''),
        String(status),
        String(task.created_by ?? 'admin'),
        toDt(task.created_at || task.create_time),
        configJson,
      ],
    );
  }

  console.log('广告任务迁移完成');
  await conn.end();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
