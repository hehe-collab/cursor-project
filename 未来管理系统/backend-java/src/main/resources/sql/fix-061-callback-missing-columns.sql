-- #061 回传配置：补齐缺失列（解决 Unknown column 'replenish_callback_enabled'）
-- 在库 drama_system 执行；若某列已存在会报 Duplicate column，跳过该行即可。
--
-- 与 fix-058-callback-config-strategy.sql 等价拆分，便于只补漏列的环境。

ALTER TABLE callback_configs
  ADD COLUMN replenish_callback_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '复充回传：1开启 0关闭' AFTER min_price_limit;

ALTER TABLE callback_configs
  ADD COLUMN config_json TEXT NULL COMMENT '策略配置JSON' AFTER replenish_callback_enabled;
