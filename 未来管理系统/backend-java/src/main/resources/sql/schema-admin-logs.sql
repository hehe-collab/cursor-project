-- ============================================
-- 管理员操作日志
-- 使用前：mysql -u root -p drama_system < schema-admin-logs.sql
-- ============================================

CREATE TABLE IF NOT EXISTS admin_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    admin_id INT NOT NULL COMMENT '操作人ID',
    admin_username VARCHAR(50) NOT NULL COMMENT '操作人用户名',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型（CREATE/UPDATE/DELETE/LOGIN/LOGOUT等）',
    target_type VARCHAR(50) COMMENT '目标对象类型（drama/user/role/admin等）',
    target_id VARCHAR(100) COMMENT '目标对象ID',
    operation_desc VARCHAR(255) COMMENT '操作描述',
    request_method VARCHAR(10) COMMENT '请求方法（GET/POST/PUT/DELETE）',
    request_url VARCHAR(500) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数（JSON）',
    response_status INT COMMENT '响应状态码',
    error_msg TEXT COMMENT '错误信息（如果失败）',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    execution_time INT COMMENT '执行耗时（毫秒）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_admin_id (admin_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_target_type (target_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';
