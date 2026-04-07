-- 推广明细相关表（指令 #078）
-- 依赖：已在库 drama_system 下执行 schema.sql
-- 执行：mysql -u root -p drama_system < src/main/resources/sql/schema-promotion.sql

SET NAMES utf8mb4;

-- 1. 充值订单（独立订单表；推广 ID 对齐 promotion_links.promote_id）
CREATE TABLE IF NOT EXISTS `recharge_orders` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` VARCHAR(100) NOT NULL COMMENT '订单号',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `promotion_id` VARCHAR(100) DEFAULT NULL COMMENT '推广ID（对齐 promotion_links.promote_id）',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '充值金额',
  `coins` INT NOT NULL COMMENT '充值金币数',
  `is_first_recharge` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否首充：1=首充，0=复充',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '充值时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recharge_orders_order_id` (`order_id`),
  KEY `idx_recharge_orders_user_id` (`user_id`),
  KEY `idx_recharge_orders_promotion_id` (`promotion_id`),
  KEY `idx_recharge_orders_created_at` (`created_at`),
  KEY `idx_recharge_orders_is_first` (`is_first_recharge`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值订单表';

-- 2. TikTok 消耗记录（每约 10 分钟一条；累计消耗 cost，时速由应用层算）
CREATE TABLE IF NOT EXISTS `tiktok_cost_records` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `promotion_id` VARCHAR(100) NOT NULL COMMENT '推广ID（对齐 promotion_links.promote_id）',
  `account_id` VARCHAR(100) DEFAULT NULL COMMENT '广告账户ID',
  `account_name` VARCHAR(200) DEFAULT NULL COMMENT '账户名称',
  `balance` DECIMAL(10,2) DEFAULT NULL COMMENT '账户余额',
  `campaign_name` VARCHAR(200) DEFAULT NULL COMMENT '系列/广告组名称',
  `cost` DECIMAL(10,2) NOT NULL COMMENT '累计消耗金额',
  `impressions` BIGINT NOT NULL DEFAULT 0 COMMENT '曝光数',
  `record_time` DATETIME NOT NULL COMMENT '记录时间（每10分钟对齐）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tiktok_cost_promotion_time` (`promotion_id`, `record_time`),
  KEY `idx_tiktok_cost_promotion_id` (`promotion_id`),
  KEY `idx_tiktok_cost_record_time` (`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok消耗记录表（每10分钟）';

-- 3. 推广明细按日汇总（提高查询性能；指标由定时任务/同步写入）
CREATE TABLE IF NOT EXISTS `promotion_details_summary` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `date` DATE NOT NULL COMMENT '日期',
  `promotion_id` VARCHAR(100) NOT NULL COMMENT '推广ID（对齐 promotion_links.promote_id）',
  `promotion_name` VARCHAR(200) DEFAULT NULL COMMENT '推广名称',
  `platform` VARCHAR(50) DEFAULT NULL COMMENT '投放媒体（tiktok/facebook/google）',
  `country` VARCHAR(10) DEFAULT NULL COMMENT '国家代码（TH/ID/VN/PH/MY）',
  `drama_id` INT DEFAULT NULL COMMENT '短剧ID',
  `drama_name` VARCHAR(200) DEFAULT NULL COMMENT '短剧名称',
  `account_id` VARCHAR(100) DEFAULT NULL COMMENT '广告账户ID',
  `account_name` VARCHAR(200) DEFAULT NULL COMMENT '账户名称',
  `balance` DECIMAL(10,2) DEFAULT NULL COMMENT '账户余额（最新）',
  `campaign_name` VARCHAR(200) DEFAULT NULL COMMENT '系列/广告组名称',
  `cost` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '当日消耗',
  `speed` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '时速（最新10分钟消耗×6）',
  `roi` DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT 'ROI = 利润 ÷ 消耗',
  `user_count` INT NOT NULL DEFAULT 0 COMMENT '当日新增用户数',
  `recharge_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '当日充值金额',
  `profit` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '当日利润 = 充值金额 - 消耗',
  `order_count` INT NOT NULL DEFAULT 0 COMMENT '当日订单数',
  `first_recharge_count` INT NOT NULL DEFAULT 0 COMMENT '当日首充数',
  `first_recharge_rate` DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '首充率（#079 按业务口径计算，如 订单数÷用户数 等）',
  `repeat_recharge_count` INT NOT NULL DEFAULT 0 COMMENT '当日复充数',
  `impressions` BIGINT NOT NULL DEFAULT 0 COMMENT '日曝光（冗余）',
  `cpm` DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '千次曝光成本 = 消耗 ÷ 曝光数 × 1000',
  `avg_recharge_per_user` DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '人均充值 = 当日充值金额 ÷ 首充数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_promo_summary_date_promotion` (`date`, `promotion_id`),
  KEY `idx_promo_summary_date` (`date`),
  KEY `idx_promo_summary_promotion_id` (`promotion_id`),
  KEY `idx_promo_summary_platform` (`platform`),
  KEY `idx_promo_summary_country` (`country`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广明细汇总表（按日）';
