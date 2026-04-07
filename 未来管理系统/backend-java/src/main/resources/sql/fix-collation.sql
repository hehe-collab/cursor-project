-- #087：统一业务表排序规则为 utf8mb4_unicode_ci（可重复执行）
-- 现象：recharge_records JOIN users 时 r.user_id 与 CAST(u.id AS CHAR) / user_code 排序规则不一致
--       报错 Illegal mix of collations (utf8mb4_unicode_ci,IMPLICIT) and (utf8mb4_0900_a_ci,IMPLICIT)
-- 用法：mysql -u root -p drama_system < src/main/resources/sql/fix-collation.sql

SET NAMES utf8mb4;

ALTER DATABASE `drama_system` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `recharge_records` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `users` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 以下表若不存在请注释掉对应行（老库仅部分扩展表时）
ALTER TABLE `promotion_details_summary` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `ad_accounts` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `promotion_links` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `tiktok_cost_records` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `recharge_orders` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
