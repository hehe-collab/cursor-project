-- ============================================
-- RBAC 权限控制系统
-- 使用前：mysql -u root -p drama_system < schema-rbac.sql
-- ============================================

-- ------------------ 角色表 ------------------
CREATE TABLE IF NOT EXISTS roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色代码',
    description VARCHAR(200) COMMENT '角色描述',
    is_system TINYINT(1) DEFAULT 0 COMMENT '是否系统内置 1=内置不可删',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ------------------ 权限表 ------------------
CREATE TABLE IF NOT EXISTS permissions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '权限名称',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限代码 drama:view',
    module VARCHAR(50) COMMENT '所属模块',
    description VARCHAR(200) COMMENT '权限描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_module (module),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ------------------ 角色-权限关联表 ------------------
CREATE TABLE IF NOT EXISTS role_permissions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ------------------ 管理员表扩展（role_id） ------------------
-- admins.role 原有 string 字段保留（兼容旧数据）；新增 role_id 关联 roles 表
-- 注意：MySQL 5.7 / 部分 8.0 不支持 ADD COLUMN IF NOT EXISTS，故用 information_schema 条件执行
SET @dbname = DATABASE();
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'admins' AND COLUMN_NAME = 'role_id') > 0,
  'SELECT 1',
  'ALTER TABLE admins ADD COLUMN role_id INT DEFAULT NULL COMMENT ''关联角色 ID'' AFTER password'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'admins' AND INDEX_NAME = 'idx_role_id') > 0,
  'SELECT 1',
  'ALTER TABLE admins ADD INDEX idx_role_id (role_id)'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------ 插入默认角色 ------------------
INSERT INTO roles (name, code, description, is_system) VALUES
('超级管理员', 'super_admin', '拥有所有权限', 1),
('运营人员', 'operator', '可查看和编辑内容，不能删除', 1),
('财务人员', 'finance', '仅充值和财务相关权限', 1),
('数据分析师', 'analyst', '仅查看权限，不能修改', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description);

-- ------------------ 插入默认权限 ------------------
INSERT INTO permissions (name, code, module, description) VALUES
-- 短剧
('查看短剧', 'drama:view', 'drama', '查看短剧列表和详情'),
('创建短剧', 'drama:create', 'drama', '创建新短剧'),
('编辑短剧', 'drama:edit', 'drama', '编辑短剧信息'),
('删除短剧', 'drama:delete', 'drama', '删除短剧'),
-- 用户
('查看用户', 'user:view', 'user', '查看用户列表和详情'),
('编辑用户', 'user:edit', 'user', '编辑用户信息'),
('删除用户', 'user:delete', 'user', '删除用户'),
('调整余额', 'user:balance', 'user', '调整用户金币余额'),
-- 充值
('查看充值', 'recharge:view', 'recharge', '查看充值记录'),
('审核充值', 'recharge:approve', 'recharge', '审核充值订单'),
('退款', 'recharge:refund', 'recharge', '处理退款'),
('管理方案', 'recharge:plan', 'recharge', '管理充值方案'),
-- 投放
('查看投放', 'promotion:view', 'promotion', '查看投放链接和数据'),
('创建投放', 'promotion:create', 'promotion', '创建投放链接'),
('编辑投放', 'promotion:edit', 'promotion', '编辑投放配置'),
('删除投放', 'promotion:delete', 'promotion', '删除投放链接'),
-- 广告账户
('查看账户', 'account:view', 'account', '查看广告账户'),
('创建账户', 'account:create', 'account', '创建广告账户'),
('编辑账户', 'account:edit', 'account', '编辑账户信息'),
('删除账户', 'account:delete', 'account', '删除账户'),
-- TikTok
('查看广告', 'tiktok:view', 'tiktok', '查看 TikTok 广告数据'),
('创建广告', 'tiktok:create', 'tiktok', '创建 TikTok 广告'),
('编辑广告', 'tiktok:edit', 'tiktok', '编辑 TikTok 广告'),
('删除广告', 'tiktok:delete', 'tiktok', '删除 TikTok 广告'),
('导入广告', 'tiktok:import', 'tiktok', '批量导入广告'),
-- 系统配置
('查看设置', 'system:view', 'system', '查看系统设置'),
('修改设置', 'system:edit', 'system', '修改系统设置'),
('管理分类', 'system:category', 'system', '管理分类标签'),
-- 看板
('查看看板', 'dashboard:view', 'dashboard', '查看数据看板'),
('导出数据', 'dashboard:export', 'dashboard', '导出报表数据'),
-- 管理员
('查看管理员', 'admin:view', 'admin', '查看管理员列表'),
('创建管理员', 'admin:create', 'admin', '创建新管理员'),
('编辑管理员', 'admin:edit', 'admin', '编辑管理员信息'),
('删除管理员', 'admin:delete', 'admin', '删除管理员'),
('分配角色', 'admin:role', 'admin', '为管理员分配角色')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ------------------ 超级管理员：全部权限 ------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'super_admin'), id
FROM permissions
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- ------------------ 运营人员：只读 + 部分编辑 ------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'operator'), id
FROM permissions
WHERE code IN (
    'drama:view','drama:create','drama:edit',
    'user:view','user:edit',
    'recharge:view',
    'promotion:view','promotion:create','promotion:edit',
    'account:view',
    'tiktok:view','tiktok:create','tiktok:edit','tiktok:import',
    'system:view','system:category',
    'dashboard:view'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- ------------------ 财务人员：充值相关 ------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'finance'), id
FROM permissions
WHERE code IN (
    'recharge:view','recharge:approve','recharge:refund','recharge:plan',
    'user:view','user:balance',
    'dashboard:view','dashboard:export'
)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- ------------------ 数据分析师：只读 + 导出 ------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'analyst'), id
FROM permissions
WHERE code LIKE '%:view' OR code = 'dashboard:export'
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- ------------------ 现有 admin 账户设为超级管理员 ------------------
UPDATE admins SET role_id = (SELECT id FROM roles WHERE code = 'super_admin')
WHERE username = 'admin' AND (role_id IS NULL OR role_id = 0);