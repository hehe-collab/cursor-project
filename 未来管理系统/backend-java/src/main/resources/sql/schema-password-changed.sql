-- 为 admins 表添加 password_changed 字段（首次登录强制修改密码）
-- 0 = 未修改初始密码，1 = 已修改
ALTER TABLE admins
  ADD COLUMN IF NOT EXISTS password_changed TINYINT(1) NOT NULL DEFAULT 0
  COMMENT '是否已修改初始密码：0=未改，1=已改';
