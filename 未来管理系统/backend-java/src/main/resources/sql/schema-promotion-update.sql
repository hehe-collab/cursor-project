-- 推广明细汇总表增量（#081 / #084）：platform、country、impressions + 索引
-- 可重复执行：缺列才 ADD，缺索引才 CREATE INDEX（使用当前库 DATABASE()）
--
-- mysql -u root -p drama_system < src/main/resources/sql/schema-promotion-update.sql

SET NAMES utf8mb4;

SET @db := DATABASE();

-- ---------- platform ----------
SET @exist := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'promotion_details_summary'
    AND COLUMN_NAME = 'platform');
SET @sqlstmt := IF(@exist = 0,
  'ALTER TABLE `promotion_details_summary` ADD COLUMN `platform` VARCHAR(50) DEFAULT NULL COMMENT ''投放媒体（tiktok/facebook/google）'' AFTER `promotion_name`',
  'SELECT ''skip: platform already exists'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- country ----------
SET @exist := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'promotion_details_summary'
    AND COLUMN_NAME = 'country');
SET @sqlstmt := IF(@exist = 0,
  'ALTER TABLE `promotion_details_summary` ADD COLUMN `country` VARCHAR(10) DEFAULT NULL COMMENT ''国家代码（ID/TH/US/VN/PH/MY）'' AFTER `platform`',
  'SELECT ''skip: country already exists'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- impressions ----------
SET @exist := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'promotion_details_summary'
    AND COLUMN_NAME = 'impressions');
SET @sqlstmt := IF(@exist = 0,
  'ALTER TABLE `promotion_details_summary` ADD COLUMN `impressions` BIGINT NOT NULL DEFAULT 0 COMMENT ''日曝光（冗余，便于聚合CPM）'' AFTER `repeat_recharge_count`',
  'SELECT ''skip: impressions already exists'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 索引（与 schema-promotion.sql 命名一致）----------
SET @exist := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'promotion_details_summary'
    AND INDEX_NAME = 'idx_promo_summary_platform');
SET @sqlstmt := IF(@exist = 0,
  'ALTER TABLE `promotion_details_summary` ADD INDEX `idx_promo_summary_platform` (`platform`)',
  'SELECT ''skip: idx_promo_summary_platform exists'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'promotion_details_summary'
    AND INDEX_NAME = 'idx_promo_summary_country');
SET @sqlstmt := IF(@exist = 0,
  'ALTER TABLE `promotion_details_summary` ADD INDEX `idx_promo_summary_country` (`country`)',
  'SELECT ''skip: idx_promo_summary_country exists'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'schema-promotion-update done' AS ok;
