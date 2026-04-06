-- #062 回传配置：幂等补齐 callback_configs 缺失列（replenish_callback_enabled、config_json）
-- 用法：mysql -u root -p drama_system < backend-java/src/main/resources/sql/fix-062-callback-config-columns.sql
-- 说明：与 fix-058 / fix-061 字段语义一致；已存在列则跳过对应 ALTER，适合反复执行或 CI。
-- 执行前请确认已 USE 目标库（命令行管道指定库名即可）。

SET @schema = DATABASE();

-- replenish_callback_enabled（与 schema.sql 一致：NOT NULL DEFAULT 1）
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'callback_configs' AND COLUMN_NAME = 'replenish_callback_enabled') > 0,
  'SELECT ''skip: replenish_callback_enabled already exists'' AS migration_note',
  CONCAT(
    'ALTER TABLE callback_configs ADD COLUMN replenish_callback_enabled ',
    'TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''复充回传：1开启 0关闭'' AFTER min_price_limit'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- config_json
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'callback_configs' AND COLUMN_NAME = 'config_json') > 0,
  'SELECT ''skip: config_json already exists'' AS migration_note',
  CONCAT(
    'ALTER TABLE callback_configs ADD COLUMN config_json ',
    'TEXT NULL COMMENT ''策略配置JSON（如 strategies 金额区间+传参）'' AFTER replenish_callback_enabled'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DESCRIBE callback_configs;
