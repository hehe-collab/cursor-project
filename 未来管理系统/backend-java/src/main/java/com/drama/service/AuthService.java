package com.drama.service;

import com.drama.common.Result;
import com.drama.dto.LoginRequest;
import com.drama.entity.Admin;
import com.drama.exception.BusinessException;
import com.drama.mapper.AdminMapper;
import com.drama.config.JwtUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Result<Map<String, Object>> login(LoginRequest req) {
        Admin admin = adminMapper.selectByUsername(req.getUsername());
        if (admin == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), admin.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtUtil.createToken(admin.getId());
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("token", token);
        Map<String, Object> adminMap = adminMap(admin);
        inner.put("admin", adminMap);
        return Result.success("登录成功", inner);
    }

    public Result<Map<String, Object>> currentUser(Integer adminId) {
        if (adminId == null) {
            return Result.error(401, "未授权");
        }
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            return Result.error(401, "未授权");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", admin.getId());
        data.put("username", admin.getUsername());
        data.put("nickname", admin.getNickname());
        data.put("role", admin.getRole() != null ? admin.getRole() : "admin");
        return Result.success(data);
    }

    private static Map<String, Object> adminMap(Admin admin) {
        Map<String, Object> adminMap = new LinkedHashMap<>();
        adminMap.put("id", admin.getId());
        adminMap.put("username", admin.getUsername());
        adminMap.put("nickname", admin.getNickname());
        adminMap.put("role", admin.getRole() != null ? admin.getRole() : "admin");
        return adminMap;
    }
}
