-- ============================================
-- TikTok API 扩展表结构（指令 #095-2）
-- 说明：Pixel、回传日志、数据报表
-- 依赖：schema-tiktok-core.sql（需先存在 tiktok_accounts）
-- 执行：mysql -u root -p drama_system < src/main/resources/sql/schema-tiktok-extended.sql
--       或：bash scripts/import-tiktok-extended-schema.sh
-- ============================================

SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. TikTok Pixel 表
-- ============================================
CREATE TABLE IF NOT EXISTS `tiktok_pixels` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `pixel_id` VARCHAR(64) NOT NULL COMMENT 'Pixel ID',
  `pixel_name` VARCHAR(200) DEFAULT NULL COMMENT 'Pixel 名称',
  `pixel_code` TEXT COMMENT 'Pixel 代码（JavaScript）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active / inactive',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tiktok_pixels_pixel_id` (`pixel_id`),
  KEY `idx_tiktok_pixels_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_pixels_status` (`status`),
  CONSTRAINT `fk_tiktok_pixels_advertiser`
    FOREIGN KEY (`advertiser_id`) REFERENCES `tiktok_accounts` (`advertiser_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok Pixel 表';

-- ============================================
-- 2. TikTok 回传日志表（pixel_id 可空，以支持 ON DELETE SET NULL）
-- ============================================
CREATE TABLE IF NOT EXISTS `tiktok_conversion_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `pixel_id` VARCHAR(64) DEFAULT NULL COMMENT 'Pixel ID（可空）',
  `event_type` VARCHAR(50) NOT NULL COMMENT 'Purchase / CompleteRegistration 等',
  `event_id` VARCHAR(100) NOT NULL COMMENT '事件唯一 ID，防重复',
  `user_id` VARCHAR(100) DEFAULT NULL COMMENT '用户 ID',
  `click_id` VARCHAR(100) DEFAULT NULL COMMENT 'ttclid',
  `external_id` VARCHAR(100) DEFAULT NULL COMMENT '外部用户 ID',
  `event_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '事件价值（如充值金额）',
  `currency` VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '货币',
  `content_type` VARCHAR(50) DEFAULT NULL COMMENT '内容类型',
  `content_id` VARCHAR(100) DEFAULT NULL COMMENT '内容 ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending / success / failed',
  `response_code` INT DEFAULT NULL COMMENT 'API 响应码',
  `response_message` TEXT COMMENT 'API 响应消息',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `event_time` DATETIME NOT NULL COMMENT '事件发生时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tiktok_conversion_logs_event_id` (`event_id`),
  KEY `idx_tiktok_cl_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_cl_pixel_id` (`pixel_id`),
  KEY `idx_tiktok_cl_event_type` (`event_type`),
  KEY `idx_tiktok_cl_status` (`status`),
  KEY `idx_tiktok_cl_event_time` (`event_time`),
  KEY `idx_tiktok_cl_retry` (`status`, `retry_count`),
  CONSTRAINT `fk_tiktok_conversion_logs_advertiser`
    FOREIGN KEY (`advertiser_id`) REFERENCES `tiktok_accounts` (`advertiser_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tiktok_conversion_logs_pixel`
    FOREIGN KEY (`pixel_id`) REFERENCES `tiktok_pixels` (`pixel_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok 回传日志表';

-- ============================================
-- 3. TikTok 报告表（唯一键便于 ON DUPLICATE KEY UPDATE）
-- ============================================
CREATE TABLE IF NOT EXISTS `tiktok_reports` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `dimensions` VARCHAR(20) NOT NULL COMMENT 'campaign / adgroup / ad',
  `dimension_id` VARCHAR(64) NOT NULL COMMENT '对应维度实体 ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `spend` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '消耗',
  `impressions` INT NOT NULL DEFAULT 0 COMMENT '曝光',
  `clicks` INT NOT NULL DEFAULT 0 COMMENT '点击',
  `ctr` DECIMAL(10, 4) NOT NULL DEFAULT 0 COMMENT 'CTR',
  `cpc` DECIMAL(10, 4) NOT NULL DEFAULT 0 COMMENT 'CPC',
  `cpm` DECIMAL(10, 4) NOT NULL DEFAULT 0 COMMENT 'CPM',
  `conversions` INT NOT NULL DEFAULT 0 COMMENT '转化数',
  `conversion_rate` DECIMAL(10, 4) NOT NULL DEFAULT 0 COMMENT '转化率',
  `cost_per_conversion` DECIMAL(10, 4) NOT NULL DEFAULT 0 COMMENT '转化成本',
  `conversion_value` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '转化价值',
  `roi` DECIMAL(10, 4) NOT NULL DEFAULT 0 COMMENT 'ROI',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tiktok_reports_dim` (`advertiser_id`, `dimensions`, `dimension_id`, `stat_date`),
  KEY `idx_tiktok_reports_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_reports_dimensions` (`dimensions`),
  KEY `idx_tiktok_reports_dimension_id` (`dimension_id`),
  KEY `idx_tiktok_reports_stat_date` (`stat_date`),
  KEY `idx_tiktok_reports_spend` (`spend`),
  KEY `idx_tiktok_reports_roi` (`roi`),
  CONSTRAINT `fk_tiktok_reports_advertiser`
    FOREIGN KEY (`advertiser_id`) REFERENCES `tiktok_accounts` (`advertiser_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok 数据报告表';

SET FOREIGN_KEY_CHECKS = 1;

-- 应用层写入建议：INSERT ... ON DUPLICATE KEY UPDATE 同步 API 指标并在应用层计算 ctr/cpc/cpm 等后写入。
