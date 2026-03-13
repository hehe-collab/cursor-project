const express = require('express');
const { authMiddleware } = require('../middleware/auth');
const { getAdAccounts, saveAdAccount, deleteAdAccount } = require('../storage');
const { parsePagination } = require('../utils/pagination');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  try {
    const { pageSize, offset } = parsePagination(req.query);
    const { media, country, subject, accountId, accountName, keyword } = req.query;
    const filter = { media, country, subject, accountId, accountName, keyword };
    const list = getAdAccounts(filter);
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
    const { media, country, subject_name, account_id, account_name } = req.body;
    const id = saveAdAccount({
      media: media || '',
      country: country || '',
      subject_name: subject_name || '',
      account_id: account_id || '',
      account_name: account_name || '',
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
    const { media, country, subject_name, account_id, account_name } = req.body;
    saveAdAccount({
      id: parseInt(req.params.id),
      media,
      country,
      subject_name,
      account_id,
      account_name,
    });
    res.json({ code: 0, message: '修改成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '修改失败' });
  }
});

router.delete('/:id', async (req, res) => {
  try {
    deleteAdAccount(req.params.id);
    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '删除失败' });
  }
});

module.exports = router;
