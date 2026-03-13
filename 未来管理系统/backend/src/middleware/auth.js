const jwt = require('jsonwebtoken');
const { pool } = require('../db');

async function authMiddleware(req, res, next) {
  const token = req.headers.authorization?.replace('Bearer ', '');
  if (!token) {
    return res.status(401).json({ code: 401, message: '请先登录' });
  }
  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'future_admin_secret');
    const [rows] = await pool.query('SELECT id, username, nickname, role FROM admins WHERE id = ?', [decoded.userId]);
    if (!rows.length) {
      return res.status(401).json({ code: 401, message: '用户不存在' });
    }
    req.user = rows[0];
    next();
  } catch (err) {
    return res.status(401).json({ code: 401, message: '登录已过期，请重新登录' });
  }
}

module.exports = { authMiddleware };
