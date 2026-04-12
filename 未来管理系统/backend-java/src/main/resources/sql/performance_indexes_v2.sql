-- ================================================================
-- 性能优化索引脚本（幂等版）
-- 文件位置：未来管理系统/backend-java/src/main/resources/sql/performance_indexes_v2.sql
-- 执行方式：mysql -u root -p drama_system < performance_indexes_v2.sql
-- 说明：所有索引均使用存储过程判断是否已存在，重复执行不会报错
-- ================================================================

USE drama_system;

-- ================================================================
-- 辅助存储过程：安全创建索引（如果不存在则创建，已存在则跳过）
-- ================================================================
DROP PROCEDURE IF EXISTS safe_create_index;

DELIMITER //

CREATE PROCEDURE safe_create_index(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_def VARCHAR(512)
)
BEGIN
    DECLARE idx_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO idx_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name;

    IF idx_exists = 0 THEN
        SET @sql = CONCAT('CREATE INDEX ', p_index_name, ' ON ', p_table_name, ' ', p_index_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('✓ Created: ', p_index_name, ' ON ', p_table_name) AS result;
    ELSE
        SELECT CONCAT('- Already exists: ', p_index_name, ' ON ', p_table_name) AS result;
    END IF;
END //

DELIMITER ;

-- ================================================================
-- 1. 用户表 (users) 索引优化
-- ================================================================

-- 复合索引：推广ID + 创建时间
CALL safe_create_index('users', 'idx_users_promote_created', '(promote_id, created_at DESC)');

-- 复合索引：国家 + 创建时间
CALL safe_create_index('users', 'idx_users_country_created', '(country, created_at DESC)');

-- 复合索引：Token 模糊搜索
CALL safe_create_index('users', 'idx_users_token', '(token(64))');

-- 复合索引：用户名搜索
CALL safe_create_index('users', 'idx_users_username', '(username)');

-- 复合索引：手机号
CALL safe_create_index('users', 'idx_users_phone', '(phone)');

-- 覆盖索引：用户列表常用字段
CALL safe_create_index('users', 'idx_users_list_cover', '(user_code, promote_id, country, created_at, status)');

-- 复合索引：状态 + 创建时间
CALL safe_create_index('users', 'idx_users_status_created', '(status, created_at DESC)');

-- ================================================================
-- 2. 充值记录表 (recharge_records) 索引优化
-- ================================================================

-- 复合索引：用户ID + 支付状态 + 创建时间
CALL safe_create_index('recharge_records', 'idx_recharge_user_status_created', '(user_id, payment_status, created_at DESC)');

-- 复合索引：推广ID + 支付状态 + 创建时间
CALL safe_create_index('recharge_records', 'idx_recharge_promote_status_created', '(promote_id, payment_status, created_at DESC)');

-- 复合索引：支付状态 + 创建时间
CALL safe_create_index('recharge_records', 'idx_recharge_status_created', '(payment_status, created_at DESC)');

-- 唯一索引：订单号
-- 注意：唯一索引需要特殊处理
DROP PROCEDURE IF EXISTS safe_create_unique_index;

DELIMITER //

CREATE PROCEDURE safe_create_unique_index(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_def VARCHAR(512)
)
BEGIN
    DECLARE idx_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO idx_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name;

    IF idx_exists = 0 THEN
        SET @sql = CONCAT('CREATE UNIQUE INDEX ', p_index_name, ' ON ', p_table_name, ' ', p_index_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('✓ Created UNIQUE: ', p_index_name, ' ON ', p_table_name) AS result;
    ELSE
        SELECT CONCAT('- Already exists: ', p_index_name, ' ON ', p_table_name) AS result;
    END IF;
END //

DELIMITER ;

-- 充值记录 - 唯一索引：订单号
CALL safe_create_unique_index('recharge_records', 'idx_recharge_order_no', '(order_no)');

-- ================================================================
-- 3. 短剧表 (dramas) 索引优化
-- ================================================================

-- 复合索引：分类 + 状态 + 排序
CALL safe_create_index('dramas', 'idx_dramas_category_status_sort', '(category_id, status, sort DESC)');

-- 复合索引：分类 + 状态 + 更新时间
CALL safe_create_index('dramas', 'idx_dramas_category_status_updated', '(category_id, is_online, updated_at DESC)');

-- 复合索引：业务剧ID
CALL safe_create_index('dramas', 'idx_dramas_public_id', '(public_id)');

-- 复合索引：是否上线 + 更新时间
CALL safe_create_index('dramas', 'idx_dramas_online_updated', '(is_online, updated_at DESC)');

-- 全文索引：标题搜索
-- 注意：全文索引需要单独处理
DROP PROCEDURE IF EXISTS safe_create_fulltext_index;

DELIMITER //

CREATE PROCEDURE safe_create_fulltext_index(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_columns VARCHAR(512)
)
BEGIN
    DECLARE idx_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO idx_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name;

    IF idx_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' ADD FULLTEXT INDEX ', p_index_name, '(', p_columns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('✓ Created FULLTEXT: ', p_index_name, ' ON ', p_table_name) AS result;
    ELSE
        SELECT CONCAT('- Already exists: ', p_index_name, ' ON ', p_table_name) AS result;
    END IF;
END //

DELIMITER ;

-- 短剧 - 全文索引：标题搜索
CALL safe_create_fulltext_index('dramas', 'idx_dramas_title_fulltext', 'title');

-- ================================================================
-- 4. 推广明细汇总表 (promotion_details_summary) 索引优化
-- ================================================================

-- 复合索引：日期 + 推广ID + 平台（字段为 promotion_id）
CALL safe_create_index('promotion_details_summary', 'idx_promotion_summary_date_promote_platform', '(date, promotion_id, platform)');

-- 复合索引：推广ID + 日期
CALL safe_create_index('promotion_details_summary', 'idx_promotion_summary_promote_date', '(promotion_id, date DESC)');

-- ================================================================
-- 5. 分类表 (categories) 索引优化
-- ================================================================
-- 注意：categories 表只有 id, name, sort, created_at, updated_at，无 status 字段
-- 只建一个排序索引
CALL safe_create_index('categories', 'idx_categories_sort', '(sort DESC)');

-- ================================================================
-- 6. 标签表 (tags) 索引优化
-- ================================================================
-- 注意：tags 表只有 id, name, created_at, updated_at，无 status 字段
-- 已有 UNIQUE KEY `uk_tags_name` (name)，无需额外索引

-- ================================================================
-- 7. 短剧-标签关联表 (drama_tags) 索引优化
-- ================================================================

-- 唯一索引：短剧ID + 标签ID
CALL safe_create_unique_index('drama_tags', 'idx_drama_tags_drama_tag', '(drama_id, tag_id)');

-- 复合索引：标签ID + 短剧ID
CALL safe_create_index('drama_tags', 'idx_drama_tags_tag_drama', '(tag_id, drama_id)');

-- ================================================================
-- 8. 短剧分集表 (drama_episodes) 索引优化
-- ================================================================

CALL safe_create_index('drama_episodes', 'idx_episodes_drama_num', '(drama_id, episode_num)');

-- ================================================================
-- 9. 用户每日统计表 (user_daily_stats) 索引优化
-- ================================================================
-- 注意：字段为 user_pk, stat_date（不是 user_id, date）
CALL safe_create_index('user_daily_stats', 'idx_user_daily_date_user', '(stat_date DESC, user_pk)');
CALL safe_create_index('user_daily_stats', 'idx_user_daily_user_date', '(user_pk, stat_date DESC)');

-- ================================================================
-- 10. 短剧每日统计表 (drama_daily_stats) 索引优化
-- ================================================================
-- 注意：字段为 drama_id, stat_date（不是 drama_id, date）
CALL safe_create_index('drama_daily_stats', 'idx_drama_daily_date_drama', '(stat_date DESC, drama_id)');

-- ================================================================
-- 11. 操作日志表 (admin_logs) 索引优化
-- ================================================================
-- 注意：admin_logs 表已有索引 idx_admin_id 和 idx_operation_type，此处仅补充复合索引
CALL safe_create_index('admin_logs', 'idx_admin_logs_admin_created', '(admin_id, created_at DESC)');
CALL safe_create_index('admin_logs', 'idx_admin_logs_type_created', '(operation_type, created_at DESC)');

-- ================================================================
-- 清理存储过程
-- ================================================================
DROP PROCEDURE IF EXISTS safe_create_index;
DROP PROCEDURE IF EXISTS safe_create_unique_index;
DROP PROCEDURE IF EXISTS safe_create_fulltext_index;

-- ================================================================
-- 验证：查看所有新建的索引
-- ================================================================
SELECT
    TABLE_NAME AS '表名',
    INDEX_NAME AS '索引名',
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS '索引字段',
    IF(NON_UNIQUE = 0, 'UNIQUE', 'NORMAL') AS '类型'
FROM information_schema.statistics
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'users', 'recharge_records', 'dramas', 'promotion_details_summary',
      'categories', 'tags', 'drama_tags', 'drama_episodes',
      'user_daily_stats', 'drama_daily_stats', 'admin_logs'
  )
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ================================================================
-- 执行完毕
-- ================================================================
