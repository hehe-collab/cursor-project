# 未来管理系统

影视/短剧管理后台（DramaBagus 风格）。后端已 **统一为 Java + Spring Boot + MySQL**（端口 **3001**）；原 Node **`backend/`** 目录已移除，历史数据请使用 **`backend-java/scripts/`** 迁移脚本从备份的 **`storage.json`** 导入。

## 架构一览

| 服务 | 端口 | 技术 | 数据 |
|------|------|------|------|
| 前端 | **5173** | Vue 3 + Vite + Element Plus | — |
| Java | **3001** | Spring Boot 3.2 + MyBatis | MySQL **`drama_system`** |

开发环境：**`frontend/vite.config.js`** 将 **`/api/*`** 全部代理到 **http://localhost:3001**。

迁移说明汇总：**`协作文档/Java后端迁移完成说明.md`**；协作规则：**`协作文档/未来管理系统-完成情况与协作指南.md`**。

## 项目结构

```
未来管理系统/
├── frontend/                 # 前端
├── backend-java/             # Java（README：backend-java/README.md）
├── 协作文档/
├── package.json              # npm run start：Java + 前端
└── README.md
```

## 快速开始

**依赖**：Node 18+、JDK 17、Maven 3.9+、MySQL 8+。

```bash
# （首次）创建 Java 使用的库并导入表结构
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS drama_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p drama_system < backend-java/src/main/resources/sql/schema.sql

# （#058 等增量）若协作文档要求，再执行：
# mysql -u root -p drama_system < backend-java/src/main/resources/sql/fix-058-drama-user-recharge.sql
# mysql -u root -p drama_system < backend-java/src/main/resources/sql/fix-058-callback-config-strategy.sql
# （#060 策略说明脚本，无新列：可选）
# mysql -u root -p drama_system < backend-java/src/main/resources/sql/callback-strategy-upgrade.sql
# （#061 若报 Unknown column replenish_callback_enabled：补列，与 #058 二选一即可）
# mysql -u root -p drama_system < backend-java/src/main/resources/sql/fix-061-callback-missing-columns.sql
# （#062 同上，幂等版：缺列才 ALTER，适合与 Monica 流程对齐、可重复执行）
# mysql -u root -p drama_system < backend-java/src/main/resources/sql/fix-062-callback-config-columns.sql

# （可选）演示数据
# bash backend-java/scripts/import-demo-data.sh

# 迁移脚本依赖（在 scripts 目录）
cd backend-java/scripts && npm install && cd ../..

# 根目录一键启动
cd 未来管理系统
npm install
npm run start
```

访问 **http://localhost:5173**。默认账号：**admin** / **admin123**。

**分拆启动**：

```bash
npm run backend-java   # Java 3001（内部已 source use-java17.sh）
npm run frontend       # 前端 5173
```

Java 手动：**`cd backend-java && source scripts/use-java17.sh && export MYSQL_PASSWORD=... && mvn spring-boot:run`**。

## MySQL 与配置

- **`backend-java/src/main/resources/application.yml`**：数据源、JWT、`app.upload-dir`。可用环境变量 **`MYSQL_PASSWORD`**、**`JWT_SECRET`**、**`ALIYUN_ACCESS_KEY_ID`**、**`ALIYUN_ACCESS_KEY_SECRET`**、**`ALIYUN_VOD_REGION`**。

## 阿里云 VOD（可选）

1. 开通视频点播并配置 RAM 权限。  
2. 使用环境变量（或写入运行环境）：

```
ALIYUN_ACCESS_KEY_ID=...
ALIYUN_ACCESS_KEY_SECRET=...
ALIYUN_VOD_REGION=cn-shanghai
```

接口：**`GET /api/vod/upload-auth`**、**`GET /api/vod/refresh-upload-auth`**、**`GET /api/vod/play-auth/:videoId`**。

## 旧 storage.json

将原 **`storage.json`** 放到 **`backend-java/data/storage.json`**（或临时保留旧路径 **`…/backend/data/storage.json`** 若你本地仍有备份），再在 **`backend-java/scripts`** 下执行对应 **`node migrate-*.js`**。详见 **`backend-java/README.md`**。
