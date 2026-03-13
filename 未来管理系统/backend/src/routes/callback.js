const express = require('express');
const { authMiddleware } = require('../middleware/auth');
const { getCallbackConfig, saveCallbackConfig, getCallbackLogs } = require('../storage');

const router = express.Router();
router.use(authMiddleware);

router.get('/config', (req, res) => {
  try {
    const config = getCallbackConfig();
    res.json({ code: 0, data: config });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

router.post('/config', (req, res) => {
  try {
    saveCallbackConfig(req.body);
    res.json({ code: 0, message: '保存成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '保存失败' });
  }
});

router.get('/logs', (req, res) => {
  try {
    const { list, total, stats } = getCallbackLogs(req.query);
    res.json({ code: 0, data: { list, total, stats: stats || { total: 0, success: 0, failure: 0, pending: 0 } } });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

module.exports = router;
