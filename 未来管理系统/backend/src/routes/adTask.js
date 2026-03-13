const express = require('express');
const { authMiddleware } = require('../middleware/auth');
const { getAdTasks, saveAdTask, deleteAdTask } = require('../storage');

const router = express.Router();
router.use(authMiddleware);

router.get('/', (req, res) => {
  try {
    const { page = 1, pageSize = 10 } = req.query;
    let list = getAdTasks();
    const total = list.length;
    const offset = (page - 1) * pageSize;
    list = list.slice(offset, offset + parseInt(pageSize));
    res.json({ code: 0, data: { list, total } });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取失败' });
  }
});

router.post('/', (req, res) => {
  try {
    const id = saveAdTask({ ...req.body, created_by: req.user?.id || 0 });
    res.json({ code: 0, data: { id }, message: '新增成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '新增失败' });
  }
});

router.put('/:id', (req, res) => {
  try {
    const all = getAdTasks();
    const existing = all.find(t => t.id === parseInt(req.params.id));
    if (!existing) return res.status(404).json({ code: 404, message: '任务不存在' });
    saveAdTask({ ...existing, ...req.body, id: existing.id });
    res.json({ code: 0, message: '修改成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '修改失败' });
  }
});

router.delete('/:id', (req, res) => {
  try {
    deleteAdTask(req.params.id);
    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '删除失败' });
  }
});

module.exports = router;
