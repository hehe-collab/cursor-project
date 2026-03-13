const express = require('express');
const { pool, getEpisodesList, getDramasWithEpisodeCount } = require('../db');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();
router.use(authMiddleware);

// 剧集信息列表（扁平化，一次请求，解决 N+1）
router.get('/episodes', async (req, res) => {
  try {
    const { page = 1, pageSize = 10, dramaId, episodeNum, externalId } = req.query;
    const result = getEpisodesList
      ? await Promise.resolve(getEpisodesList({ page, pageSize, dramaId, episodeNum, externalId }))
      : { list: [], total: 0 };
    res.json({ code: 0, data: result });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取剧集列表失败' });
  }
});

// 短剧管理列表（含 episode_count、task_status、enabled）
router.get('/manage-list', async (req, res) => {
  try {
    const { page = 1, pageSize = 10, dramaId, title, categoryId, status } = req.query;
    const fn = getDramasWithEpisodeCount || (async () => ({ list: [], total: 0 }));
    const result = await Promise.resolve(fn({ page, pageSize, dramaId, title, categoryId, status }));
    res.json({ code: 0, data: result });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取短剧管理列表失败' });
  }
});


// 剧集列表
router.get('/', async (req, res) => {
  try {
    const { page = 1, pageSize = 10, keyword, categoryId, status } = req.query;
    const offset = (page - 1) * pageSize;
    let where = 'WHERE 1=1';
    const params = [];

    if (keyword) {
      where += ' AND d.title LIKE ?';
      params.push(`%${keyword}%`);
    }
    if (categoryId) {
      where += ' AND d.category_id = ?';
      params.push(categoryId);
    }
    if (status) {
      where += ' AND d.status = ?';
      params.push(status);
    }

    const [[{ total }]] = await pool.query(
      `SELECT COUNT(*) as total FROM dramas d ${where}`,
      params
    );

    const [rows] = await pool.query(
      `SELECT d.*, c.name as category_name FROM dramas d 
       LEFT JOIN categories c ON d.category_id = c.id 
       ${where} ORDER BY d.sort DESC, d.id DESC LIMIT ? OFFSET ?`,
      [...params, parseInt(pageSize), offset]
    );

    res.json({
      code: 0,
      data: {
        list: rows,
        total,
        page: parseInt(page),
        pageSize: parseInt(pageSize),
      },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取列表失败' });
  }
});

// 剧集详情（含集数）
router.get('/:id', async (req, res) => {
  try {
    const [dramas] = await pool.query(
      `SELECT d.*, c.name as category_name FROM dramas d 
       LEFT JOIN categories c ON d.category_id = c.id WHERE d.id = ?`,
      [req.params.id]
    );
    if (!dramas.length) {
      return res.status(404).json({ code: 404, message: '剧集不存在' });
    }
    const drama = dramas[0];

    const [episodes] = await pool.query(
      'SELECT * FROM drama_episodes WHERE drama_id = ? ORDER BY episode_num',
      [req.params.id]
    );
    const [tagIds] = await pool.query('SELECT tag_id FROM drama_tags WHERE drama_id = ?', [req.params.id]);
    drama.episodes = episodes;
    drama.tag_ids = tagIds.map((t) => t.tag_id);

    res.json({ code: 0, data: drama });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '获取详情失败' });
  }
});

// 新增剧集
router.post('/', async (req, res) => {
  try {
    const { title, cover, description, category_id, status, sort, tag_ids, episodes } = req.body;
    if (!title) {
      return res.status(400).json({ code: 400, message: '请输入剧集标题' });
    }

    const [result] = await pool.query(
      'INSERT INTO dramas (title, cover, description, category_id, status, sort) VALUES (?, ?, ?, ?, ?, ?)',
      [title, cover || '', description || '', category_id || null, status || 'draft', sort || 0]
    );
    const dramaId = result.insertId;

    if (tag_ids && tag_ids.length) {
      for (const tagId of tag_ids) {
        await pool.query('INSERT INTO drama_tags (drama_id, tag_id) VALUES (?, ?)', [dramaId, tagId]);
      }
    }

    if (episodes && episodes.length) {
      for (const ep of episodes) {
        await pool.query(
          'INSERT INTO drama_episodes (drama_id, episode_num, title, video_id, video_url, duration) VALUES (?, ?, ?, ?, ?, ?)',
          [dramaId, ep.episode_num, ep.title || '', ep.video_id || '', ep.video_url || '', ep.duration || 0]
        );
      }
    }

    res.json({ code: 0, data: { id: dramaId }, message: '创建成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '创建失败' });
  }
});

// 更新剧集
router.put('/:id', async (req, res) => {
  try {
    const { title, cover, description, category_id, status, sort, tag_ids, episodes } = req.body;
    const id = req.params.id;

    await pool.query(
      'UPDATE dramas SET title=?, cover=?, description=?, category_id=?, status=?, sort=?, updated_at=NOW() WHERE id=?',
      [title, cover, description, category_id, status, sort, id]
    );

    await pool.query('DELETE FROM drama_tags WHERE drama_id = ?', [id]);
    if (tag_ids && tag_ids.length) {
      for (const tagId of tag_ids) {
        await pool.query('INSERT INTO drama_tags (drama_id, tag_id) VALUES (?, ?)', [id, tagId]);
      }
    }

    await pool.query('DELETE FROM drama_episodes WHERE drama_id = ?', [id]);
    if (episodes && episodes.length) {
      for (const ep of episodes) {
        await pool.query(
          'INSERT INTO drama_episodes (drama_id, episode_num, title, video_id, video_url, duration) VALUES (?, ?, ?, ?, ?, ?)',
          [id, ep.episode_num, ep.title || '', ep.video_id || '', ep.video_url || '', ep.duration || 0]
        );
      }
    }

    res.json({ code: 0, message: '更新成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '更新失败' });
  }
});

// 删除剧集
router.delete('/:id', async (req, res) => {
  try {
    await pool.query('DELETE FROM dramas WHERE id = ?', [req.params.id]);
    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ code: 500, message: '删除失败' });
  }
});

module.exports = router;
