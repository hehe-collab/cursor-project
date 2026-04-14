package com.drama.bootstrap;

import com.drama.entity.Admin;
import com.drama.entity.Role;
import com.drama.mapper.AdminMapper;
import com.drama.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminMapper adminMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (adminMapper.selectByUsername("admin") != null) {
            return;
        }
        Admin a = new Admin();
        a.setUsername("admin");
        a.setPassword(passwordEncoder.encode("admin123"));
        a.setNickname("超级管理员");
        a.setRole("super_admin");

        Role superAdminRole = roleMapper.selectByCode("super_admin");
        if (superAdminRole != null) {
            a.setRoleId(superAdminRole.getId());
        } else {
            log.warn("AdminInitializer: super_admin role not found in DB, roleId not set. Please run schema-rbac.sql first.");
        }

        adminMapper.insert(a);
        log.info("AdminInitializer: default admin account created.");
    }
}
