/**
 * 将 storage.json 中的 recharge_plan_groups 及 plan 关联导入 MySQL。
 * 请先执行 schema.sql 中的表结构，并优先跑完 migrate-recharge-plans.js。
 *
 * 用法：MYSQL_PASSWORD=xxx node migrate-recharge-groups.js
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

async function main() {
  const raw = JSON.parse(fs.readFileSync(resolveStoragePath(), 'utf8'));
  const groups = raw.recharge_plan_groups || [];
  if (!groups.length) {
    console.log('没有充值方案组需要迁移');
    return;
  }

  const conn = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: process.env.MYSQL_PASSWORD || '',
    database: 'drama_system',
  });

  console.log(`迁移 ${groups.length} 条充值方案组...`);
  for (const g of groups) {
    const resolvedName =
      (g.group_name != null && String(g.group_name).trim() !== '')
        ? String(g.group_name).trim()
        : (g.name != null && String(g.name).trim() !== '' ? String(g.name).trim() : '');
    const publicId =
      g.group_id != null && String(g.group_id).trim() !== ''
        ? String(g.group_id).trim()
        : `RG_migrated_${g.id}`;
    const st = g.status === 'inactive' ? 'inactive' : 'active';
    const planIds = Array.isArray(g.plan_ids)
      ? g.plan_ids.map((x) => parseInt(x, 10)).filter((n) => !Number.isNaN(n))
      : [];

    await conn.execute(
      `INSERT INTO recharge_plan_groups (
        id, name, group_name, group_public_id, sort_order, description, status, group_uuid,
        item_no, item_token, media_platform, pixel_id, pixel_token,
        creator, created_by, created_by_name, created_at, updated_at
      ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, COALESCE(?, NOW()), COALESCE(?, NOW()))
      ON DUPLICATE KEY UPDATE
        name=VALUES(name), group_name=VALUES(group_name), group_public_id=VALUES(group_public_id),
        sort_order=VALUES(sort_order), description=VALUES(description), status=VALUES(status),
        group_uuid=VALUES(group_uuid), item_no=VALUES(item_no), item_token=VALUES(item_token),
        media_platform=VALUES(media_platform), pixel_id=VALUES(pixel_id), pixel_token=VALUES(pixel_token),
        creator=VALUES(creator), created_by=VALUES(created_by), created_by_name=VALUES(created_by_name),
        updated_at=VALUES(updated_at)`,
      [
        g.id,
        resolvedName,
        resolvedName,
        publicId,
        g.sort_order != null ? Number(g.sort_order) : 999,
        g.description || '',
        st,
        g.group_uuid || null,
        g.item_no != null ? String(g.item_no) : '',
        g.item_token || '',
        g.media_platform != null ? String(g.media_platform) : '',
        g.pixel_id != null ? String(g.pixel_id) : '',
        g.pixel_token != null ? String(g.pixel_token) : '',
        g.creator || g.created_by_name || 'admin',
        g.created_by ?? null,
        g.created_by_name || '',
        g.created_at ? new Date(String(g.created_at).replace(' ', 'T')) : null,
        g.updated_at ? new Date(String(g.updated_at).replace(' ', 'T')) : null,
      ],
    );

    await conn.execute('DELETE FROM recharge_plan_group_plans WHERE group_id = ?', [g.id]);
    let i = 0;
    for (const pid of planIds) {
      await conn.execute(
        'INSERT INTO recharge_plan_group_plans (group_id, plan_id, sort_order) VALUES (?,?,?)',
        [g.id, pid, i++],
      );
    }
  }
  await conn.end();
  console.log('recharge_plan_groups 迁移完成');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
