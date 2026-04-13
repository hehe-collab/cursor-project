-- 投放链接表新增 UTM 参数字段（每列单独 ADD，兼容各版本 MySQL）
ALTER TABLE promotion_links
  ADD COLUMN utm_source VARCHAR(50) NOT NULL DEFAULT 'tiktok' COMMENT '流量来源（tiktok/meta/google/facebook）',
  ADD COLUMN utm_medium VARCHAR(50) NOT NULL DEFAULT 'paid' COMMENT '媒介类型（paid/organic）',
  ADD COLUMN use_tiktok_macros TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否附加 TikTok 宏变量（1=是，0=否）';
