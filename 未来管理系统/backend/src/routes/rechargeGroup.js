const express = require('express');
const { authMiddleware } = require('../middleware/auth');
const { getRechargePlanGroups, saveRechargePlanGroup, deleteRechargePlanGroup, getRechargePlans } = require('../storage');
const { parsePagination } = require('../utils/pagination');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  try {
    const { pageSize, offset } = parsePagination(req.query);
    const { name } = req.query;
    let list = getRechargePlanGroups({ name });
    const total = list.length;
    list = list.slice(offset, offset + pageSize);
    const plans = getRechargePlans({});
    list = list.map(g => ({
      ...g,
      plans: (g.plan_ids || []).map(pid => plans.find(p => p.id === pid)?.name).filter(Boolean).join('、') || '-',
    }));
    res.json({ code: 0, data: { list, total } });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

router.post('/', async (req, res) => {
  try {
    const { name, plan_ids } = req.body;
    const id = saveRechargePlanGroup({ name: name || '', plan_ids: plan_ids || [] });
    res.json({ code: 0, data: { id }, message: '新增成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '新增失败' });
  }
});

router.put('/:id', async (req, res) => {
  try {
    const { name, plan_ids } = req.body;
    const all = getRechargePlanGroups({});
    const existing = all.find(r => r.id === parseInt(req.params.id)) || {};
    saveRechargePlanGroup({ ...existing, id: parseInt(req.params.id), name: name ?? existing.name, plan_ids: plan_ids ?? existing.plan_ids });
    res.json({ code: 0, message: '修改成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '修改失败' });
  }
});

router.delete('/:id', async (req, res) => {
  try {
    deleteRechargePlanGroup(req.params.id);
    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '删除失败' });
  }
});

module.exports = router;
