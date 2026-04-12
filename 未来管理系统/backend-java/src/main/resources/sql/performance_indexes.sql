-- =============================================================================
-- 性能优化索引脚本（指令 #074 阶段 1）
-- 创建日期：2026-04-07
-- 用法：mysql -u root -p drama_system < src/main/resources/sql/performance_indexes.sql
--       或无密码：mysql -u root drama_system < ...
--
-- 说明：
-- - 列名与当前库表一致（users.promote_id 非 promotion_id；promotion_links.promote_id；
--   drama_episodes.episode_num；recharge_records 无 status/paid_at，用 payment_status 等；
--   ad_accounts 为 media 非 platform；ad_materials.type 非 material_type；callback_configs.link_id）。
-- - MySQL 无可靠的 CREATE INDEX IF NOT EXISTS，故用存储过程幂等创建。
-- =============================================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_perf_index;
DELIMITER $$
CREATE PROCEDURE add_perf_index(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index
    LIMIT 1
  ) THEN
    SET @s = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '`(', p_columns, ')');
    PREPARE stmt FROM @s;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------------
-- 1. users（已有 idx_users_username、idx_users_promote_id，仅补高频筛选）
-- ---------------------------------------------------------------------------
CALL add_perf_index('users', 'idx_perf_users_country', '`country`');
CALL add_perf_index('users', 'idx_perf_users_status', '`status`');
CALL add_perf_index('users', 'idx_perf_users_created_at', '`created_at`');
CALL add_perf_index('users', 'idx_perf_users_promote_status', '`promote_id`, `status`');
CALL add_perf_index('users', 'idx_perf_users_country_created', '`country`, `created_at`');

-- ---------------------------------------------------------------------------
-- 2. dramas（已有 idx_dramas_category）
-- ---------------------------------------------------------------------------
CALL add_perf_index('dramas', 'idx_perf_dramas_title', '`title`(100)');
CALL add_perf_index('dramas', 'idx_perf_dramas_status', '`status`');
CALL add_perf_index('dramas', 'idx_perf_dramas_created_at', '`created_at`');
-- public_id 若已存在 UNIQUE（fix-058），无需再建重复索引
CALL add_perf_index('dramas', 'idx_perf_dramas_status_created', '`status`, `created_at`');

-- ---------------------------------------------------------------------------
-- 3. drama_episodes（uk_drama_episode 已覆盖 drama_id+episode_num；补按时间排序）
-- ---------------------------------------------------------------------------
CALL add_perf_index('drama_episodes', 'idx_perf_episodes_drama_created', '`drama_id`, `created_at`');

-- ---------------------------------------------------------------------------
-- 4. recharge_records（已有 idx_rr_user、idx_rr_promo、idx_rr_order_time）
-- ---------------------------------------------------------------------------
CALL add_perf_index('recharge_records', 'idx_perf_rr_order_no', '`order_no`');
CALL add_perf_index('recharge_records', 'idx_perf_rr_payment_status', '`payment_status`');
CALL add_perf_index('recharge_records', 'idx_perf_rr_created_at', '`created_at`');
CALL add_perf_index('recharge_records', 'idx_perf_rr_user_payment', '`user_id`(40), `payment_status`');
CALL add_perf_index('recharge_records', 'idx_perf_rr_paystat_created', '`payment_status`, `created_at`');
CALL add_perf_index('recharge_records', 'idx_perf_rr_country', '`country`');

-- ---------------------------------------------------------------------------
-- 5. recharge_plans（该表已在 schema.sql 中 DROP，由 recharge_plan_groups / recharge_plan_group_plans 替代，无需建索引）
-- ---------------------------------------------------------------------------
-- 以下 CALL 已由存储过程幂等跳过，不会报错，保留仅供参考
-- CALL add_perf_index('recharge_plans', 'idx_perf_rp_created_at', '`created_at`');
-- CALL add_perf_index('recharge_plans', 'idx_perf_rp_name', '`name`(100)');

-- ---------------------------------------------------------------------------
-- 6. promotion_links（已有 uk_pl_promote_id、idx_pl_drama、idx_pl_plan_group）
-- ---------------------------------------------------------------------------
CALL add_perf_index('promotion_links', 'idx_perf_pl_status', '`status`');
CALL add_perf_index('promotion_links', 'idx_perf_pl_created_at', '`created_at`');
CALL add_perf_index('promotion_links', 'idx_perf_pl_status_created', '`status`, `created_at`');
CALL add_perf_index('promotion_links', 'idx_perf_pl_country', '`country`');

-- ---------------------------------------------------------------------------
-- 7. ad_accounts（已有 idx_aa_media、idx_aa_country；无 platform/status 列）
-- ---------------------------------------------------------------------------
CALL add_perf_index('ad_accounts', 'idx_perf_aa_account_name', '`account_name`(100)');
CALL add_perf_index('ad_accounts', 'idx_perf_aa_account_id', '`account_id`');
CALL add_perf_index('ad_accounts', 'idx_perf_aa_created_at', '`created_at`');
CALL add_perf_index('ad_accounts', 'idx_perf_aa_media_country', '`media`, `country`');

-- ---------------------------------------------------------------------------
-- 8. ad_materials（已有 idx_am_account；列为 type，无 material_type/drama_id/status）
-- ---------------------------------------------------------------------------
CALL add_perf_index('ad_materials', 'idx_perf_am_type', '`type`');
CALL add_perf_index('ad_materials', 'idx_perf_am_created_at', '`created_at`');
CALL add_perf_index('ad_materials', 'idx_perf_am_type_created', '`type`, `created_at`');
CALL add_perf_index('ad_materials', 'idx_perf_am_material_name', '`material_name`(100)');

-- ---------------------------------------------------------------------------
-- 9. callback_configs（已有 idx_cc_link_id、idx_cc_platform；无 promotion_link_id/status）
-- ---------------------------------------------------------------------------
CALL add_perf_index('callback_configs', 'idx_perf_cc_created_at', '`created_at`');
CALL add_perf_index('callback_configs', 'idx_perf_cc_link_id_platform', '`link_id`(80), `platform`');

-- ---------------------------------------------------------------------------
-- 10. ad_tasks（已有 idx_ad_tasks_created；无 account_id/drama_id/start_time）
-- ---------------------------------------------------------------------------
CALL add_perf_index('ad_tasks', 'idx_perf_at_status', '`status`');
CALL add_perf_index('ad_tasks', 'idx_perf_at_status_created', '`status`, `created_at`');
CALL add_perf_index('ad_tasks', 'idx_perf_at_promotion_type', '`promotion_type`');

DROP PROCEDURE IF EXISTS add_perf_index;

-- ---------------------------------------------------------------------------
-- 索引数量统计（按表汇总）
-- ---------------------------------------------------------------------------
SELECT
  TABLE_NAME AS table_name,
  COUNT(DISTINCT INDEX_NAME) AS index_count
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
GROUP BY TABLE_NAME
ORDER BY TABLE_NAME;
