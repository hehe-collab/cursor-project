/**
 * 将 storage.json 中的 recharge_plans 导入 MySQL drama_system.recharge_plans
 * 依赖：本目录 npm install（mysql2）
 *
 * 用法：MYSQL_PASSWORD=xxx node migrate-recharge-plans.js
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
  const plans = raw.recharge_plans || [];
  if (!plans.length) {
    console.log('没有充值方案需要迁移');
    return;
  }

  const conn = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: process.env.MYSQL_PASSWORD || '',
    database: 'drama_system',
  });

  console.log(`迁移 ${plans.length} 条充值方案...`);
  for (const p of plans) {
    const bean = p.bean_count ?? p.actual_coins ?? 0;
    const extra = p.extra_bean ?? p.bonus_coins ?? 0;
    const unlock = p.unlock_full_series === true || p.unlock_full_series === 'true' ? 1 : 0;
    const plat = p.pay_platform || p.payment_platform || '';
    const st = p.status === 'inactive' ? 'inactive' : 'active';
    const info = p.recharge_info || p.description || '';
    const desc = p.description || info;
    const uuid = p.uuid || null;
    await conn.execute(
      `INSERT INTO recharge_plans (
        id, name, bean_count, extra_bean, amount, recharge_info, pay_platform, currency, status,
        description, unlock_full_series, plan_uuid, created_by, created_by_name,
        is_recommended, is_hot, created_at, updated_at
      ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, COALESCE(?, NOW()), COALESCE(?, NOW()))
      ON DUPLICATE KEY UPDATE
        name=VALUES(name), bean_count=VALUES(bean_count), extra_bean=VALUES(extra_bean), amount=VALUES(amount),
        recharge_info=VALUES(recharge_info), pay_platform=VALUES(pay_platform), currency=VALUES(currency),
        status=VALUES(status), description=VALUES(description), unlock_full_series=VALUES(unlock_full_series),
        plan_uuid=VALUES(plan_uuid), created_by=VALUES(created_by), created_by_name=VALUES(created_by_name),
        is_recommended=VALUES(is_recommended), is_hot=VALUES(is_hot), updated_at=VALUES(updated_at)`,
      [
        p.id,
        p.name || '',
        Number(bean) || 0,
        Number(extra) || 0,
        p.amount != null ? Number(p.amount) : 0,
        info,
        plat,
        p.currency || 'USD',
        st,
        desc,
        unlock,
        uuid,
        p.created_by ?? null,
        p.created_by_name || '',
        p.is_recommended ? 1 : 0,
        p.is_hot ? 1 : 0,
        p.created_at ? new Date(p.created_at) : null,
        p.updated_at ? new Date(p.updated_at) : null,
      ],
    );
  }
  await conn.end();
  console.log('recharge_plans 迁移完成');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
