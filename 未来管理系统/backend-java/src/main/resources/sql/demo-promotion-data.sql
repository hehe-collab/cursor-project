-- 推广明细演示数据（#078 / #084 / #085）
-- 依赖：schema.sql + demo_data.sql（用户 101～110）+ schema-promotion.sql（或 schema-promotion-update.sql）
-- 可重复执行
--
-- 含：多日 history + **当日（2026-04-08）** 印尼/泰国/美国 × TikTok/Facebook/Google，便于默认「今天 + country=ID」列表有数据。

SET NAMES utf8mb4;

DELETE FROM `promotion_details_summary`
WHERE `promotion_id` IN ('id:79446', 'id:79447', 'id:79448', 'id:79449');
DELETE FROM `tiktok_cost_records`
WHERE `promotion_id` = 'id:79446'
  AND `record_time` IN ('2026-04-07 10:00:00', '2026-04-07 10:10:00');
DELETE FROM `recharge_orders`
WHERE `order_id` IN ('DEMO_PROMO_ORD001', 'DEMO_PROMO_ORD002', 'DEMO_PROMO_ORD003');

INSERT INTO `recharge_orders` (`order_id`, `user_id`, `promotion_id`, `amount`, `coins`, `is_first_recharge`, `created_at`) VALUES
('DEMO_PROMO_ORD001', 101, 'id:79446', 10.00, 100, 1, '2026-04-07 10:00:00'),
('DEMO_PROMO_ORD002', 102, 'id:79446', 20.00, 200, 1, '2026-04-07 11:00:00'),
('DEMO_PROMO_ORD003', 101, 'id:79446', 15.00, 150, 0, '2026-04-07 12:00:00');

INSERT INTO `tiktok_cost_records` (`promotion_id`, `account_id`, `account_name`, `balance`, `campaign_name`, `cost`, `impressions`, `record_time`) VALUES
('id:79446', '76211337', 'C-GF-xd-49T-EN-02', 59.57, 'id:18616', 26.74, 10000, '2026-04-07 10:00:00'),
('id:79446', '76211337', 'C-GF-xd-49T-EN-02', 59.57, 'id:18616', 28.50, 11000, '2026-04-07 10:10:00');

INSERT INTO `promotion_details_summary` (
  `date`, `promotion_id`, `promotion_name`, `platform`, `country`, `drama_id`, `drama_name`,
  `account_id`, `account_name`, `balance`, `campaign_name`,
  `cost`, `speed`, `roi`, `user_count`, `recharge_amount`, `profit`, `order_count`, `first_recharge_count`,
  `first_recharge_rate`, `repeat_recharge_count`, `impressions`, `cpm`, `avg_recharge_per_user`
) VALUES
('2026-04-07', 'id:79446', 'C-GF-xd-周末不爱情侣', 'tiktok', 'ID',
 NULL, '泰国不爱情侣酒-THAI', '76211337', 'C-GF-xd-49T-EN-02', 59.57, 'id:18616',
 26.74, 1.84, 0.3300, 39, 26.74, -32.83, 175, 39, 0.2200, 0, 11000, 4.5000, 0.6900),
('2026-04-06', 'id:79446', 'C-GF-xd-周末不爱情侣', 'tiktok', 'ID',
 NULL, '泰国不爱情侣酒-THAI', '76211337', 'C-GF-xd-49T-EN-02', 59.57, 'id:18616',
 24.50, 1.50, 0.3500, 35, 25.00, -30.00, 160, 35, 0.2200, 0, 10500, 4.2000, 0.7100),
('2026-04-05', 'id:79446', 'C-GF-xd-周末不爱情侣', 'tiktok', 'ID',
 NULL, '泰国不爱情侣酒-THAI', '76211337', 'C-GF-xd-49T-EN-02', 59.57, 'id:18616',
 22.30, 1.30, 0.3800, 32, 23.50, -28.00, 145, 32, 0.2200, 0, 10000, 4.0000, 0.7300),
('2026-04-07', 'id:79447', 'TH-测试推广', 'facebook', 'TH',
 NULL, '泰国测试剧', '76211338', 'TH-FB-01', 100.00, 'id:18617',
 50.00, 3.00, 0.4000, 50, 60.00, 10.00, 200, 50, 0.2500, 5, 12000, 5.0000, 1.2000),

-- #085：「今天」汇总行（与看板默认日期一致时列表非空）
('2026-04-08', 'id:79446', 'C-GF-xd-周末不爱情侣', 'tiktok', 'ID',
 NULL, '泰国不爱情侣酒-THAI', '76211337', 'C-GF-xd-49T-EN-02', 59.57, 'id:18616',
 30.00, 2.00, 0.3500, 45, 35.00, 5.00, 200, 45, 0.2200, 5, 10000, 4.8000, 0.7800),
('2026-04-08', 'id:79448', 'ID-FB-测试推广', 'facebook', 'ID',
 NULL, '印尼测试剧', '76211339', 'ID-FB-01', 80.00, 'id:18618',
 25.00, 1.80, 0.4000, 40, 30.00, 5.00, 180, 40, 0.2200, 3, 8000, 4.5000, 0.7500),
('2026-04-08', 'id:79447', 'TH-测试推广', 'tiktok', 'TH',
 NULL, '泰国测试剧', '76211338', 'TH-TT-01', 100.00, 'id:18617',
 50.00, 3.00, 0.4000, 50, 60.00, 10.00, 200, 50, 0.2500, 5, 12000, 5.0000, 1.2000),
('2026-04-08', 'id:79449', 'US-Google-推广', 'google', 'US',
 NULL, '美国测试剧', '76211340', 'US-GG-01', 150.00, 'id:18619',
 80.00, 5.00, 0.5000, 60, 100.00, 20.00, 250, 60, 0.2400, 8, 15000, 6.0000, 1.6700);

-- #085：广告账户 media/country（与推广筛选项、账户 ID 对齐；无 UNIQUE(account_id) 时用 UPDATE + 条件 INSERT）
UPDATE `ad_accounts` SET `media` = 'tiktok', `country` = 'ID' WHERE `account_id` = '76211337';
UPDATE `ad_accounts` SET `media` = 'tiktok', `country` = 'TH' WHERE `account_id` = '76211338';
UPDATE `ad_accounts` SET `media` = 'facebook', `country` = 'ID' WHERE `account_id` = '76211339';
UPDATE `ad_accounts` SET `media` = 'google', `country` = 'US' WHERE `account_id` = '76211340';

INSERT INTO `ad_accounts` (`media`, `country`, `subject_name`, `account_id`, `account_name`, `media_alias`, `account_agent`)
SELECT 'tiktok', 'ID', '推广明细演示', '76211337', 'C-GF-xd-49T-EN-02', '', ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ad_accounts` a WHERE a.`account_id` = '76211337');
INSERT INTO `ad_accounts` (`media`, `country`, `subject_name`, `account_id`, `account_name`, `media_alias`, `account_agent`)
SELECT 'tiktok', 'TH', '推广明细演示', '76211338', 'TH-TT-01', '', ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ad_accounts` a WHERE a.`account_id` = '76211338');
INSERT INTO `ad_accounts` (`media`, `country`, `subject_name`, `account_id`, `account_name`, `media_alias`, `account_agent`)
SELECT 'facebook', 'ID', '推广明细演示', '76211339', 'ID-FB-01', '', ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ad_accounts` a WHERE a.`account_id` = '76211339');
INSERT INTO `ad_accounts` (`media`, `country`, `subject_name`, `account_id`, `account_name`, `media_alias`, `account_agent`)
SELECT 'google', 'US', '推广明细演示', '76211340', 'US-GG-01', '', ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `ad_accounts` a WHERE a.`account_id` = '76211340');
