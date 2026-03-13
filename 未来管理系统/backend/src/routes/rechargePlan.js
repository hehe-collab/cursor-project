const express = require('express');
const { authMiddleware } = require('../middleware/auth');
const { getRechargePlans, saveRechargePlan, deleteRechargePlan } = require('../storage');
const { parsePagination } = require('../utils/pagination');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  try {
    const { pageSize, offset } = parsePagination(req.query);
    const { name, payPlatform } = req.query;
    const filter = { name, payPlatform };
    const list = getRechargePlans(filter);
    const total = list.length;
    const rows = list.slice(offset, offset + pageSize);
    res.json({ code: 0, data: { list: rows, total } });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

router.post('/', async (req, res) => {
  try {
    const { name, bean_count, extra_bean, amount, recharge_info, pay_platform } = req.body;
    const id = saveRechargePlan({
      name: name || '',
      bean_count: bean_count || 0,
      extra_bean: extra_bean || 0,
      amount: amount || 0,
      recharge_info: recharge_info || '',
      pay_platform: pay_platform || '',
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
    const { name, bean_count, extra_bean, amount, recharge_info, pay_platform } = req.body;
    saveRechargePlan({
      id: parseInt(req.params.id),
      name,
      bean_count,
      extra_bean,
      amount,
      recharge_info,
      pay_platform,
    });
    res.json({ code: 0, message: '修改成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '修改失败' });
  }
});

router.delete('/:id', async (req, res) => {
  try {
    deleteRechargePlan(req.params.id);
    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '删除失败' });
  }
});

module.exports = router;
