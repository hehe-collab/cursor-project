# drama-admin（Java / Spring Boot）

**Spring Boot 3.2.x** + **MyBatis** + **MySQL `drama_system`**；默认端口 **3001**。原 Node **`backend/`** 已移除，全部 **`/api/*`** 由本服务提供。

## 已完成模块（指令 #036～#056，2026-04）

| 模块 | Controller | 说明 | 指令 |
|------|------------|------|------|
| 充值记录 | `RechargeController` | CRUD、统计、筛选 | #036 |
| 用户 | `UserController` | CRUD、统计、注册时间筛 | #037 |
| 短剧与分集 | `DramaController` | CRUD、分集 REST、统计 | #038 |
| 分类 | `CategoryController` | CRUD、统计、列表双形态 | #039 |
| 标签 | `TagController` | CRUD、统计、`drama_tags` | #040 |
| 站点设置 | `SettingController` | 扁平 key-value、UPSERT | #041 |
| 看板 | `DashboardController` | 聚合、`/trends`、推广明细 Demo | #042 |
| 路由规范 | 各 Controller | 固定路径优先；`/{id:\\d+}` 防 `/stats` 冲突 | #043 |
| 认证 | `AuthController` | `POST /login`、`GET /me`、`POST /logout` | #045 |
| 充值方案 | `RechargePlanController` | 列表分页、CRUD；表 `recharge_plans` | #046 |
| 充值方案组 | `RechargePlanGroupController` | 列表/详情/CRUD、`/frontend`、`batch-delete`、`test-pixel` | #047 |
| 投放链接 | `PromotionLinkController` | `/api/delivery-links`，导出/复制/批量状态 | #048 |
| 广告账户 | `AdAccountController` | `/api/accounts`，主体/国家/导出 | #049 |
| 标题包 | `TitlePackController` | `/api/title-pack`，批量删、导出 | #050 |
| 广告素材 | `AdMaterialController` | `/api/ad-material`，上传记录、multipart | #051 |
| 上传访问 | `UploadsController` | `GET /api/uploads/{file}`（封面免 JWT） | #051 |
| 回传 | `CallbackController` | `/api/callback`：config/logs/stats | #052 |
| 广告任务 | `AdTaskController` | `/api/ad-task`：列表/详情/导出/创建 | #053 |
| 阿里云 VOD | `VodController` | `/api/vod/*`（GET，与历史 Node 一致） | #054 |
| 按日 ROI 统计 | `StatsController` | `GET /api/stats`（充值按日聚合） | #055 |

另：`HealthController`。

**数据迁移**：在 **`scripts/`** 执行 **`npm install`**（本地 **`mysql2`**）。脚本依次从 **`../../backend/data/storage.json`**（若仍存在）或 **`../data/storage.json`** 读取：**`migrate-recharge-plans.js`**、**`migrate-recharge-groups.js`**、**`migrate-promotion-links.js`**、**`migrate-ad-accounts.js`**、**`migrate-title-packs.js`**、**`migrate-ad-materials.js`**、**`migrate-callback.js`**、**`migrate-ad-tasks.js`**。请将旧 **`storage.json`** 放到 **`backend-java/data/`** 若已删除原 **`backend/`** 目录。  
**`recharge_records.platform`**：用于 **`/api/stats`** 的 **`media`** 筛选（对齐 Node）。  
素材文件目录：**`app.upload-dir`**（默认 **`./uploads`**）。

## 快速开始

1. 安装 **JDK 17、Maven、MySQL**，建库 **`drama_system`**。  
2. 导入表结构：`mysql -u root -p drama_system < src/main/resources/sql/schema.sql`  
3.（可选）导入演示数据：见下文 **「导入演示数据」**。  
4. `source scripts/use-java17.sh && export MYSQL_PASSWORD=...`（若 root 有密码）后执行 **`mvn spring-boot:run`**。  
5. 访问 <http://localhost:3001/api/health>；登录 **`POST /api/auth/login`**（默认 **`admin` / `admin123`** 在首次启动时写入）。

## 环境

- JDK **17**（推荐；Maven Enforcer 要求 **≥17**）
- Maven 3.9+
- MySQL 8/9，数据库 `drama_system`

### macOS：终端里 `java` / `mvn` 找不到，或运行成了 Java 23

Homebrew 安装的 JDK 往往不在默认 PATH；请先 **固定 JAVA_HOME 为 JDK 17**，再执行 Maven：

```bash
cd 未来管理系统/backend-java
source scripts/use-java17.sh
java -version   # 应显示 17.x
mvn -DskipTests package
mvn spring-boot:run
```

若未安装 JDK 17：`brew install openjdk@17`，并按 `brew info openjdk@17` 提示把 `JAVA_HOME` 指到该版本。  
Maven / MySQL 若未在 PATH：`brew install maven mysql`，一般位于 `/opt/homebrew/bin`（Apple Silicon）或 `/usr/local/bin`（Intel）。

### `zsh: command not found: mvn` / `mysql`

通常是当前 shell **未加载 Homebrew 的 PATH**（新开终端、某些 IDE 内置终端、仅 `source` 了 Python `.venv` 等）。任选其一：

```bash
# 临时（当前终端）
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
```

或按 Homebrew 提示，在 `~/.zprofile` 中加入 `eval "$(/opt/homebrew/bin/brew shellenv)"` 后重开终端。确认：`which mvn` → `/opt/homebrew/bin/mvn`。

### `Port 3001 was already in use`

说明本机已有进程占用 3001（多为上次未退出的 Spring Boot）。先释放端口再启动：

```bash
lsof -ti:3001 | xargs kill -9
```

## 数据库（首次）

在 MySQL 中建库 `drama_system`（若尚无），再导入 DDL：

```bash
mysql -u root -p drama_system < src/main/resources/sql/schema.sql
```

表数量以 **`schema.sql`** 为准（含 **`recharge_plans`、`recharge_plan_groups`、`recharge_plan_group_plans`、`promotion_links`、`ad_accounts`、`title_packs`、`ad_materials`、`ad_material_records`** 等）；说明见协作文档 §5 / §14。

首次启动会自动插入默认管理员 **admin** / **admin123**（若 `admins` 表尚无 `admin` 用户）。

## 导入演示数据

快速生成 **10 个测试用户、20 条充值记录、5 个分类、10 个标签、5 部短剧（DEMO_ 前缀）**：

```bash
cd 未来管理系统/backend-java
# 方式 1：脚本（无密码可省略 MYSQL_PASSWORD）
export MYSQL_PASSWORD=你的root密码   # 若 MySQL root 无密码可不设
chmod +x scripts/import-demo-data.sh   # 仅首次
./scripts/import-demo-data.sh
# 若提示无执行权限或脚本异常，可改用：bash scripts/import-demo-data.sh

# 方式 2：手动（密码须与 MySQL root 一致；无密码时不要加 -p）
mysql -u root -p drama_system < src/main/resources/sql/demo_data.sql
# 或 root 无密码：
# mysql -u root drama_system < src/main/resources/sql/demo_data.sql
```

导入后可测（需先登录拿 `token`；有 `jq` 时可简化）：

```bash
TOKEN=$(curl -s -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

curl -s "http://localhost:3001/api/users?page=1&pageSize=10" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:3001/api/recharge/stats" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:3001/api/recharge?page=1&pageSize=10" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:3001/api/dramas?page=1&pageSize=20" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:3001/api/categories" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:3001/api/tags" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:3001/api/dashboard/stats?start_date=2026-04-01&end_date=2026-04-05" -H "Authorization: Bearer $TOKEN"
```

说明：演示数据脚本会先删除同批 **`demo_user_%`** 用户及 **`DEMO_`** 短剧等再插入，可重复执行。若出现 **`Duplicate entry '501-1' for key 'drama_episodes.uk_drama_episode'`**，请使用最新 **`demo_data.sql`**（已显式清理 **`drama_episodes`**），或先手动：`DELETE FROM drama_episodes WHERE drama_id BETWEEN 501 AND 505;` 再导入。

**`ERROR 1045 Access denied`**：多为 **`mysql -p`** 输入的密码与真实 root 密码不一致；与 **`import-demo-data.sh`** 相同方式时，请 **`export MYSQL_PASSWORD=...`** 后执行脚本（无密码则不要设置 **`MYSQL_PASSWORD`**）。

### 演示数据包含什么（ID 与当前 `demo_data.sql` 一致）

| 类型 | 说明 |
|------|------|
| 用户 | **10** 条，**id 101～110**（`demo_user_1`～`demo_user_10`） |
| 充值记录 | **20** 条，**id 910001～910020**，订单号 **`DEMO_ORD001`～`DEMO_ORD020`** |
| 分类 | **5** 条，**id 301～305**（爱情、悬疑、喜剧、动作、科幻） |
| 标签 | **10** 条，**id 401～410** |
| 短剧 | **5** 条，**id 501～505**（标题 **`DEMO_` 前缀**） |
| 剧集 | 当前脚本仅为剧 **501** 插入 **2** 条 **`drama_episodes`**（用于详情验收） |

### 导入后快速验证（需后端已启动、已导入演示数据）

```bash
TOKEN=$(curl -s -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')

curl -s "http://localhost:3001/api/dramas?page=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.total'
curl -s "http://localhost:3001/api/dramas/501" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.episodes | length'
curl -s "http://localhost:3001/api/users?page=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.total'
curl -s "http://localhost:3001/api/recharge/stats" \
  -H "Authorization: Bearer $TOKEN" | jq '.data'
```

期望：**`dramas.total`=5**，**`episodes` 长度=2**，**`users.total`=10**，**`total_count`=20** 且 **`pending_count`=3**。

### 常见问题

| 现象 | 处理 |
|------|------|
| **`ERROR 1062` … `501-1` / `uk_drama_episode`** | 使用最新 **`demo_data.sql`**（已清理 **`drama_episodes`**）；或见协作文档 **§15.1** |
| **`ERROR 1045`** | 核对 **`MYSQL_PASSWORD`** / 勿对无密码 root 使用 **`-p`**；见 **§15.1**（协作文档） |

### 前端与双后端

`frontend/vite.config.js` 已按 **URL 前缀** 分流（**非**整段 `/api` 单一目标）：例如 `/api/users`、`/api/recharge`、`/api/dramas`、`/api/delivery-links`、`/api/accounts`、`/api/ad-material`、`/api/title-pack`、`/api/uploads` 等走 **3001**；回调/VOD/统计/广告任务等走 **3000**。详见协作文档 **§5** 代理表。

## 目录结构（摘要）

```
backend-java/
├── src/main/java/com/drama/
│   ├── controller/     # REST（`RechargeController`、`UserController`、…）
│   ├── service/      # 业务
│   ├── mapper/       # MyBatis 接口
│   ├── entity/、dto/、common/、config/、exception/
├── src/main/resources/
│   ├── mapper/*.xml
│   ├── sql/schema.sql、demo_data.sql
│   └── application.yml
└── scripts/use-java17.sh、import-demo-data.sh
```

## 开发规范（摘要）

- **返回**：统一 `Result`（`code` / `message` / `data`）；对外 JSON 多为 **snake_case**。
- **路由**：固定路径（如 `/stats`）先于 **`@GetMapping`** 列表；详情使用 **`/{id:\\d+}`** 避免与字面路径冲突（指令 **#043**）。
- **分类/标签列表**：无 `page`/`pageSize` 时 `data` 为数组；有分页时为 `{ list, total, page, pageSize }`。

## 配置

编辑 `src/main/resources/application.yml`，或通过环境变量设置密码与 JWT：

```bash
export MYSQL_PASSWORD=你的root密码
export JWT_SECRET=你的密钥   # 可选，默认见 application.yml
mvn spring-boot:run
```

## 运行

```bash
cd 未来管理系统/backend-java
source scripts/use-java17.sh   # 可选，见上文
mvn spring-boot:run
```

验证：<http://localhost:3001/api/health>

### 核心 API 一览（除登录、健康检查外需 `Authorization: Bearer <token>`）

路径中的 **`{id}`** 实为数字约束 **`\\d+`**（与 OpenAPI 表述等价）。**`GET/POST/PUT/DELETE`** 未逐行展开了的仍见各 Controller。

| 说明 | 方法 | 路径 |
|------|------|------|
| 登录 | POST | `/api/auth/login` |
| 健康 | GET | `/api/health` |
| 用户统计 / 列表 / CRUD | GET… | `/api/users/stats`、`/api/users`、`/api/users/{id}` … |
| 充值统计 / 列表 / CRUD | GET… | `/api/recharge/stats`、`/api/recharge`、`/api/recharge/{id}` … |
| 短剧统计 / 列表 / CRUD / 分集 | GET… | `/api/dramas/stats`、`/api/dramas`、`/api/dramas/{id}`、`/api/dramas/{id}/episodes` … |
| 分类统计 / 双形态列表 / CRUD | GET… | `/api/categories/stats`、`/api/categories`、… |
| 标签统计 / 双形态列表 / CRUD | GET… | `/api/tags/stats`、`/api/tags`、… |
| 站点设置 | GET/POST/PUT | `/api/settings`、`/api/settings/{key}` … |
| 看板 | GET | `/api/dashboard`、`/api/dashboard/stats`（**`start_date`/`end_date` 可选**，缺省近 7 天）、`/api/dashboard/trends`、`/api/dashboard/promotion-details` |

完整验收示例与字段说明：**`未来管理系统/协作文档/未来管理系统-完成情况与协作指南.md`**（**§5.1**）。

## 构建与打包

```bash
mvn -DskipTests compile    # 编译
mvn -DskipTests package    # 打包 → target/drama-admin-0.0.1-SNAPSHOT.jar
java -jar target/drama-admin-0.0.1-SNAPSHOT.jar
```

## 相关文档

- **`../backend/README.md`** — Node 侧与「仍由 Node 提供」的前缀  
- **`../协作文档/Java后端迁移完成说明.md`** — 迁移小结  
- **`../协作文档/未来管理系统-完成情况与协作指南.md`**
