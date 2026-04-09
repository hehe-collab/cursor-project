-- 指令 #096：TikTok OAuth 与多广告主（Marketing API v1.3）
-- 执行：mysql -u root -p drama_system < src/main/resources/sql/fix-096-tiktok-accounts.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `tiktok_accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `advertiser_name` VARCHAR(200) DEFAULT NULL COMMENT '广告主名称',
  `access_token` TEXT COMMENT 'Access Token',
  `refresh_token` TEXT COMMENT 'Refresh Token',
  `token_expires_at` DATETIME DEFAULT NULL COMMENT 'Access Token 过期时间',
  `currency` VARCHAR(16) DEFAULT 'USD' COMMENT '货币',
  `timezone` VARCHAR(64) DEFAULT 'UTC' COMMENT '时区',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active / inactive',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tiktok_accounts_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_accounts_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok 广告主与 OAuth 凭据';
