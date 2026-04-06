# 数据迁移脚本

## 依赖

```bash
cd "$(dirname "$0")"
npm install
```

使用 **`mysql2/promise`**（见 **`package.json`**），**不再依赖**已删除的 Node **`backend/node_modules`**。

## storage.json 路径

脚本按顺序尝试：

1. **`../../backend/data/storage.json`**（若你本地仍保留旧目录备份）
2. **`../data/storage.json`**（推荐：将备份放到 **`backend-java/data/storage.json`**）

## 环境与执行

```bash
export MYSQL_PASSWORD='你的root密码'   # 无密码可省略
node migrate-recharge-plans.js
node migrate-recharge-groups.js
node migrate-promotion-links.js
node migrate-ad-accounts.js
node migrate-title-packs.js
node migrate-ad-materials.js
node migrate-callback.js
node migrate-ad-tasks.js
```

顺序上：**方案组**依赖 **方案**表已有数据时可后跑；其余按业务依赖自行调整，通常与上表一致即可。

## 增量 DDL（旧库保留数据时）

**不要**使用与仓库不一致的通用「postback」表模板。请以 **`src/main/resources/sql/schema.sql`** 中 **§24～§26**（`callback_configs`、`callback_logs`、`ad_tasks`）及 **`recharge_records`** 的 **`platform`** 列为准：

- **`recharge_records`**：若表上尚无 **`platform`**，执行：  
  `ALTER TABLE recharge_records ADD COLUMN platform VARCHAR(64) NULL COMMENT '媒体/渠道（统计筛选用）' AFTER payment_method;`
- **三张新表**：从 **`schema.sql`** 复制对应 **`CREATE TABLE IF NOT EXISTS`** 整段执行（字段类型、主键 **`callback_logs.id`** 为 **BIGINT 非 AUTO** 等与 Java/迁移脚本一致）。

整库可清空时，直接用 **`schema.sql`** 全量重建更简单。

---

## 表缺失报错（如 `ad_accounts doesn't exist`）

- **根因**：`drama_system` 是迁移前建的，未包含 **`schema.sql`** 里 **#048～#053** 等表。  
- **推荐**：先 **`./scripts/backup-database.sh`**，再 **`mysql -u root -p drama_system < src/main/resources/sql/schema.sql`**（可清空时注意脚本里的 **`DROP TABLE`** 段）。  
- **仅补表（保留数据）**：使用仓库内与 **`schema.sql` 一致** 的增量脚本，**不要**使用网络泛用 DDL（字段名/类型与 Java 不一致会再次报错）：

```bash
mysql -u root -p drama_system < src/main/resources/sql/fix-missing-tables-after-migration.sql
```

脚本末尾为 **`recharge_records.platform`** 使用 **`information_schema` 动态 `ALTER`**（避免部分 **MySQL 9.x** 对 **`ADD COLUMN IF NOT EXISTS`** 报语法错误）。

缺 **`recharge_plans` / `recharge_plan_groups`**（充值方案页报错）时执行（与 **`schema.sql`** 一致，**不**动 **`recharge_records`**）：

```bash
mysql -u root -p drama_system < src/main/resources/sql/fix-recharge-tables.sql
```

---

## 指令 #057：环境与监控（摘要）

- **Profile**：默认 **`dev`**（`application-dev.yml`），生产设 **`SPRING_PROFILES_ACTIVE=prod`** 并完善 **`application-prod.yml`** 中的数据库密码与 **`JWT_SECRET`**。
- **启动**：**`./scripts/start-dev.sh`**（会 `source .env.dev`、`use-java17.sh`）或 **`mvn spring-boot:run`**。
- **Actuator**：**`GET /actuator/health`**、**`/actuator/prometheus`** 等；业务 JWT 不拦截 **`/actuator/**`**。
- **日志**：**`logback-spring.xml`**，目录由 **`LOG_PATH`**（默认 **`./logs`**）控制。
