-- H5 匿名设备用户：users 增加 device_id，用于无登录场景下的设备建档
ALTER TABLE users
  ADD COLUMN device_id VARCHAR(128) NULL COMMENT 'H5 设备唯一标识' AFTER token,
  ADD UNIQUE KEY uk_users_device_id (device_id);
