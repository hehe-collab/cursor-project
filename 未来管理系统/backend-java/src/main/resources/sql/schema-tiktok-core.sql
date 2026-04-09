-- ============================================
-- TikTok API 核心表结构（指令 #095-1）
-- 说明：账户、广告系列、广告组、广告；外键级联删除
-- 执行：mysql -u root -p drama_system < src/main/resources/sql/schema-tiktok-core.sql
--       或：bash scripts/import-tiktok-core-schema.sh
--
-- 兼容 #096：若已执行 tiktok-accounts.sql，则 tiktok_accounts 已存在且可能无 balance 列，
--           请先在本库执行：
--   ALTER TABLE tiktok_accounts
--     ADD COLUMN balance DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '账户余额' AFTER timezone;
--           （若列已存在会报错，可忽略）
-- ============================================

SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. TikTok 账户表
-- ============================================
CREATE TABLE IF NOT EXISTS `tiktok_accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `advertiser_name` VARCHAR(200) DEFAULT NULL COMMENT '广告主名称',
  `access_token` TEXT COMMENT 'Access Token',
  `refresh_token` TEXT COMMENT 'Refresh Token',
  `token_expires_at` DATETIME DEFAULT NULL COMMENT 'Access Token 过期时间',
  `currency` VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '货币（USD / CNY 等）',
  `timezone` VARCHAR(64) NOT NULL DEFAULT 'UTC' COMMENT '时区',
  `balance` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '账户余额',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active / inactive',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tiktok_accounts_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_accounts_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok 账户表';

-- ============================================
-- 2. TikTok 广告系列表
-- ============================================
CREATE TABLE IF NOT EXISTS `tiktok_campaigns` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `campaign_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告系列 ID',
  `campaign_name` VARCHAR(200) DEFAULT NULL COMMENT '广告系列名称',
  `objective` VARCHAR(50) DEFAULT NULL COMMENT '推广目标枚举',
  `budget` DECIMAL(10, 2) DEFAULT NULL COMMENT '预算',
  `budget_mode` VARCHAR(20) DEFAULT NULL COMMENT '日预算/总预算等',
  `operation_status` VARCHAR(20) DEFAULT NULL COMMENT 'ENABLE / DISABLE / DELETE 等',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tiktok_campaigns_campaign_id` (`campaign_id`),
  KEY `idx_tiktok_campaigns_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_campaigns_operation_status` (`operation_status`),
  CONSTRAINT `fk_tiktok_campaigns_advertiser`
    FOREIGN KEY (`advertiser_id`) REFERENCES `tiktok_accounts` (`advertiser_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok 广告系列表';

-- ============================================
-- 3. TikTok 广告组表
-- ============================================
CREATE TABLE IF NOT EXISTS `tiktok_adgroups` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `campaign_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告系列 ID',
  `adgroup_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告组 ID',
  `adgroup_name` VARCHAR(200) DEFAULT NULL COMMENT '广告组名称',
  `placement_type` VARCHAR(20) DEFAULT NULL COMMENT '投放位置类型',
  `placements` TEXT COMMENT '投放位置列表（JSON）',
  `budget` DECIMAL(10, 2) DEFAULT NULL COMMENT '预算',
  `budget_mode` VARCHAR(20) DEFAULT NULL COMMENT '预算模式',
  `billing_event` VARCHAR(50) DEFAULT NULL COMMENT '计费事件',
  `bid_type` VARCHAR(20) DEFAULT NULL COMMENT '出价类型',
  `bid_price` DECIMAL(10, 4) DEFAULT NULL COMMENT '出价',
  `location_ids` TEXT COMMENT '地域定向（JSON）',
  `age_groups` TEXT COMMENT '年龄定向（JSON）',
  `gender` VARCHAR(20) DEFAULT NULL COMMENT '性别定向',
  `languages` TEXT COMMENT '语言定向（JSON）',
  `interest_category_ids` TEXT COMMENT '兴趣定向（JSON）',
  `schedule_type` VARCHAR(20) DEFAULT NULL COMMENT '排期类型',
  `schedule_start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `schedule_end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `operation_status` VARCHAR(20) DEFAULT NULL COMMENT '操作状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tiktok_adgroups_adgroup_id` (`adgroup_id`),
  KEY `idx_tiktok_adgroups_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_adgroups_campaign_id` (`campaign_id`),
  KEY `idx_tiktok_adgroups_operation_status` (`operation_status`),
  CONSTRAINT `fk_tiktok_adgroups_advertiser`
    FOREIGN KEY (`advertiser_id`) REFERENCES `tiktok_accounts` (`advertiser_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tiktok_adgroups_campaign`
    FOREIGN KEY (`campaign_id`) REFERENCES `tiktok_campaigns` (`campaign_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok 广告组表';

-- ============================================
-- 4. TikTok 广告表
-- ============================================
CREATE TABLE IF NOT EXISTS `tiktok_ads` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `campaign_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告系列 ID',
  `adgroup_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告组 ID',
  `ad_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告 ID',
  `ad_name` VARCHAR(200) DEFAULT NULL COMMENT '广告名称',
  `creative_type` VARCHAR(50) DEFAULT NULL COMMENT 'VIDEO / IMAGE 等',
  `video_id` VARCHAR(64) DEFAULT NULL COMMENT '视频 ID',
  `image_ids` TEXT COMMENT '图片 ID 列表（JSON）',
  `ad_text` TEXT COMMENT '广告文案',
  `call_to_action` VARCHAR(50) DEFAULT NULL COMMENT '行动号召',
  `landing_page_url` TEXT COMMENT '落地页 URL',
  `display_name` VARCHAR(200) DEFAULT NULL COMMENT '显示名称',
  `pixel_id` VARCHAR(64) DEFAULT NULL COMMENT 'Pixel ID',
  `operation_status` VARCHAR(20) DEFAULT NULL COMMENT '操作状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tiktok_ads_ad_id` (`ad_id`),
  KEY `idx_tiktok_ads_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_ads_campaign_id` (`campaign_id`),
  KEY `idx_tiktok_ads_adgroup_id` (`adgroup_id`),
  KEY `idx_tiktok_ads_operation_status` (`operation_status`),
  CONSTRAINT `fk_tiktok_ads_advertiser`
    FOREIGN KEY (`advertiser_id`) REFERENCES `tiktok_accounts` (`advertiser_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tiktok_ads_campaign`
    FOREIGN KEY (`campaign_id`) REFERENCES `tiktok_campaigns` (`campaign_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tiktok_ads_adgroup`
    FOREIGN KEY (`adgroup_id`) REFERENCES `tiktok_adgroups` (`adgroup_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok 广告表';

SET FOREIGN_KEY_CHECKS = 1;
