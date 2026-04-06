-- #058 回传配置：复充回传开关 + 策略 JSON（与协作文档 §13 任务 8 对齐）
-- 在库 drama_system 执行一次；若列已存在会报错，请跳过对应语句。

ALTER TABLE callback_configs
  ADD COLUMN replenish_callback_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '复充回传：1开启 0关闭' AFTER min_price_limit,
  ADD COLUMN config_json TEXT NULL COMMENT '策略配置JSON（如 strategies 金额区间+传参）' AFTER replenish_callback_enabled;
