-- =============================================================================
-- 增量补表：充值方案 / 方案组 / 关联表（与 schema.sql #17～#18 定义一致）
-- 适用：库中已有早期表但缺 recharge_plans / recharge_plan_groups / recharge_plan_group_plans
--
-- ⚠️ 不要执行网络泛用「price/beans BIGINT」模板；与 Java Mapper、Node 迁移不一致。
-- ⚠️ 不创建或替换 recharge_records：该表结构见 schema.sql §16，错误 DDL 会破坏现有数据。
--
-- 用法：
--   mysql -u root -p drama_system < src/main/resources/sql/fix-recharge-tables.sql
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `recharge_plans` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(200) NOT NULL COMMENT '方案名称',
  `bean_count` INT NOT NULL DEFAULT 0 COMMENT '实际到账金豆',
  `extra_bean` INT NOT NULL DEFAULT 0 COMMENT '赠送金豆',
  `amount` DECIMAL(12,1) NOT NULL DEFAULT 0.0 COMMENT '标价金额',
  `recharge_info` VARCHAR(500) DEFAULT NULL COMMENT '充值信息文案',
  `pay_platform` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '支付平台',
  `currency` VARCHAR(16) NOT NULL DEFAULT 'USD' COMMENT '货币',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active/inactive',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `unlock_full_series` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否解锁全集',
  `plan_uuid` VARCHAR(64) DEFAULT NULL COMMENT '业务 UUID（展示用）',
  `created_by` INT DEFAULT NULL,
  `created_by_name` VARCHAR(100) DEFAULT NULL,
  `is_recommended` TINYINT(1) NOT NULL DEFAULT 0,
  `is_hot` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rp_status` (`status`),
  KEY `idx_rp_pay` (`pay_platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值方案';

CREATE TABLE IF NOT EXISTS `recharge_plan_groups` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(200) NOT NULL DEFAULT '' COMMENT '名称',
  `group_name` VARCHAR(200) NOT NULL DEFAULT '' COMMENT '分组名',
  `group_public_id` VARCHAR(64) NOT NULL COMMENT '展示用分组ID 如 RG_xxx',
  `sort_order` INT NOT NULL DEFAULT 999,
  `description` TEXT,
  `status` VARCHAR(32) NOT NULL DEFAULT 'active',
  `group_uuid` VARCHAR(64) DEFAULT NULL,
  `item_no` VARCHAR(255) DEFAULT NULL,
  `item_token` VARCHAR(500) DEFAULT NULL,
  `media_platform` VARCHAR(64) DEFAULT NULL,
  `pixel_id` VARCHAR(255) DEFAULT NULL,
  `pixel_token` TEXT,
  `creator` VARCHAR(100) DEFAULT NULL,
  `created_by` INT DEFAULT NULL,
  `created_by_name` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rpg_public_id` (`group_public_id`),
  KEY `idx_rpg_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值方案组';

CREATE TABLE IF NOT EXISTS `recharge_plan_group_plans` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` INT NOT NULL,
  `plan_id` INT NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rpgp_gp` (`group_id`,`plan_id`),
  KEY `idx_rpgp_group` (`group_id`),
  CONSTRAINT `fk_rpgp_group` FOREIGN KEY (`group_id`) REFERENCES `recharge_plan_groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rpgp_plan` FOREIGN KEY (`plan_id`) REFERENCES `recharge_plans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='方案组与方案关联';

SET FOREIGN_KEY_CHECKS = 1;
