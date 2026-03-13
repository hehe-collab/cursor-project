const express = require('express');
const { authMiddleware } = require('../middleware/auth');
const { getRechargeRecords } = require('../storage');
const { parsePagination } = require('../utils/pagination');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  try {
    const { page, pageSize, offset } = parsePagination(req.query);
    const { userId, currency, merchantId, orderNo, externalOrderNo, promoteId, payResult, country, registerStart, registerEnd } = req.query;
    const filter = { userId, currency, merchantId, orderNo, externalOrderNo, promoteId, payResult, country, registerStart, registerEnd };
    const list = getRechargeRecords(filter);
    const total = list.length;
    const rows = list.slice(offset, offset + pageSize);
    const totalAmount = list.reduce((s, r) => s + (r.amount || 0), 0);
    res.json({ code: 0, data: { list: rows, total, totalAmount } });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

module.exports = router;
