const express = require('express');
const RPCClient = require('@alicloud/pop-core').RPCClient;
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();
router.use(authMiddleware);

// 获取上传凭证（用于前端直传阿里云 VOD）
router.get('/upload-auth', async (req, res) => {
  try {
    const { title, fileName } = req.query;
    if (!title || !fileName) {
      return res.status(400).json({ code: 400, message: '缺少 title 或 fileName' });
    }

    const accessKeyId = process.env.ALIYUN_ACCESS_KEY_ID;
    const accessKeySecret = process.env.ALIYUN_ACCESS_KEY_SECRET;
    const region = process.env.ALIYUN_VOD_REGION || 'cn-shanghai';

    if (!accessKeyId || !accessKeySecret) {
      return res.status(500).json({ code: 500, message: '请先配置阿里云 VOD 的 AccessKey' });
    }

    const client = new RPCClient({
      accessKeyId,
      accessKeySecret,
      endpoint: `https://vod.${region}.aliyuncs.com`,
      apiVersion: '2017-03-21',
    });

    const result = await client.request('CreateUploadVideo', {
      Title: title,
      FileName: fileName,
    });

    res.json({
      code: 0,
      data: {
        videoId: result.VideoId,
        uploadAuth: result.UploadAuth,
        uploadAddress: result.UploadAddress,
      },
    });
  } catch (err) {
    console.error('VOD 获取上传凭证失败:', err);
    res.status(500).json({
      code: 500,
      message: err.message || '获取上传凭证失败',
    });
  }
});

// 刷新上传凭证（大文件上传时凭证过期用）
router.get('/refresh-upload-auth', async (req, res) => {
  try {
    const { videoId } = req.query;
    if (!videoId) {
      return res.status(400).json({ code: 400, message: '缺少 videoId' });
    }

    const accessKeyId = process.env.ALIYUN_ACCESS_KEY_ID;
    const accessKeySecret = process.env.ALIYUN_ACCESS_KEY_SECRET;
    const region = process.env.ALIYUN_VOD_REGION || 'cn-shanghai';

    if (!accessKeyId || !accessKeySecret) {
      return res.status(500).json({ code: 500, message: '请先配置阿里云 VOD 的 AccessKey' });
    }

    const client = new RPCClient({
      accessKeyId,
      accessKeySecret,
      endpoint: `https://vod.${region}.aliyuncs.com`,
      apiVersion: '2017-03-21',
    });

    const result = await client.request('RefreshUploadVideo', {
      VideoId: videoId,
    });

    res.json({
      code: 0,
      data: {
        videoId: result.VideoId,
        uploadAuth: result.UploadAuth,
        uploadAddress: result.UploadAddress,
      },
    });
  } catch (err) {
    console.error('VOD 刷新凭证失败:', err);
    res.status(500).json({
      code: 500,
      message: err.message || '刷新凭证失败',
    });
  }
});

// 获取视频播放地址
router.get('/play-auth/:videoId', async (req, res) => {
  try {
    const { videoId } = req.params;

    const accessKeyId = process.env.ALIYUN_ACCESS_KEY_ID;
    const accessKeySecret = process.env.ALIYUN_ACCESS_KEY_SECRET;
    const region = process.env.ALIYUN_VOD_REGION || 'cn-shanghai';

    if (!accessKeyId || !accessKeySecret) {
      return res.status(500).json({ code: 500, message: '请先配置阿里云 VOD 的 AccessKey' });
    }

    const client = new RPCClient({
      accessKeyId,
      accessKeySecret,
      endpoint: `https://vod.${region}.aliyuncs.com`,
      apiVersion: '2017-03-21',
    });

    const result = await client.request('GetVideoPlayAuth', {
      VideoId: videoId,
    });

    res.json({
      code: 0,
      data: {
        playAuth: result.PlayAuth,
        videoMeta: result.VideoMeta,
      },
    });
  } catch (err) {
    console.error('VOD 获取播放凭证失败:', err);
    res.status(500).json({
      code: 500,
      message: err.message || '获取播放凭证失败',
    });
  }
});

module.exports = router;
