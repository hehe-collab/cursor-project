-- =============================================================================
-- 性能索引清理脚本（针对 performance_indexes.sql 和 performance_indexes_v2.sql 的遗留问题）
-- 执行方式：mysql -u root -p drama_system < performance_indexes_cleanup.sql
-- 说明：使用存储过程实现幂等删除，重复执行不会报错
-- =============================================================================

SET NAMES utf8mb4;

-- ================================================================
-- 辅助存储过程：安全删除索引（如果存在则删除，不存在则跳过）
-- ================================================================
DROP PROCEDURE IF EXISTS safe_drop_index;

DELIMITER $$

CREATE PROCEDURE safe_drop_index(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
BEGIN
  DECLARE idx_exists INT DEFAULT 0;

  SELECT COUNT(*) INTO idx_exists
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = p_table
    AND index_name = p_index
  LIMIT 1;

  IF idx_exists > 0 THEN
    SET @s = CONCAT('DROP INDEX `', p_index, '` ON `', p_table, '`');
    PREPARE stmt FROM @s;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    SELECT CONCAT('[CLEANUP] Dropped: ', p_index, ' ON ', p_table) AS result;
  ELSE
    SELECT CONCAT('[CLEANUP] Not found (skip): ', p_index, ' ON ', p_table) AS result;
  END IF;
END$$

DELIMITER ;

-- ================================================================
-- 清理一：recharge_records 表 - 删除冗余/重复索引
-- 原因：
--   - idx_perf_rr_order_no    → v2 已建普通 idx_recharge_order_no，保留 v2 版本
--   - idx_perf_rr_payment_status → 被 idx_recharge_status_created 覆盖，删除
--   - idx_perf_rr_created_at   → 被 idx_recharge_user_status_created 覆盖，删除
--   - idx_perf_rr_user_payment → 被 idx_recharge_user_status_created 覆盖，删除
--   - idx_perf_rr_paystat_created → 与 idx_recharge_status_created 完全重复，删除
--   - idx_perf_rr_country      → recharge_records 无 country 列（schema 无此字段），无效
-- ================================================================
CALL safe_drop_index('recharge_records', 'idx_perf_rr_order_no');
CALL safe_drop_index('recharge_records', 'idx_perf_rr_payment_status');
CALL safe_drop_index('recharge_records', 'idx_perf_rr_created_at');
CALL safe_drop_index('recharge_records', 'idx_perf_rr_user_payment');
CALL safe_drop_index('recharge_records', 'idx_perf_rr_paystat_created');
CALL safe_drop_index('recharge_records', 'idx_perf_rr_country');

-- ================================================================
-- 清理二：users 表 - 删除冗余单列索引（已被复合索引覆盖）
-- 原因：
--   - idx_perf_users_country  → 被 idx_users_country_created 覆盖，删除
--   - idx_perf_users_status    → 被 idx_users_status_created 覆盖，删除
--   - idx_perf_users_created_at → 被 idx_users_promote_created / idx_users_status_created 覆盖，删除
--   - idx_perf_users_promote_status → 被 idx_users_promote_created 部分覆盖，保留（查询 promote_id+status 仍有效）
--   - idx_perf_users_country_created → 与 idx_users_country_created 完全重复，删除
-- ================================================================
CALL safe_drop_index('users', 'idx_perf_users_country');
CALL safe_drop_index('users', 'idx_perf_users_status');
CALL safe_drop_index('users', 'idx_perf_users_created_at');
CALL safe_drop_index('users', 'idx_perf_users_country_created');

-- ================================================================
-- 清理三：dramas 表 - 删除冗余单列索引（已被复合索引覆盖）
-- 原因：
--   - idx_perf_dramas_created_at → 被 idx_dramas_category_status_sort / idx_dramas_online_updated 覆盖，删除
--   - idx_perf_dramas_status      → 被 idx_dramas_category_status_sort 覆盖，删除
-- 保留：idx_perf_dramas_title（前缀索引，v2 无此名称，v1 独立保留）
-- 保留：idx_perf_dramas_status_created（status+created_at，v2 无此列组合）
-- ================================================================
CALL safe_drop_index('dramas', 'idx_perf_dramas_created_at');
CALL safe_drop_index('dramas', 'idx_perf_dramas_status');

-- ================================================================
-- 清理四：recharge_plans 表 - 清理针对不存在表的无效索引调用
-- 注意：该表已在 schema.sql 中 DROP，存储过程幂等跳过，此处无索引可删
--       仅在此处留注释说明：无操作
-- ================================================================
-- recharge_plans 表已不存在，performance_indexes.sql 第71-72行 CALL 不报错但无效，无需处理

-- ================================================================
-- 清理五：admin_logs 表 - 删除 v1 中的旧索引（如果存在）
-- v1 中未建 admin_logs 索引，无可清理项，仅留注释
-- ================================================================

-- ================================================================
-- 清理六：删除存储过程
-- ================================================================
DROP PROCEDURE IF EXISTS safe_drop_index;

-- ================================================================
-- 验证：查看清理后 recharge_records / users / dramas 表的索引
-- ================================================================
SELECT
    TABLE_NAME      AS '表名',
    INDEX_NAME      AS '索引名',
    GROUP_CONCAT(CONCAT(COLUMN_NAME, IF(NON_UNIQUE = 0, '[UNIQUE]', '')) ORDER BY SEQ_IN_INDEX) AS '索引字段',
    IF(NON_UNIQUE = 0, 'UNIQUE', 'NORMAL') AS '类型'
FROM information_schema.statistics
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('recharge_records', 'users', 'dramas')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ================================================================
-- 执行完毕
-- ================================================================
