# Java 后端迁移完成说明

> **时间**：2026-04  
> **状态**：**迁移已完成**。指令 **#036～#056**：核心业务、认证、充值方案/组、投放/账户/标题包/素材、回传、广告任务、VOD、按日统计均为 **Java + MySQL**；**Node `backend/` 目录已删除**。

---

## 迁移概况

### 已由 Java 承接的模块

| 模块 | 指令 | Java Controller | 说明 |
|------|------|-----------------|------|
| 充值记录 | #036 | `RechargeController` | CRUD、聚合统计、筛选；含 **`platform`** 字段 |
| 用户 | #037 | `UserController` | CRUD、统计 |
| 短剧与分集 | #038 | `DramaController` | CRUD、分集 REST、统计 |
| 分类 | #039 | `CategoryController` | CRUD、统计 |
| 标签 | #040 | `TagController` | CRUD、`drama_tags` |
| 站点设置 | #041 | `SettingController` | `settings` UPSERT |
| 看板 | #042 | `DashboardController` | 聚合、`/trends`、`promotion-details` |
| 路由防冲突 | #043 | 各 Controller | `/stats` 与 `/{id:\\d+}` |
| 认证 | #045 | `AuthController` | 登录 / me / logout |
| 充值方案/组 | #046～#047 | `RechargePlan*`、`RechargePlanGroup*` | |
| 投放 / 账户 / 标题包 / 素材 | #048～#051 | `PromotionLink*`、`AdAccount*`、`TitlePack*`、`AdMaterial*`、`UploadsController` | |
| 回传 | #052 | `CallbackController` | `callback_configs`、`callback_logs` |
| 广告任务 | #053 | `AdTaskController` | `ad_tasks`、Excel 导出 |
| 阿里云 VOD | #054 | `VodController` | `aliyun-java-sdk-vod` |
| 按日统计 | #055 | `StatsController` | `GET /api/stats`（与 Node 算法一致） |
| 收尾 | #056 | — | 删除 Node **`backend/`**；Vite **`/api` → 3001**；根 **`package.json`** 仅 Java+前端 |

另：`HealthController`。

### 开发与代理

- **`frontend/vite.config.js`**：**`/api` 单一代理 → `http://localhost:3001`**。  
- **根目录 `npm run start`**：**`concurrently`** 启动 **`backend-java`** + **`frontend`**（**`backend-java`** 脚本内含 **`source scripts/use-java17.sh`**）。

### 数据迁移

- 脚本目录：**`backend-java/scripts`**，先 **`npm install`**。  
- **`storage.json` 路径**：优先 **`backend/data/storage.json`**（若你本地仍保留该备份路径），否则 **`backend-java/data/storage.json`**。  
- 详见 **`backend-java/README.md`** 迁移列表。

---

## 技术约定（摘要）

- **统一响应**：`Result`（`code` / `message` / `data`）。  
- **路由**：固定路径优先；数值详情 **`/{id:\\d+}`**（**#043**）。  
- **素材封面**：**`GET /api/uploads/*`** **无需 JWT**。  
- **VOD**：环境变量 **`ALIYUN_ACCESS_KEY_ID`**、**`ALIYUN_ACCESS_KEY_SECRET`**、**`ALIYUN_VOD_REGION`**（默认 **cn-shanghai**）。

---

## 文档与代码入口

| 路径 | 用途 |
|------|------|
| `协作文档/未来管理系统-完成情况与协作指南.md` | 主协作文档 §5 / §14 |
| `backend-java/README.md` | 启动、模块、迁移命令 |
| 仓库根 `README.md` | 一键启动与结构说明 |

---

*本文档与协作文档 §13/§14 同步维护。*
