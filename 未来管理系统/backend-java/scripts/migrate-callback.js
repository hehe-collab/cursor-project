/**
 * 迁移 storage.json.callback_configs / callback_logs → MySQL
 * 依赖：本目录 npm install（mysql2）
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

async function main() {
  const storagePath = resolveStoragePath();
  const raw = JSON.parse(fs.readFileSync(storagePath, 'utf8'));
  const configs = raw.callback_configs || [];
  const logs = raw.callback_logs || [];

  const conn = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: process.env.MYSQL_PASSWORD || '',
    database: "drama_system",
  });

  if (configs.length) {
    console.log(`迁移 ${configs.length} 条回传配置...`);
    for (const c of configs) {
      await conn.execute(
        `INSERT INTO callback_configs (
          id, link_id, platform, cold_start_count, min_price_limit,
          replenish_callback_enabled, config_json,
          creator, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
          link_id = VALUES(link_id),
          platform = VALUES(platform),
          cold_start_count = VALUES(cold_start_count),
          min_price_limit = VALUES(min_price_limit),
          replenish_callback_enabled = VALUES(replenish_callback_enabled),
          config_json = VALUES(config_json),
          creator = VALUES(creator),
          updated_at = VALUES(updated_at)`,
        [
          c.id,
          String(c.link_id || ''),
          String(c.platform || ''),
          c.cold_start_count ?? 0,
          c.min_price_limit ?? 0,
          c.replenish_callback_enabled === 0 || c.replenish_callback_enabled === false ? 0 : 1,
          c.config_json != null && String(c.config_json).length
            ? String(c.config_json)
            : null,
          String(c.creator || 'admin'),
          toDt(c.created_at),
          toDt(c.updated_at || c.created_at),
        ],
      );
    }
  }

  if (logs.length) {
    console.log(`迁移 ${logs.length} 条回传日志...`);
    for (const log of logs) {
      const orderNo = log.order_no || log.orderNo || log.order_id || '';
      await conn.execute(
        `INSERT INTO callback_logs (
          id, order_no, order_id, event, event_type, pixel_id, status, error_message,
          retry_count, send_time, sent_at, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
          order_no = VALUES(order_no),
          order_id = VALUES(order_id),
          event = VALUES(event),
          event_type = VALUES(event_type),
          pixel_id = VALUES(pixel_id),
          status = VALUES(status),
          error_message = VALUES(error_message),
          retry_count = VALUES(retry_count),
          send_time = VALUES(send_time),
          sent_at = VALUES(sent_at)`,
        [
          log.id,
          orderNo,
          log.order_id != null ? String(log.order_id) : orderNo,
          log.event || null,
          log.event_type || log.event || null,
          log.pixel_id || null,
          String(log.status || ''),
          log.error_message || null,
          log.retry_count ?? 0,
          log.send_time ? toDt(log.send_time) : null,
          (log.sent_at || log.send_time) ? toDt(log.sent_at || log.send_time) : null,
          toDt(log.created_at),
        ],
      );
    }
  }

  console.log('回传迁移完成');
  await conn.end();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
