const express = require('express');
const { pool } = require('../db');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT key_name, value FROM settings');
    const data = {};
    rows.forEach((r) => (data[r.key_name] = r.value));
    res.json({ code: 0, data });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

router.post('/', async (req, res) => {
  try {
    const items = req.body;
    for (const [key, value] of Object.entries(items)) {
      await pool.query(
        'INSERT INTO settings (key_name, value) VALUES (?, ?) ON DUPLICATE KEY UPDATE value=?',
        [key, String(value), String(value)]
      );
    }
    res.json({ code: 0, message: '保存成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '保存失败' });
  }
});

module.exports = router;
