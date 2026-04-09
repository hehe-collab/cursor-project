-- ============================================
-- TikTok API 任务表结构（指令 #095-3）
-- 说明：广告任务队列、Excel 导入记录、同步/API 调用日志
-- 依赖：schema-tiktok-core.sql（需先存在 tiktok_accounts）
-- 执行：mysql -u root -p drama_system < src/main/resources/sql/schema-tiktok-tasks.sql
--       或：bash scripts/import-tiktok-tasks-schema.sh
-- ============================================

SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. TikTok 广告任务表
-- ============================================
CREATE TABLE IF NOT EXISTS `tiktok_ad_tasks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `task_type` VARCHAR(50) NOT NULL COMMENT 'create_campaign / create_adgroup / create_ad 等',
  `task_name` VARCHAR(200) DEFAULT NULL COMMENT '任务名称',
  `task_params` JSON DEFAULT NULL COMMENT '任务参数 JSON',
  `target_id` VARCHAR(64) DEFAULT NULL COMMENT 'campaign_id / adgroup_id / ad_id',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending / processing / success / failed',
  `result_id` VARCHAR(64) DEFAULT NULL COMMENT '执行结果业务 ID',
  `result_data` JSON DEFAULT NULL COMMENT 'API 响应等 JSON',
  `error_message` TEXT COMMENT '错误信息',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `max_retries` INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级，越大越优先',
  `scheduled_at` DATETIME DEFAULT NULL COMMENT '计划执行时间',
  `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人（管理员）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tiktok_ad_tasks_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_ad_tasks_task_type` (`task_type`),
  KEY `idx_tiktok_ad_tasks_status` (`status`),
  KEY `idx_tiktok_ad_tasks_target_id` (`target_id`),
  KEY `idx_tiktok_ad_tasks_priority` (`priority`),
  KEY `idx_tiktok_ad_tasks_scheduled_at` (`scheduled_at`),
  KEY `idx_tiktok_ad_tasks_retry` (`status`, `retry_count`),
  KEY `idx_tiktok_ad_tasks_created_by` (`created_by`),
  CONSTRAINT `fk_tiktok_ad_tasks_advertiser`
    FOREIGN KEY (`advertiser_id`) REFERENCES `tiktok_accounts` (`advertiser_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok 广告任务表';

-- ============================================
-- 2. TikTok Excel 导入记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `tiktok_excel_imports` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) NOT NULL COMMENT 'TikTok 广告主 ID',
  `import_type` VARCHAR(50) NOT NULL COMMENT 'campaigns / adgroups / ads / creatives 等',
  `file_path` VARCHAR(500) DEFAULT NULL COMMENT '存储路径',
  `original_filename` VARCHAR(200) DEFAULT NULL COMMENT '原始文件名',
  `file_size` BIGINT DEFAULT NULL COMMENT '字节数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending / processing / success / failed / partial',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '总行数',
  `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数',
  `failed_count` INT NOT NULL DEFAULT 0 COMMENT '失败数',
  `error_logs` JSON DEFAULT NULL COMMENT '错误明细 JSON 数组',
  `started_at` DATETIME DEFAULT NULL COMMENT '开始处理时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tiktok_excel_imports_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_excel_imports_import_type` (`import_type`),
  KEY `idx_tiktok_excel_imports_status` (`status`),
  KEY `idx_tiktok_excel_imports_created_by` (`created_by`),
  KEY `idx_tiktok_excel_imports_created_at` (`created_at`),
  CONSTRAINT `fk_tiktok_excel_imports_advertiser`
    FOREIGN KEY (`advertiser_id`) REFERENCES `tiktok_accounts` (`advertiser_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok Excel 导入记录表';

-- ============================================
-- 3. TikTok 同步日志表（advertiser_id 可空：如拉取账户列表）
-- ============================================
CREATE TABLE IF NOT EXISTS `tiktok_sync_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `advertiser_id` VARCHAR(64) DEFAULT NULL COMMENT '广告主 ID，可空',
  `sync_type` VARCHAR(50) NOT NULL COMMENT 'report / account / campaign 等',
  `api_endpoint` VARCHAR(200) DEFAULT NULL COMMENT 'API 路径',
  `request_method` VARCHAR(10) NOT NULL DEFAULT 'GET' COMMENT 'GET / POST / PUT / DELETE',
  `request_params` JSON DEFAULT NULL COMMENT '请求参数 JSON',
  `response_code` INT DEFAULT NULL COMMENT 'HTTP 状态码',
  `response_data` JSON DEFAULT NULL COMMENT '响应 JSON',
  `status` VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT 'success / failed',
  `error_message` TEXT COMMENT '错误信息',
  `duration_ms` INT DEFAULT NULL COMMENT '耗时毫秒',
  `executed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tiktok_sync_logs_advertiser_id` (`advertiser_id`),
  KEY `idx_tiktok_sync_logs_sync_type` (`sync_type`),
  KEY `idx_tiktok_sync_logs_status` (`status`),
  KEY `idx_tiktok_sync_logs_executed_at` (`executed_at`),
  KEY `idx_tiktok_sync_logs_api_endpoint` (`api_endpoint`),
  CONSTRAINT `fk_tiktok_sync_logs_advertiser`
    FOREIGN KEY (`advertiser_id`) REFERENCES `tiktok_accounts` (`advertiser_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TikTok 同步日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- JSON 列需 MySQL 5.7.8+；定期清理 tiktok_sync_logs 见业务侧定时任务。
