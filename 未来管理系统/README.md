# 未来管理系统

影视/短剧管理后台，类似 DramaBagus 的完整管理系统。支持剧集管理、分类标签、用户管理、阿里云视频点播（VOD）等。

**开箱即用**：默认使用 JSON 文件存储，无需安装 MySQL。配置 `DB_PASSWORD` 后自动切换 MySQL。

## 技术栈

- **前端**：Vue 3 + Element Plus + Vue Router + Axios
- **后端**：Node.js + Express + MySQL
- **视频**：阿里云视频点播 VOD

## 项目结构

```
未来管理系统/
├── backend/          # 后端服务
│   ├── src/
│   │   ├── index.js      # 入口
│   │   ├── db.js         # 数据库初始化
│   │   ├── middleware/   # 中间件
│   │   └── routes/       # API 路由
│   ├── .env             # 环境变量（需配置）
│   └── package.json
├── frontend/         # 前端
│   ├── src/
│   │   ├── views/       # 页面
│   │   ├── api/         # 请求封装
│   │   └── router/      # 路由
│   └── package.json
└── README.md
```

## 一、安装 MySQL

### macOS（Homebrew）

```bash
brew install mysql
brew services start mysql
```

### 创建数据库和用户

```bash
mysql -u root -p
```

在 MySQL 中执行：

```sql
CREATE DATABASE future_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 如需单独用户：
-- CREATE USER 'future'@'localhost' IDENTIFIED BY 'your_password';
-- GRANT ALL ON future_admin.* TO 'future'@'localhost';
-- FLUSH PRIVILEGES;
```

## 二、后端配置与启动

```bash
cd backend
cp .env.example .env
# 编辑 .env，填写 MySQL 密码等
npm install
npm run dev
```

**`.env` 必填项**：

- `DB_PASSWORD`：MySQL root 密码
- `JWT_SECRET`：建议改为随机字符串

后端默认运行在 `http://localhost:3000`。首次启动会自动建表并创建默认管理员。

## 三、前端启动

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，开发时自动代理 `/api` 到后端。

## 四、默认账号

- **用户名**：`admin`
- **密码**：`admin123`

首次登录后请立即修改密码。

## 五、阿里云 VOD 配置

### 1. 开通视频点播

1. 登录 [阿里云控制台](https://www.aliyun.com/)
2. 开通「视频点播」服务
3. 创建 RAM 用户，获取 AccessKey ID 和 AccessKey Secret
4. 为 RAM 用户授予 `AliyunVODFullAccess` 权限

### 2. 配置到项目

在 `backend/.env` 中填写：

```
ALIYUN_ACCESS_KEY_ID=你的AccessKeyId
ALIYUN_ACCESS_KEY_SECRET=你的AccessKeySecret
ALIYUN_VOD_REGION=cn-shanghai
```

### 3. 上传视频

**方式一：阿里云控制台**

1. 进入 [视频点播控制台](https://vod.console.aliyun.com/)
2. 上传视频 → 上传完成后获取 **VideoId**
3. 在「剧集管理」→「编辑剧集」→「剧集列表」中，将 VideoId 填入对应集数

**方式二：后端 API 获取上传凭证**

前端可调用 `GET /api/vod/upload-auth?title=xxx&fileName=xxx.mp4` 获取上传凭证，再配合阿里云 [JavaScript 上传 SDK](https://help.aliyun.com/zh/vod/developer-reference/upload-sdk-for-javascript) 实现直传。

## 六、功能模块

| 模块 | 说明 |
|------|------|
| 仪表盘 | 剧集数、用户数、播放量等统计 |
| 剧集管理 | 剧集列表、新增、编辑、删除、搜索、分页 |
| 分类管理 | 分类增删改查 |
| 标签管理 | 标签增删改查 |
| 用户管理 | 前台用户列表 |
| 系统设置 | 站点名称、Logo、备案号等 |

## 七、生产部署

### 后端

```bash
cd backend
npm install --production
npm start
```

建议使用 PM2：

```bash
npm install -g pm2
pm2 start src/index.js --name future-admin
```

### 前端

```bash
cd frontend
npm run build
```

将 `dist/` 目录部署到 Nginx 或其他静态服务器。

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/frontend/dist;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }
    location /api {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 八、常见问题

**Q: 后端启动报错 "Cannot connect to MySQL"**

- 检查 MySQL 是否已启动
- 检查 `.env` 中 `DB_HOST`、`DB_USER`、`DB_PASSWORD` 是否正确

**Q: 视频无法播放**

- 确认已配置阿里云 VOD 的 AccessKey
- 确认剧集集数中填写了正确的 `video_id`（阿里云 VOD 的 VideoId）

**Q: 登录后 401**

- 检查 JWT_SECRET 是否与部署环境一致
- 清除浏览器 localStorage 后重新登录

---

如有问题，请检查后端控制台日志和浏览器 Network 面板。
