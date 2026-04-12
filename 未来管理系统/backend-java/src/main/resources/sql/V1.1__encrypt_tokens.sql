-- ============================================================
-- V1.1__encrypt_tokens.sql
-- 敏感信息加密迁移脚本
-- 执行方式: mysql -u root -p drama_system < src/main/resources/sql/V1.1__encrypt_tokens.sql
-- ============================================================

-- 1. 添加加密字段
ALTER TABLE ad_accounts
    ADD COLUMN access_token_encrypted TEXT COMMENT '访问令牌（加密，AES-256）' AFTER account_agent,
    ADD COLUMN refresh_token_encrypted TEXT COMMENT '刷新令牌（加密，AES-256）' AFTER access_token_encrypted;

-- 2. 为防止迁移过程出错，先备份现有数据（可选，建议生产环境执行）
-- CREATE TABLE ad_accounts_backup AS SELECT * FROM ad_accounts;

-- 3. 创建索引以提升查询性能
CREATE INDEX idx_ad_accounts_token_encrypted ON ad_accounts(access_token_encrypted(64));

-- 4. 迁移完成后（确认加密数据正常后可删除明文字段）
-- 警告：生产环境删除列前请务必确保应用层已完全切换到加密字段
-- ALTER TABLE ad_accounts DROP COLUMN access_token;
-- ALTER TABLE ad_accounts DROP COLUMN refresh_token;
