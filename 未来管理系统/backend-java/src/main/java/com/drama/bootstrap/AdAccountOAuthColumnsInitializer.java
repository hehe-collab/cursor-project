package com.drama.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 兼容旧库：补齐 ad_accounts OAuth 相关列。
 *
 * <p>历史库的 ad_accounts 只有基础账户字段，后续代码新增了加密 token 与过期时间列。
 * 若本地库未执行增量 DDL，新增/编辑账户会直接报 Unknown column。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdAccountOAuthColumnsInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn(
                "access_token_encrypted",
                "ALTER TABLE ad_accounts "
                        + "ADD COLUMN access_token_encrypted TEXT NULL COMMENT '加密后的 Access Token' "
                        + "AFTER account_agent");
        ensureColumn(
                "refresh_token_encrypted",
                "ALTER TABLE ad_accounts "
                        + "ADD COLUMN refresh_token_encrypted TEXT NULL COMMENT '加密后的 Refresh Token' "
                        + "AFTER access_token_encrypted");
        ensureColumn(
                "token_expires_at",
                "ALTER TABLE ad_accounts "
                        + "ADD COLUMN token_expires_at DATETIME NULL COMMENT 'Token 过期时间' "
                        + "AFTER refresh_token_encrypted");
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            if (hasColumn(columnName)) {
                return;
            }
            jdbcTemplate.execute(ddl);
            log.info("AdAccountOAuthColumnsInitializer: added ad_accounts.{}", columnName);
        } catch (Exception e) {
            log.error(
                    "AdAccountOAuthColumnsInitializer failed for column {}: {}",
                    columnName,
                    e.getMessage(),
                    e);
        }
    }

    private boolean hasColumn(String columnName) {
        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) "
                                + "FROM information_schema.columns "
                                + "WHERE table_schema = DATABASE() "
                                + "AND table_name = 'ad_accounts' "
                                + "AND column_name = ?",
                        Integer.class,
                        columnName);
        return count != null && count > 0;
    }
}
