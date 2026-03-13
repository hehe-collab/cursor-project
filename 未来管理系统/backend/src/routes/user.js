const express = require('express');
const { pool } = require('../db');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  try {
    const { page = 1, pageSize = 10, userId, username, promoteId, token, registerStart, registerEnd } = req.query;
    const offset = (page - 1) * pageSize;
    let where = 'WHERE 1=1';
    const params = [];
    if (userId) {
      where += ' AND id = ?';
      params.push(parseInt(userId) || userId);
    }
    if (username) {
      where += ' AND username LIKE ?';
      params.push(`%${username}%`);
    }
    if (promoteId) {
      where += ' AND (promote_id = ? OR promote_id LIKE ?)';
      params.push(promoteId, `%${promoteId}%`);
    }
    if (token) {
      where += ' AND (token LIKE ? OR token = ?)';
      params.push(`%${token}%`, token);
    }
    if (registerStart) {
      where += ' AND DATE(created_at) >= ?';
      params.push(registerStart);
    }
    if (registerEnd) {
      where += ' AND DATE(created_at) <= ?';
      params.push(registerEnd);
    }
    const [[{ total }]] = await pool.query(`SELECT COUNT(*) as total FROM users ${where}`, params);
    const [rows] = await pool.query(
      `SELECT * FROM users ${where} ORDER BY id DESC LIMIT ? OFFSET ?`,
      [...params, parseInt(pageSize), offset]
    );
    res.json({
      code: 0,
      data: { list: rows, total, page: parseInt(page), pageSize: parseInt(pageSize) },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

module.exports = router;
