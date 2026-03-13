const express = require('express');
const { authMiddleware } = require('../middleware/auth');
const { getPromotionLinks, savePromotionLink, deletePromotionLink, getRechargeRecords } = require('../storage');
const { parsePagination } = require('../utils/pagination');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  try {
    const { pageSize, offset } = parsePagination(req.query);
    const { promoteId, media, country, promoteName, dramaId, domain } = req.query;
    const filter = { promoteId, media, country, promoteName, dramaId, domain };
    const list = getPromotionLinks(filter);
    const total = list.length;
    let rows = list.slice(offset, offset + pageSize);

    // 统计：按推广ID聚合充值记录
    const rechargeList = getRechargeRecords({});
    const statByPromote = {};
    rechargeList.forEach(r => {
      const pid = r.promote_id || r.promote_link_id || '-';
      if (!statByPromote[pid]) statByPromote[pid] = { orders: 0, amount: 0, spend: 0 };
      statByPromote[pid].orders += 1;
      statByPromote[pid].amount += r.amount || 0;
      statByPromote[pid].spend += r.consume ?? r.amount * 0.6 ?? 0;
    });
    rows = rows.map(r => {
      const pid = r.promote_id || '-';
      const s = statByPromote[pid] || { orders: 0, amount: 0, spend: 0 };
      return {
        ...r,
        stat: `${s.orders}单/${s.amount.toFixed(0)}`,
        amount: Math.round(s.amount * 100) / 100,
        spend: Math.round(s.spend * 100) / 100,
        target: r.drama_name || `剧${r.drama_id || '-'}`,
        status: r.status || 'active',
      };
    });

    res.json({ code: 0, data: { list: rows, total } });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

router.post('/', async (req, res) => {
  try {
    const { promote_id, platform, country, promote_name, drama_id, plan_group_id, bean_count, free_episodes, domain } = req.body;
    const id = savePromotionLink({
      promote_id: promote_id || '',
      platform: platform || '',
      country: country || '',
      promote_name: promote_name || '',
      drama_id: drama_id || null,
      plan_group_id: plan_group_id || null,
      bean_count: bean_count || 0,
      free_episodes: free_episodes || 0,
      domain: domain || '',
      status: 'active',
      created_by: req.user?.id || 0,
    });
    res.json({ code: 0, data: { id }, message: '新增成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '新增失败' });
  }
});

router.put('/:id', async (req, res) => {
  try {
    const all = getPromotionLinks({});
    const existing = all.find(r => r.id === parseInt(req.params.id)) || {};
    savePromotionLink({
      ...existing,
      ...req.body,
      id: parseInt(req.params.id),
    });
    res.json({ code: 0, message: '修改成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '修改失败' });
  }
});

router.delete('/:id', async (req, res) => {
  try {
    deletePromotionLink(req.params.id);
    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '删除失败' });
  }
});

module.exports = router;
