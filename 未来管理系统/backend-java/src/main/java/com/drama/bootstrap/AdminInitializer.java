package com.drama.bootstrap;

import com.drama.entity.Admin;
import com.drama.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminMapper adminMapper;
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
        adminMapper.insert(a);
    }
}
