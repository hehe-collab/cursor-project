-- 未来管理系统 MySQL DDL（指令 #002）
-- 字符集 utf8mb4；请在库 drama_system 下执行：mysql -u root -p drama_system < src/main/resources/sql/schema.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- #087：库默认排序规则与下列表一致，避免与 MySQL 8 默认 utf8mb4_0900_ai_ci 混用导致 JOIN 报错
ALTER DATABASE `drama_system` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 1. 管理员
DROP TABLE IF EXISTS `admins`;
CREATE TABLE `admins` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录名',
  `password` VARCHAR(255) NOT NULL COMMENT 'bcrypt 密码',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `role` VARCHAR(32) NOT NULL DEFAULT 'admin' COMMENT '角色',
  `password_changed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已修改初始密码：0=未改，1=已改',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admins_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台管理员';

-- 2. 终端用户（C 端）
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(100) DEFAULT NULL COMMENT '用户名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1正常',
  `promote_id` VARCHAR(100) DEFAULT NULL COMMENT '推广 ID',
  `promote_name` VARCHAR(100) DEFAULT NULL COMMENT '推广名称',
  `coin_balance` INT NOT NULL DEFAULT 0 COMMENT '金豆余额',
  `token` VARCHAR(255) DEFAULT NULL COMMENT '业务 Token',
  `country` VARCHAR(50) DEFAULT NULL COMMENT '国家/地区',
  `new_user_id` VARCHAR(100) DEFAULT NULL COMMENT '新用户 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_users_promote_id` (`promote_id`),
  KEY `idx_users_username` (`username`(50))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='终端用户';

-- 3. 分类
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短剧分类';

-- 4. 标签
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '标签名',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tags_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签';

-- 5. 短剧
DROP TABLE IF EXISTS `dramas`;
CREATE TABLE `dramas` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `cover` VARCHAR(500) DEFAULT NULL COMMENT '封面',
  `description` TEXT COMMENT '简介',
  `category_id` INT DEFAULT NULL COMMENT '分类 ID',
  `status` ENUM('draft','published','offline') NOT NULL DEFAULT 'draft' COMMENT '上架状态',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '播放量',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `display_name` VARCHAR(255) DEFAULT NULL COMMENT '展示名',
  `display_text` VARCHAR(500) DEFAULT NULL COMMENT '展示文本',
  `beans_per_episode` INT NOT NULL DEFAULT 5 COMMENT '每集金豆',
  `total_episodes` INT NOT NULL DEFAULT 0 COMMENT '总集数',
  `free_episodes` INT NOT NULL DEFAULT 0 COMMENT '免费集数',
  `oss_path` TEXT COMMENT 'OSS 路径',
  `category` VARCHAR(100) DEFAULT NULL COMMENT '分类标签',
  `task_status` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '任务状态',
  `is_online` TINYINT NOT NULL DEFAULT 1 COMMENT '是否上架',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dramas_category` (`category_id`),
  CONSTRAINT `fk_dramas_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短剧';

-- 6. 剧集（对应 Node 的 drama_episodes）
DROP TABLE IF EXISTS `drama_episodes`;
CREATE TABLE `drama_episodes` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `drama_id` INT NOT NULL COMMENT '短剧 ID',
  `episode_num` INT NOT NULL COMMENT '集序号',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '集标题',
  `video_id` VARCHAR(100) DEFAULT NULL COMMENT '视频 ID',
  `video_url` VARCHAR(500) DEFAULT NULL COMMENT '视频地址',
  `vod_video_id` VARCHAR(100) DEFAULT NULL COMMENT '阿里云 VOD 视频 ID',
  `vod_status` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT 'VOD 状态：manual/uploading/transcoding/normal/failed',
  `video_size` BIGINT NOT NULL DEFAULT 0 COMMENT '视频大小（字节）',
  `vod_cover_url` VARCHAR(500) DEFAULT NULL COMMENT 'VOD 封面地址',
  `duration` INT NOT NULL DEFAULT 0 COMMENT '时长秒',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_drama_episode` (`drama_id`,`episode_num`),
  KEY `idx_episodes_vod_video_id` (`vod_video_id`),
  CONSTRAINT `fk_episodes_drama` FOREIGN KEY (`drama_id`) REFERENCES `dramas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='剧集';

-- 7. 短剧-标签
DROP TABLE IF EXISTS `drama_tags`;
CREATE TABLE `drama_tags` (
  `drama_id` INT NOT NULL COMMENT '短剧 ID',
  `tag_id` INT NOT NULL COMMENT '标签 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`drama_id`,`tag_id`),
  CONSTRAINT `fk_dt_drama` FOREIGN KEY (`drama_id`) REFERENCES `dramas` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_dt_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短剧标签关联';

-- 8. 观看历史
DROP TABLE IF EXISTS `user_watch_history`;
CREATE TABLE `user_watch_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_pk` INT NOT NULL COMMENT 'users.id',
  `drama_id` INT NOT NULL COMMENT '短剧 ID',
  `episode_num` INT NOT NULL DEFAULT 1 COMMENT '看到第几集',
  `progress_sec` INT NOT NULL DEFAULT 0 COMMENT '进度秒',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_uwh_user` (`user_pk`),
  KEY `idx_uwh_drama` (`drama_id`),
  CONSTRAINT `fk_uwh_user` FOREIGN KEY (`user_pk`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_uwh_drama` FOREIGN KEY (`drama_id`) REFERENCES `dramas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='观看历史';

-- 9. 收藏
DROP TABLE IF EXISTS `user_favorites`;
CREATE TABLE `user_favorites` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_pk` INT NOT NULL COMMENT 'users.id',
  `drama_id` INT NOT NULL COMMENT '短剧 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fav_user_drama` (`user_pk`,`drama_id`),
  CONSTRAINT `fk_fav_user` FOREIGN KEY (`user_pk`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fav_drama` FOREIGN KEY (`drama_id`) REFERENCES `dramas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏';

-- 10. 金豆流水
DROP TABLE IF EXISTS `coin_transactions`;
CREATE TABLE `coin_transactions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_pk` INT NOT NULL COMMENT 'users.id',
  `delta` INT NOT NULL COMMENT '变动金豆（可负）',
  `reason` VARCHAR(100) DEFAULT NULL COMMENT '原因',
  `ref_type` VARCHAR(50) DEFAULT NULL COMMENT '关联类型',
  `ref_id` VARCHAR(64) DEFAULT NULL COMMENT '关联 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ct_user` (`user_pk`),
  CONSTRAINT `fk_ct_user` FOREIGN KEY (`user_pk`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='金豆流水';

-- 11. 系统设置
DROP TABLE IF EXISTS `settings`;
CREATE TABLE `settings` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `key_name` VARCHAR(100) NOT NULL COMMENT '键',
  `value` TEXT COMMENT '值',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settings_key` (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统设置';

-- 12. 每日统计（全站）
DROP TABLE IF EXISTS `daily_stats`;
CREATE TABLE `daily_stats` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date` DATE NOT NULL COMMENT '统计日',
  `metric` VARCHAR(64) NOT NULL COMMENT '指标名',
  `metric_value` BIGINT NOT NULL DEFAULT 0 COMMENT '指标值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_daily_metric` (`stat_date`,`metric`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日统计';

-- 13. 用户每日统计
DROP TABLE IF EXISTS `user_daily_stats`;
CREATE TABLE `user_daily_stats` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_pk` INT NOT NULL COMMENT 'users.id',
  `stat_date` DATE NOT NULL COMMENT '统计日',
  `metric` VARCHAR(64) NOT NULL COMMENT '指标名',
  `metric_value` BIGINT NOT NULL DEFAULT 0 COMMENT '指标值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uds` (`user_pk`,`stat_date`,`metric`),
  CONSTRAINT `fk_uds_user` FOREIGN KEY (`user_pk`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户每日统计';

-- 14. 短剧每日统计
DROP TABLE IF EXISTS `drama_daily_stats`;
CREATE TABLE `drama_daily_stats` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `drama_id` INT NOT NULL COMMENT '短剧 ID',
  `stat_date` DATE NOT NULL COMMENT '统计日',
  `metric` VARCHAR(64) NOT NULL COMMENT '指标名',
  `metric_value` BIGINT NOT NULL DEFAULT 0 COMMENT '指标值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dds` (`drama_id`,`stat_date`,`metric`),
  CONSTRAINT `fk_dds_drama` FOREIGN KEY (`drama_id`) REFERENCES `dramas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短剧每日统计';

-- 15. 推广每日统计
DROP TABLE IF EXISTS `promotion_daily_stats`;
CREATE TABLE `promotion_daily_stats` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `promotion_id` VARCHAR(100) NOT NULL COMMENT '推广 ID',
  `stat_date` DATE NOT NULL COMMENT '统计日',
  `metric` VARCHAR(64) NOT NULL COMMENT '指标名',
  `metric_value` BIGINT NOT NULL DEFAULT 0 COMMENT '指标值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pds` (`promotion_id`,`stat_date`,`metric`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广每日统计';

-- 16. 充值记录（对齐 storage.recharge_records 字段）
DROP TABLE IF EXISTS `recharge_records`;
CREATE TABLE `recharge_records` (
  `id` BIGINT NOT NULL COMMENT '主键（可与订单号一致）',
  `order_no` VARCHAR(100) DEFAULT NULL COMMENT '内部订单号',
  `user_id` VARCHAR(100) NOT NULL COMMENT '用户业务 ID（字符串）',
  `drama_id` INT DEFAULT NULL COMMENT '短剧 ID',
  `drama_name` VARCHAR(255) DEFAULT NULL COMMENT '剧名',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  `coins` INT DEFAULT NULL COMMENT '金豆数量',
  `payment_status` VARCHAR(32) DEFAULT NULL COMMENT '展示用支付状态',
  `pay_status` VARCHAR(32) DEFAULT NULL COMMENT '原始支付状态',
  `promotion_id` VARCHAR(100) DEFAULT NULL COMMENT '推广 ID',
  `promote_id` VARCHAR(100) DEFAULT NULL COMMENT '推广 ID 别名',
  `new_user_id` VARCHAR(100) DEFAULT NULL COMMENT '新用户 ID',
  `is_first_recharge` TINYINT(1) DEFAULT NULL COMMENT '是否首充',
  `is_new_user` TINYINT(1) DEFAULT NULL COMMENT '是否新用户',
  `local_register_time` DATETIME DEFAULT NULL COMMENT '当地注册时间',
  `local_order_time` DATETIME DEFAULT NULL COMMENT '当地订单时间',
  `local_time` DATETIME DEFAULT NULL COMMENT '注册时间原始',
  `country` VARCHAR(16) DEFAULT NULL COMMENT '国家',
  `external_order_id` VARCHAR(100) DEFAULT NULL COMMENT '外部订单号',
  `external_order_no` VARCHAR(100) DEFAULT NULL COMMENT '外部订单号别名',
  `payment_method` VARCHAR(64) DEFAULT NULL COMMENT '支付方式',
  `platform` VARCHAR(64) DEFAULT NULL COMMENT '媒体/渠道（统计筛选用，对齐 Node）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rr_user` (`user_id`(40)),
  KEY `idx_rr_promo` (`promotion_id`),
  KEY `idx_rr_order_time` (`local_order_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值记录';

-- 17. 充值方案（指令 #046，对齐 Node storage.recharge_plans）
DROP TABLE IF EXISTS `recharge_plan_group_plans`;
DROP TABLE IF EXISTS `recharge_plan_groups`;
DROP TABLE IF EXISTS `recharge_plans`;
CREATE TABLE `recharge_plans` (
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

-- 18. 充值方案组（指令 #047）
CREATE TABLE `recharge_plan_groups` (
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

CREATE TABLE `recharge_plan_group_plans` (
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

-- 19. 投放链接（指令 #048，对齐 Node storage.promotion_links / /api/delivery-links）
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

-- 20. 广告账户（指令 #049）
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

-- 21. 标题包（指令 #050）
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

-- 22. 广告素材（指令 #051）
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

-- 23. 素材同步/上传记录（Node storage.material_records）
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

-- 24. 回传配置（指令 #052，对齐 Node storage.callback_configs）
CREATE TABLE IF NOT EXISTS `callback_configs` (
                     `id` INT NOT NULL AUTO_INCREMENT,
                     `link_id` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '投放链接关键字/推广链 id',
                     `platform` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '媒体',
                     `cold_start_count` INT NOT NULL DEFAULT 0 COMMENT '冷启动次数',
                     `min_price_limit` INT NOT NULL DEFAULT 0 COMMENT '最低客单价',
                     `replenish_callback_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '复充回传：1开启 0关闭',
                     `config_json` TEXT NULL COMMENT '策略配置JSON（如 strategies 金额区间+传参）',
                     `creator` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '创建人',
                     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                     `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                     PRIMARY KEY (`id`),
                     KEY `idx_cc_platform` (`platform`),
                     KEY `idx_cc_link_id` (`link_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回传配置';

-- 25. 回传日志（对齐 Node storage.callback_logs）
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

-- 26. 广告任务（指令 #053，对齐 Node storage.ad_tasks）
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
