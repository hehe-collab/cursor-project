-- 广告账户表增量（#084）：若早期库缺少 media / country，则补列与索引
-- 本仓库 schema.sql 通常已包含；本脚本可重复执行（仅缺列时 ALTER）
-- mysql -u root -p drama_system < src/main/resources/sql/schema-accounts-update.sql

SET NAMES utf8mb4;

SET @db := DATABASE();

-- media（投放媒体；与产品所称 platform 对应）
SET @exist := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ad_accounts' AND COLUMN_NAME = 'media');
SET @sqlstmt := IF(@exist = 0,
  'ALTER TABLE `ad_accounts` ADD COLUMN `media` VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''投放媒体'' AFTER `id`',
  'SELECT ''skip: ad_accounts.media exists'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ad_accounts' AND COLUMN_NAME = 'country');
SET @sqlstmt := IF(@exist = 0,
  'ALTER TABLE `ad_accounts` ADD COLUMN `country` VARCHAR(16) NOT NULL DEFAULT '''' COMMENT ''国家代码'' AFTER `media`',
  'SELECT ''skip: ad_accounts.country exists'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ad_accounts' AND INDEX_NAME = 'idx_aa_media');
SET @sqlstmt := IF(@exist = 0,
  'ALTER TABLE `ad_accounts` ADD INDEX `idx_aa_media` (`media`)',
  'SELECT ''skip: idx_aa_media exists'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ad_accounts' AND INDEX_NAME = 'idx_aa_country');
SET @sqlstmt := IF(@exist = 0,
  'ALTER TABLE `ad_accounts` ADD INDEX `idx_aa_country` (`country`)',
  'SELECT ''skip: idx_aa_country exists'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'schema-accounts-update done' AS ok;
