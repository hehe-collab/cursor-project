-- 演示数据（指令 #003）
-- 依赖：已执行 schema.sql；管理员由 AdminInitializer 创建，本脚本不插入 admins。
-- 可重复执行：先删除同批次演示行再插入。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清理本脚本写入的演示数据（按外键逆序）
DELETE dt FROM drama_tags dt
  INNER JOIN tags t ON dt.tag_id = t.id
  WHERE t.name IN ('热门','推荐','新剧','完结','独家','高分','经典','热播','必看','精选');
DELETE FROM drama_tags WHERE drama_id BETWEEN 501 AND 505 OR tag_id BETWEEN 401 AND 410;
-- FOREIGN_KEY_CHECKS=0 时删除 dramas 不会级联删子表，剧集行会残留导致重复导入报 uk_drama_episode
DELETE FROM drama_episodes WHERE drama_id BETWEEN 501 AND 505;
DELETE FROM dramas WHERE id BETWEEN 501 AND 505;
DELETE FROM tags WHERE id BETWEEN 401 AND 410
   OR name IN ('热门','推荐','新剧','完结','独家','高分','经典','热播','必看','精选');
DELETE FROM categories WHERE id BETWEEN 301 AND 305
   OR name IN ('爱情','悬疑','喜剧','动作','科幻');
DELETE FROM recharge_records WHERE id BETWEEN 910001 AND 910020;
DELETE FROM users WHERE username LIKE 'demo_user_%';

SET FOREIGN_KEY_CHECKS = 1;

-- 1.2 用户（10 条，固定 id 101–110，便于充值记录 user_id 对齐）
INSERT INTO users (id, username, country, coin_balance, promote_id, token, created_at) VALUES
(101, 'demo_user_1', 'US', 100, 'P001', 'tok_demo_1', NOW()),
(102, 'demo_user_2', 'UK', 200, 'P001', 'tok_demo_2', NOW()),
(103, 'demo_user_3', 'JP', 150, 'P002', 'tok_demo_3', NOW()),
(104, 'demo_user_4', 'KR', 300, 'P002', 'tok_demo_4', NOW()),
(105, 'demo_user_5', 'US', 50, 'P003', 'tok_demo_5', NOW()),
(106, 'demo_user_6', 'UK', 400, 'P003', 'tok_demo_6', NOW()),
(107, 'demo_user_7', 'JP', 250, 'P001', 'tok_demo_7', NOW()),
(108, 'demo_user_8', 'KR', 180, 'P002', 'tok_demo_8', NOW()),
(109, 'demo_user_9', 'US', 90, 'P003', 'tok_demo_9', NOW()),
(110, 'demo_user_10', 'UK', 500, 'P001', 'tok_demo_10', NOW());

-- 1.4 分类（5 条，固定 id 301–305，避免与已有数据冲突）
INSERT INTO categories (id, name, sort, created_at) VALUES
(301, '爱情', 1, NOW()),
(302, '悬疑', 2, NOW()),
(303, '喜剧', 3, NOW()),
(304, '动作', 4, NOW()),
(305, '科幻', 5, NOW());

-- 1.5 标签（10 条，固定 id 401–410）
INSERT INTO tags (id, name, created_at) VALUES
(401, '热门', NOW()),
(402, '推荐', NOW()),
(403, '新剧', NOW()),
(404, '完结', NOW()),
(405, '独家', NOW()),
(406, '高分', NOW()),
(407, '经典', NOW()),
(408, '热播', NOW()),
(409, '必看', NOW()),
(410, '精选', NOW());

-- 1.6 短剧（5 条，固定 id 501–505，避免重复导入时自增错位）
INSERT INTO dramas (id, title, description, cover, category_id, status, total_episodes, view_count, sort, is_online, created_at) VALUES
(501, 'DEMO_霸道总裁爱上我', '一个灰姑娘与霸道总裁的爱情故事', 'https://example.com/cover1.jpg', 301, 'published', 30, 10000, 1, 1, NOW()),
(502, 'DEMO_悬疑小镇', '一个小镇发生的离奇案件', 'https://example.com/cover2.jpg', 302, 'published', 24, 8000, 2, 1, NOW()),
(503, 'DEMO_爆笑日常', '搞笑的日常生活故事', 'https://example.com/cover3.jpg', 303, 'published', 20, 12000, 3, 1, NOW()),
(504, 'DEMO_特工任务', '惊险刺激的特工故事', 'https://example.com/cover4.jpg', 304, 'published', 28, 9000, 4, 1, NOW()),
(505, 'DEMO_未来世界', '科幻题材的未来故事', 'https://example.com/cover5.jpg', 305, 'draft', 32, 0, 5, 0, NOW());

INSERT INTO drama_episodes (drama_id, episode_num, title, video_url, duration) VALUES
(501, 1, '第1集', 'https://example.com/video501_ep1.mp4', 300),
(501, 2, '第2集', 'https://example.com/video501_ep2.mp4', 280);

INSERT INTO drama_tags (drama_id, tag_id) VALUES
(501, 401), (501, 402),
(502, 403), (502, 404),
(503, 405), (503, 406),
(504, 407), (504, 408),
(505, 409), (505, 410);

-- 1.3 充值记录（20 条；id 主键；order_no；user_id 为字符串，对应 users.id）
-- total_count=20；pending_count=3（与 RechargeRecordMapper.countPendingApprox 一致：payment_status=pending 或 pay_status 为 pending/unpaid）
INSERT INTO recharge_records (
  id, order_no, user_id, drama_id, drama_name, amount, coins,
  payment_status, pay_status, promotion_id, promote_id,
  country, platform, local_register_time, local_order_time, created_at
) VALUES
(910001, 'DEMO_ORD001', '101', 501, 'DEMO_霸道总裁爱上我', 9.99, 100, 'paid', 'success', 'P001', 'P001', 'US', 'TikTok', NOW(), NOW(), NOW()),
(910002, 'DEMO_ORD002', '102', 502, 'DEMO_悬疑小镇', 19.99, 200, 'paid', 'success', 'P001', 'P001', 'UK', 'TikTok', NOW(), NOW(), NOW()),
(910003, 'DEMO_ORD003', '103', 503, 'DEMO_爆笑日常', 14.99, 150, 'paid', 'success', 'P002', 'P002', 'JP', 'Facebook', NOW(), NOW(), NOW()),
(910004, 'DEMO_ORD004', '104', 504, 'DEMO_特工任务', 29.99, 300, 'paid', 'success', 'P002', 'P002', 'KR', 'Facebook', NOW(), NOW(), NOW()),
(910005, 'DEMO_ORD005', '105', 505, 'DEMO_未来世界', 4.99, 50, 'pending', 'pending', 'P003', 'P003', 'US', 'TikTok', NOW(), NOW(), NOW()),
(910006, 'DEMO_ORD006', '106', 501, 'DEMO_霸道总裁爱上我', 39.99, 400, 'paid', 'success', 'P003', 'P003', 'UK', 'TikTok', NOW(), NOW(), NOW()),
(910007, 'DEMO_ORD007', '107', 502, 'DEMO_悬疑小镇', 24.99, 250, 'paid', 'success', 'P001', 'P001', 'JP', 'TikTok', NOW(), NOW(), NOW()),
(910008, 'DEMO_ORD008', '108', 503, 'DEMO_爆笑日常', 17.99, 180, 'paid', 'success', 'P002', 'P002', 'KR', 'Facebook', NOW(), NOW(), NOW()),
(910009, 'DEMO_ORD009', '109', 504, 'DEMO_特工任务', 8.99, 90, 'pending', 'pending', 'P003', 'P003', 'US', 'TikTok', NOW(), NOW(), NOW()),
(910010, 'DEMO_ORD010', '110', 505, 'DEMO_未来世界', 49.99, 500, 'paid', 'success', 'P001', 'P001', 'UK', 'Facebook', NOW(), NOW(), NOW()),
(910011, 'DEMO_ORD011', '101', 501, 'DEMO_霸道总裁爱上我', 9.99, 100, 'paid', 'success', 'P001', 'P001', 'US', 'TikTok', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(910012, 'DEMO_ORD012', '102', 502, 'DEMO_悬疑小镇', 19.99, 200, 'paid', 'success', 'P001', 'P001', 'UK', 'TikTok', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(910013, 'DEMO_ORD013', '103', 503, 'DEMO_爆笑日常', 14.99, 150, 'failed', 'failed', 'P002', 'P002', 'JP', 'Facebook', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(910014, 'DEMO_ORD014', '104', 504, 'DEMO_特工任务', 29.99, 300, 'paid', 'success', 'P002', 'P002', 'KR', 'TikTok', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(910015, 'DEMO_ORD015', '105', 505, 'DEMO_未来世界', 4.99, 50, 'paid', 'success', 'P003', 'P003', 'US', 'Facebook', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(910016, 'DEMO_ORD016', '106', 501, 'DEMO_霸道总裁爱上我', 39.99, 400, 'paid', 'success', 'P003', 'P003', 'UK', 'TikTok', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(910017, 'DEMO_ORD017', '107', 502, 'DEMO_悬疑小镇', 24.99, 250, 'pending', 'unpaid', 'P001', 'P001', 'JP', 'Facebook', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(910018, 'DEMO_ORD018', '108', 503, 'DEMO_爆笑日常', 17.99, 180, 'paid', 'success', 'P002', 'P002', 'KR', 'TikTok', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(910019, 'DEMO_ORD019', '109', 504, 'DEMO_特工任务', 8.99, 90, 'paid', 'success', 'P003', 'P003', 'US', 'Facebook', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
(910020, 'DEMO_ORD020', '110', 505, 'DEMO_未来世界', 49.99, 500, 'paid', 'success', 'P001', 'P001', 'UK', 'TikTok', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY));
