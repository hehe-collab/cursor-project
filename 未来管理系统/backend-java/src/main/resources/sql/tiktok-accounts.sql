-- TikTok OAuth 账户（指令 #096，最小 DDL）
-- 新建环境请优先执行 schema-tiktok-core.sql（#095-1，含 balance 与下游外键表定义）。
-- 执行：mysql -u root -p drama_system < src/main/resources/sql/tiktok-accounts.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `tiktok_accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `advertiser_name` VARCHAR(200) DEFAULT NULL COMMENT '广告主名称',
  `access_token` TEXT COMMENT 'Access Token',
  `refresh_token` TEXT COMMENT 'Refresh Token',
  `token_expires_at` DATETIME DEFAULT NULL COMMENT 'Access Token 过期时间',
  `currency` VARCHAR(10) NOT NULL DEFAULT 'USD',
  `timezone` VARCHAR(64) NOT NULL DEFAULT 'UTC',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active / inactive',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tiktok_accounts_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_accounts_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok OAuth 广告主账户';
