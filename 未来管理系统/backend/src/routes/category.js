const express = require('express');
const { pool } = require('../db');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT * FROM categories ORDER BY sort ASC, id ASC');
    res.json({ code: 0, data: rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

router.post('/', async (req, res) => {
  try {
    const { name, sort } = req.body;
    if (!name) return res.status(400).json({ code: 400, message: '请输入分类名称' });
    const [r] = await pool.query('INSERT INTO categories (name, sort) VALUES (?, ?)', [name, sort || 0]);
    res.json({ code: 0, data: { id: r.insertId }, message: '创建成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '创建失败' });
  }
});

router.put('/:id', async (req, res) => {
  try {
    const { name, sort } = req.body;
    await pool.query('UPDATE categories SET name=?, sort=? WHERE id=?', [name, sort, req.params.id]);
    res.json({ code: 0, message: '更新成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '更新失败' });
  }
});

router.delete('/:id', async (req, res) => {
  try {
    await pool.query('DELETE FROM categories WHERE id = ?', [req.params.id]);
    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '删除失败' });
  }
});

module.exports = router;
