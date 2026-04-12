-- ============================================================
-- 系统配置扩展表结构（兼容 MySQL 5.7+）
-- 包含 categories / tags 的完整字段
-- ============================================================

SET NAMES utf8mb4;

-- ------------------ 分类表（扩展字段） ------------------
-- 添加 slug 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND COLUMN_NAME = 'slug');
SET @sql := IF(@exist = 0, 'ALTER TABLE categories ADD COLUMN slug VARCHAR(50) DEFAULT NULL COMMENT ''URL 友好标识'' AFTER name', 'SELECT ''slug already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 description 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND COLUMN_NAME = 'description');
SET @sql := IF(@exist = 0, 'ALTER TABLE categories ADD COLUMN description VARCHAR(200) DEFAULT NULL COMMENT ''分类描述'' AFTER slug', 'SELECT ''description already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 sort_order 字段（原始表是 sort，这里改成 sort_order）
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND COLUMN_NAME = 'sort_order');
SET @sql := IF(@exist = 0, 'ALTER TABLE categories ADD COLUMN sort_order INT DEFAULT 0 COMMENT ''排序权重'' AFTER description', 'SELECT ''sort_order already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 is_enabled 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND COLUMN_NAME = 'is_enabled');
SET @sql := IF(@exist = 0, 'ALTER TABLE categories ADD COLUMN is_enabled TINYINT(1) DEFAULT 1 COMMENT ''是否启用 1=启用 0=禁用'' AFTER sort_order', 'SELECT ''is_enabled already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 drama_count 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND COLUMN_NAME = 'drama_count');
SET @sql := IF(@exist = 0, 'ALTER TABLE categories ADD COLUMN drama_count INT DEFAULT 0 COMMENT ''关联短剧数'' AFTER is_enabled', 'SELECT ''drama_count already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加索引
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND INDEX_NAME = 'idx_slug');
SET @sql := IF(@exist = 0, 'ALTER TABLE categories ADD INDEX idx_slug (slug)', 'SELECT ''idx_slug already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------ 标签表（扩展字段） ------------------
-- 添加 color 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tags' AND COLUMN_NAME = 'color');
SET @sql := IF(@exist = 0, 'ALTER TABLE tags ADD COLUMN color VARCHAR(20) DEFAULT ''#409EFF'' COMMENT ''标签颜色（HEX）'' AFTER name', 'SELECT ''color already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 sort_order 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tags' AND COLUMN_NAME = 'sort_order');
SET @sql := IF(@exist = 0, 'ALTER TABLE tags ADD COLUMN sort_order INT DEFAULT 0 COMMENT ''排序权重'' AFTER color', 'SELECT ''sort_order already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 is_hot 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tags' AND COLUMN_NAME = 'is_hot');
SET @sql := IF(@exist = 0, 'ALTER TABLE tags ADD COLUMN is_hot TINYINT(1) DEFAULT 0 COMMENT ''是否热门 1=热门 0=普通'' AFTER sort_order', 'SELECT ''is_hot already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加 usage_count 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tags' AND COLUMN_NAME = 'usage_count');
SET @sql := IF(@exist = 0, 'ALTER TABLE tags ADD COLUMN usage_count INT DEFAULT 0 COMMENT ''使用次数'' AFTER is_hot', 'SELECT ''usage_count already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加索引
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tags' AND INDEX_NAME = 'idx_hot');
SET @sql := IF(@exist = 0, 'ALTER TABLE tags ADD INDEX idx_hot (is_hot)', 'SELECT ''idx_hot already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------ 插入默认分类 ------------------
INSERT INTO categories (name, slug, description, sort_order, is_enabled, drama_count)
VALUES
    ('都市言情', 'urban-romance', '现代都市背景的爱情故事', 1, 1, 0),
    ('古装穿越', 'costume-travel', '穿越到古代的奇幻剧情', 2, 1, 0),
    ('霸道总裁', 'domineering-ceo', '霸道总裁爱上我', 3, 1, 0),
    ('悬疑推理', 'mystery', '烧脑推理剧情', 4, 1, 0),
    ('甜宠日常', 'sweet-daily', '轻松甜蜜的日常故事', 5, 1, 0)
ON DUPLICATE KEY UPDATE
    slug = VALUES(slug),
    description = VALUES(description),
    sort_order = VALUES(sort_order);

-- ------------------ 插入默认标签 ------------------
INSERT INTO tags (name, color, sort_order, is_hot, usage_count)
VALUES
    ('爆款', '#F56C6C', 1, 1, 0),
    ('高甜', '#E6A23C', 2, 1, 0),
    ('反转', '#409EFF', 3, 1, 0),
    ('虐恋', '#909399', 4, 0, 0),
    ('搞笑', '#67C23A', 5, 0, 0),
    ('热血', '#F56C6C', 6, 0, 0)
ON DUPLICATE KEY UPDATE
    color = VALUES(color),
    sort_order = VALUES(sort_order);

SELECT 'schema-system-config upgrade done' AS status;
