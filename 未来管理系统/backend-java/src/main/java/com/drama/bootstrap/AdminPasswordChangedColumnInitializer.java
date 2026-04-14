package com.drama.bootstrap;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 兼容旧库：补齐 admins.password_changed。
 *
 * <p>历史库可能只有 username/password/nickname/role 等字段，未执行过新增
 * password_changed 的 DDL，导致首次修改密码时报 SQL 异常。
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class AdminPasswordChangedColumnInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (hasPasswordChangedColumn()) {
                return;
            }

            jdbcTemplate.execute(
                    "ALTER TABLE admins "
                            + "ADD COLUMN password_changed TINYINT(1) NOT NULL DEFAULT 0 "
                            + "COMMENT '是否已修改初始密码：0=未改，1=已改'");

            backfillPasswordChanged();
            log.info("AdminPasswordChangedColumnInitializer: added admins.password_changed");
        } catch (Exception e) {
            log.error("AdminPasswordChangedColumnInitializer failed: {}", e.getMessage(), e);
        }
    }

    private boolean hasPasswordChangedColumn() {
        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) "
                                + "FROM information_schema.columns "
                                + "WHERE table_schema = DATABASE() "
                                + "AND table_name = 'admins' "
                                + "AND column_name = 'password_changed'",
                        Integer.class);
        return count != null && count > 0;
    }

    private void backfillPasswordChanged() {
        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList("SELECT id, username, password FROM admins");
        for (Map<String, Object> row : rows) {
            Integer id = toInteger(row.get("id"));
            if (id == null) {
                continue;
            }
            String username = stringVal(row.get("username"));
            String password = stringVal(row.get("password"));

            // 保留默认管理员首次登录强制改密；其它旧账号默认视为已改过密码。
            int passwordChanged =
                    ("admin".equalsIgnoreCase(username)
                                    && !password.isBlank()
                                    && passwordEncoder.matches("admin123", password))
                            ? 0
                            : 1;
            jdbcTemplate.update(
                    "UPDATE admins SET password_changed = ? WHERE id = ?",
                    passwordChanged,
                    id);
        }
    }

    private static String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
