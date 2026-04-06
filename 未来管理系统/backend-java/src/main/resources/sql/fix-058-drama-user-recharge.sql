-- #058 增量：用户展示 ID、短剧业务 ID、充值账户/回传字段
-- 在目标库执行一次；若列已存在会报错，可手工跳过对应语句。

ALTER TABLE users
  ADD COLUMN user_code VARCHAR(16) NULL COMMENT '8位展示用用户编码' AFTER id;

UPDATE users SET user_code = LPAD(id, 8, '0') WHERE user_code IS NULL OR user_code = '';

ALTER TABLE users ADD UNIQUE KEY uk_users_user_code (user_code);

ALTER TABLE dramas
  ADD COLUMN public_id VARCHAR(32) NULL COMMENT '15位业务剧ID' AFTER id;

UPDATE dramas SET public_id = LPAD(id, 15, '0') WHERE public_id IS NULL OR public_id = '';

ALTER TABLE dramas ADD UNIQUE KEY uk_dramas_public_id (public_id);

ALTER TABLE recharge_records
  ADD COLUMN ad_account_id VARCHAR(96) NULL COMMENT '广告账户ID' AFTER platform,
  ADD COLUMN ad_account_name VARCHAR(255) NULL COMMENT '广告账户名称' AFTER ad_account_id,
  ADD COLUMN callback_sent TINYINT(1) NULL DEFAULT 0 COMMENT '是否已回传' AFTER ad_account_name;
