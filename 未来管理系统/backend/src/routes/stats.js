const express = require('express');
const { authMiddleware } = require('../middleware/auth');
const { getRechargeRecords } = require('../storage');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  try {
    const { dramaId, media, page = 1, pageSize = 10 } = req.query;
    const list = getRechargeRecords({});
    const byDate = {};
    list.forEach(r => {
      const date = (r.created_at || '').slice(0, 10);
      if (!date) return;
      if (dramaId && String(r.drama_id) !== String(dramaId)) return;
      if (media && (r.platform || '').toLowerCase() !== (media || '').toLowerCase()) return;
      if (!byDate[date]) byDate[date] = { time: date, orderCount: 0, rechargeAmount: 0, consume: 0, roi: '-' };
      byDate[date].orderCount += 1;
      byDate[date].rechargeAmount += r.amount || 0;
    });
    const rows = Object.values(byDate).sort((a, b) => b.time.localeCompare(a.time));
    rows.forEach(r => {
      r.consume = Math.round(r.rechargeAmount * 0.6);
      r.roi = r.consume ? (r.rechargeAmount / r.consume).toFixed(2) : '-';
    });
    const total = rows.length;
    const p = parseInt(page, 10) || 1;
    const ps = parseInt(pageSize, 10) || 10;
    const offset = (p - 1) * ps;
    const listPage = rows.slice(offset, offset + ps);
    res.json({ code: 0, data: { list: listPage, total } });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

module.exports = router;
