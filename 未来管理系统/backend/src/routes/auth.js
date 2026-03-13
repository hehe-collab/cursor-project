const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { pool } = require('../db');

const router = express.Router();

router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ code: 400, message: '请输入用户名和密码' });
    }
    const [rows] = await pool.query('SELECT * FROM admins WHERE username = ?', [username]);
    if (!rows.length) {
      return res.status(401).json({ code: 401, message: '用户名或密码错误' });
    }
    const admin = rows[0];
    const valid = await bcrypt.compare(password, admin.password);
    if (!valid) {
      return res.status(401).json({ code: 401, message: '用户名或密码错误' });
    }
    const token = jwt.sign(
      { userId: admin.id },
      process.env.JWT_SECRET || 'future_admin_secret',
      { expiresIn: '7d' }
    );
    res.json({
      code: 0,
      data: {
        token,
        user: {
          id: admin.id,
          username: admin.username,
          nickname: admin.nickname,
          role: admin.role,
        },
      },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '登录失败' });
  }
});

module.exports = router;
