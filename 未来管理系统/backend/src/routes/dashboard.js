const express = require('express');
const { authMiddleware } = require('../middleware/auth');
const { getRechargeRecords, getPromotionLinks } = require('../storage');

const router = express.Router();
router.use(authMiddleware);

const EXCHANGE_RATE = 16779; // 1 USD = 16779 IDR (BCA)

router.get('/stats', async (req, res) => {
  try {
    const { promoteId, promoteName, dramaId, dramaName, account, media, country, dateStart, dateEnd } = req.query;
    const today = new Date().toISOString().slice(0, 10);
    const start = dateStart || today;
    const end = dateEnd || today;

    const rechargeList = getRechargeRecords({});
    const promotions = getPromotionLinks({});
    let filtered = rechargeList.filter(r => {
      const d = (r.created_at || '').slice(0, 10);
      if (d < start || d > end) return false;
      if (promoteId && (r.promote_id || r.promote_link_id || '').indexOf(promoteId) < 0) return false;
      if (promoteName && (r.promote_name || '').indexOf(promoteName) < 0) return false;
      if (dramaId && String(r.drama_id) !== String(dramaId)) return false;
      if (dramaName && (r.drama_name || '').indexOf(dramaName) < 0) return false;
      if (account && (r.account_id || '').indexOf(account) < 0) return false;
      if (media && (r.platform || '').toLowerCase() !== (media || '').toLowerCase()) return false;
      if (country && (r.country || '').toUpperCase() !== (country || '').toUpperCase()) return false;
      return true;
    });

    const spend = filtered.reduce((s, r) => s + (r.consume ?? r.amount * 0.6 ?? 0), 0);
    const recharge = filtered.reduce((s, r) => s + (r.amount || 0), 0);
    const roi = spend > 0 ? recharge / spend : 0;
    const profit = recharge - spend;
    const hourlyRate = spend; // 简化：时速≈消耗

    // 今日概览（DramaBagus 风格）
    const todayFiltered = rechargeList.filter(r => (r.created_at || '').slice(0, 10) === today);
    const todayUsers = new Set(todayFiltered.map(r => r.user_id || r.order_no || r.id)).size;
    const todayAmount = todayFiltered.reduce((s, r) => s + (r.amount || 0), 0);
    const todayOrders = todayFiltered.length;
    const unsettled = 0; // 未结算，占位

    const tableData = [];
    const byPromote = {};
    filtered.forEach(r => {
      const pid = r.promote_id || r.promote_link_id || '-';
      if (!byPromote[pid]) {
        byPromote[pid] = { promote_id: pid, promote_name: (r.promote_name || r.promote_id || '-'), date: (r.created_at || '').slice(0, 10), account: (r.account_id || '').slice(-6) || '-', spend: 0, recharge: 0, users: 0, orders: 0 };
      }
      byPromote[pid].spend += r.consume ?? r.amount * 0.6 ?? 0;
      byPromote[pid].recharge += r.amount || 0;
      byPromote[pid].orders += 1;
      byPromote[pid].users = byPromote[pid].orders;
    });
    tableData.push({
      is_summary: true,
      promote_id: '汇总',
      promote_name: '',
      date: '',
      account: '',
      spend,
      hourly_rate: hourlyRate,
      roi,
      users: filtered.length,
      recharge,
      profit,
      orders: filtered.length,
    });
    tableData.push({
      is_summary: true,
      promote_id: '',
      promote_name: '汇总',
      date: '',
      account: '',
      spend: 0,
      hourly_rate: 0,
      roi: 0,
      users: 0,
      recharge: 0,
      profit: 0,
      orders: 0,
    });
    Object.values(byPromote).forEach(r => {
      tableData.push({
        is_summary: false,
        promote_id: r.promote_id,
        promote_name: r.promote_name,
        date: r.date,
        account: r.account,
        spend: r.spend,
        hourly_rate: r.spend,
        roi: r.spend > 0 ? r.recharge / r.spend : 0,
        users: r.users,
        recharge: r.recharge,
        profit: r.recharge - r.spend,
        orders: r.orders,
      });
    });

    res.json({
      code: 0,
      data: {
        spend: Math.round(spend * 100) / 100,
        hourlyRate: Math.round(hourlyRate * 100) / 100,
        recharge: Math.round(recharge * 100) / 100,
        roi: Math.round(roi * 10000) / 10000,
        profit: Math.round(profit * 100) / 100,
        exchangeRate: EXCHANGE_RATE,
        exchangeTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
        todayUsers,
        todayAmount: Math.round(todayAmount * 100) / 100,
        todayOrders,
        unsettled,
        tableData,
      },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取统计失败' });
  }
});

module.exports = router;
