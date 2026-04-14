-- =============================================================================
-- 增量补表：仅「补建」#048～#053 相关表（与 schema.sql 定义一字一致）
-- 适用：库已存在且已有早期表，但未执行过含 promotion_links～ad_tasks 的完整 DDL
--
-- ⚠️ 不要使用网上通用「投放链接 name/link_url/BIGINT」等模板，与 Java Mapper 不兼容。
--
-- 用法：
--   mysql -u root -p drama_system < src/main/resources/sql/fix-missing-tables-after-migration.sql
--
-- 若希望库与线上一致：仍推荐整库备份后执行完整 schema.sql（见 README）
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- #048 投放链接（表名 promotion_links，不是泛用 delivery 模板）
CREATE TABLE IF NOT EXISTS `promotion_links` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `promote_id` VARCHAR(64) NOT NULL COMMENT '业务推广 ID',
  `platform` VARCHAR(64) NOT NULL DEFAULT '',
  `country` VARCHAR(32) NOT NULL DEFAULT '',
  `promote_name` VARCHAR(255) NOT NULL DEFAULT '',
  `drama_id` INT DEFAULT NULL,
  `plan_group_id` INT DEFAULT NULL,
  `bean_count` INT NOT NULL DEFAULT 0,
  `free_episodes` INT NOT NULL DEFAULT 0,
  `preview_episodes` INT NOT NULL DEFAULT 0,
  `domain` VARCHAR(255) NOT NULL DEFAULT '',
  `drama_name` VARCHAR(255) NOT NULL DEFAULT '',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active',
  `stat` VARCHAR(64) DEFAULT NULL,
  `amount` DECIMAL(14,2) DEFAULT NULL,
  `spend` DECIMAL(14,2) DEFAULT NULL,
  `target` VARCHAR(255) DEFAULT NULL,
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人（ID 数字或昵称，与 Node 一致）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pl_promote_id` (`promote_id`),
  KEY `idx_pl_drama` (`drama_id`),
  KEY `idx_pl_plan_group` (`plan_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投放链接';

-- #049 广告账户
CREATE TABLE IF NOT EXISTS `ad_accounts` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `media` VARCHAR(64) NOT NULL DEFAULT '',
  `country` VARCHAR(16) NOT NULL DEFAULT '',
  `subject_name` VARCHAR(255) NOT NULL DEFAULT '',
  `account_id` VARCHAR(100) NOT NULL DEFAULT '',
  `account_name` VARCHAR(255) NOT NULL DEFAULT '',
  `media_alias` VARCHAR(128) NOT NULL DEFAULT '',
  `account_agent` VARCHAR(64) NOT NULL DEFAULT '',
  `access_token_encrypted` TEXT NULL COMMENT '加密后的 Access Token',
  `refresh_token_encrypted` TEXT NULL COMMENT '加密后的 Refresh Token',
  `token_expires_at` DATETIME DEFAULT NULL COMMENT 'Token 过期时间',
  `created_by` INT DEFAULT NULL,
  `created_by_name` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_aa_media` (`media`),
  KEY `idx_aa_country` (`country`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告账户';

-- #050 标题包
CREATE TABLE IF NOT EXISTS `title_packs` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(500) NOT NULL DEFAULT '',
  `content` TEXT,
  `created_by` INT DEFAULT NULL,
  `created_by_name` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tp_name` (`name`(190))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标题包';

-- #051 广告素材
CREATE TABLE IF NOT EXISTS `ad_materials` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `material_id` VARCHAR(64) NOT NULL COMMENT '业务素材号 MAT…',
  `material_name` VARCHAR(255) NOT NULL DEFAULT '',
  `type` VARCHAR(32) NOT NULL DEFAULT 'image',
  `entity_name` VARCHAR(255) NOT NULL DEFAULT '',
  `account_id` VARCHAR(100) NOT NULL DEFAULT '',
  `video_id` VARCHAR(100) NOT NULL DEFAULT '',
  `cover_url` TEXT,
  `created_by` INT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_am_material_id` (`material_id`),
  KEY `idx_am_account` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告素材';

CREATE TABLE IF NOT EXISTS `ad_material_records` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `account_id` VARCHAR(100) NOT NULL DEFAULT '',
  `account_name` VARCHAR(255) NOT NULL DEFAULT '',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending',
  `task_type` VARCHAR(32) NOT NULL DEFAULT 'upload',
  `detail` TEXT,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_amr_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告素材任务记录';

-- #052 回传（Node 契约：无 name/config_id 等泛用字段）
CREATE TABLE IF NOT EXISTS `callback_configs` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `link_id` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '投放链接关键字/推广链 id',
  `platform` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '媒体',
  `cold_start_count` INT NOT NULL DEFAULT 0 COMMENT '冷启动次数',
  `min_price_limit` INT NOT NULL DEFAULT 0 COMMENT '最低客单价',
  `replenish_callback_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '复充回传：1开启 0关闭',
  `config_json` TEXT NULL COMMENT '策略配置JSON',
  `creator` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cc_platform` (`platform`),
  KEY `idx_cc_link_id` (`link_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回传配置';

CREATE TABLE IF NOT EXISTS `callback_logs` (
  `id` BIGINT NOT NULL,
  `order_no` VARCHAR(128) DEFAULT NULL,
  `order_id` VARCHAR(128) DEFAULT NULL,
  `event` VARCHAR(100) DEFAULT NULL,
  `event_type` VARCHAR(100) DEFAULT NULL,
  `pixel_id` VARCHAR(512) DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '成功/失败/待处理 或 success/failed/pending',
  `error_message` TEXT,
  `retry_count` INT NOT NULL DEFAULT 0,
  `send_time` DATETIME DEFAULT NULL,
  `sent_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cl_created` (`created_at`),
  KEY `idx_cl_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回传日志';

-- #053 广告任务（含自增 id + 业务 task_id）
CREATE TABLE IF NOT EXISTS `ad_tasks` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(64) NOT NULL COMMENT '业务任务号',
  `account_ids` TEXT,
  `account_names` TEXT,
  `promotion_type` VARCHAR(64) NOT NULL DEFAULT '',
  `status` VARCHAR(32) NOT NULL DEFAULT 'running',
  `created_by` VARCHAR(100) NOT NULL DEFAULT '',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `config_json` TEXT COMMENT '任务配置 JSON',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ad_tasks_task_id` (`task_id`),
  KEY `idx_ad_tasks_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告任务';

SET FOREIGN_KEY_CHECKS = 1;

-- #055 recharge_records.platform（不用 ADD COLUMN IF NOT EXISTS：部分 MySQL 9.x 仍报语法错）
SET @db := DATABASE();
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'recharge_records' AND COLUMN_NAME = 'platform'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `recharge_records` ADD COLUMN `platform` VARCHAR(64) NULL COMMENT ''媒体/渠道（统计筛选用）'' AFTER `payment_method`',
  'SELECT ''skip: recharge_records.platform exists'' AS migration_note'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
