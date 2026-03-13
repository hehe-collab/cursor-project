require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { initDatabase } = require('./db');

const authRoutes = require('./routes/auth');
const dashboardRoutes = require('./routes/dashboard');
const statsRoutes = require('./routes/stats');
const dramaRoutes = require('./routes/drama');
const categoryRoutes = require('./routes/category');
const tagRoutes = require('./routes/tag');
const userRoutes = require('./routes/user');
const vodRoutes = require('./routes/vod');
const settingsRoutes = require('./routes/settings');
const rechargeRoutes = require('./routes/recharge');
const promotionRoutes = require('./routes/promotion');
const rechargePlanRoutes = require('./routes/rechargePlan');
const rechargeGroupRoutes = require('./routes/rechargeGroup');
const accountRoutes = require('./routes/account');
const callbackRoutes = require('./routes/callback');
const adMaterialRoutes = require('./routes/adMaterial');
const titlePackRoutes = require('./routes/titlePack');
const adTaskRoutes = require('./routes/adTask');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 路由
app.use('/api/auth', authRoutes);
app.use('/api/dashboard', dashboardRoutes);
app.use('/api/stats', statsRoutes);
app.use('/api/dramas', dramaRoutes);
app.use('/api/categories', categoryRoutes);
app.use('/api/tags', tagRoutes);
app.use('/api/users', userRoutes);
app.use('/api/vod', vodRoutes);
app.use('/api/settings', settingsRoutes);
app.use('/api/recharge', rechargeRoutes);
app.use('/api/delivery-links', promotionRoutes);  /* DramaBagus 投放链接 */
app.use('/api/recharge-plans', rechargePlanRoutes); /* DramaBagus 充值方案 */
app.use('/api/recharge-groups', rechargeGroupRoutes); /* DramaBagus 充值方案组 */
app.use('/api/accounts', accountRoutes);  /* DramaBagus 账户管理 */
app.use('/api/callback', callbackRoutes);
app.use('/api/ad-material', adMaterialRoutes);
app.use('/api/title-pack', titlePackRoutes);
app.use('/api/ad-task', adTaskRoutes);

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: '未来管理系统 API 运行中' });
});

// 404 - 未匹配的 API
app.use((req, res) => {
  res.status(404).json({ code: 404, message: '接口不存在' });
});

const { initDemoData } = require('./storage');

// 初始化数据库并启动
initDatabase()
  .then(() => {
    try { initDemoData(); } catch (e) {}
    app.listen(PORT, () => {
      console.log(`🚀 服务器运行在 http://localhost:${PORT}`);
    });
  })
  .catch((err) => {
    console.error('数据库初始化失败:', err);
    process.exit(1);
  });
