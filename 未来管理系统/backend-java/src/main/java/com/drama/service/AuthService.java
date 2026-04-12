package com.drama.service;

import com.drama.common.Result;
import com.drama.dto.LoginRequest;
import com.drama.entity.Admin;
import com.drama.entity.Role;
import com.drama.exception.BusinessException;
import com.drama.mapper.AdminMapper;
import com.drama.mapper.RoleMapper;
import com.drama.mapper.PermissionMapper;
import com.drama.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminMapper adminMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    public Result<Map<String, Object>> login(LoginRequest req, String ip) {
        String username = req.getUsername();

        // 检查账户是否被锁定
        if (loginAttemptService.isUsernameBlocked(username)) {
            log.warn("[AUTH] 账户已锁定: username={}, ip={}", username, ip);
            throw new BusinessException(429, "登录失败次数过多，账户已锁定30分钟");
        }

        // 检查 IP 是否被锁定
        if (loginAttemptService.isIpBlocked(ip)) {
            log.warn("[AUTH] IP已锁定: ip={}", ip);
            throw new BusinessException(429, "该IP登录失败次数过多，已被锁定30分钟");
        }

        Admin admin = adminMapper.selectByUsername(username);
        if (admin == null) {
            loginAttemptService.loginFailed(username, ip);
            int remaining = loginAttemptService.getRemainingAttempts(username);
            throw new BusinessException(401, "用户名或密码错误（剩余 " + remaining + " 次尝试）");
        }

        if (!passwordEncoder.matches(req.getPassword(), admin.getPassword())) {
            loginAttemptService.loginFailed(username, ip);
            int remaining = loginAttemptService.getRemainingAttempts(username);
            throw new BusinessException(401, "用户名或密码错误（剩余 " + remaining + " 次尝试）");
        }

        // 登录成功，清除失败记录
        loginAttemptService.loginSucceeded(username, ip);
        log.info("[AUTH] 登录成功: username={}, ip={}", username, ip);

        String token = jwtUtil.createToken(admin.getId());

        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("token", token);
        inner.put("admin", adminMap(admin));

        List<String> permissions = permissionMapper.selectPermissionCodesByAdminId(admin.getId());
        if (permissions.isEmpty() && admin.getRoleId() != null) {
            Role bindRole = roleMapper.selectById(admin.getRoleId());
            if (bindRole != null && "super_admin".equals(bindRole.getCode())) {
                permissions = permissionMapper.selectAllPermissionCodes();
            }
        }
        inner.put("permissions", permissions);

        // 首次登录（密码未修改）时提示前端跳转修改密码
        boolean needChangePwd = admin.getPasswordChanged() == null || admin.getPasswordChanged() == 0;
        inner.put("need_change_password", needChangePwd);

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
        data.put("role_id", admin.getRoleId());
        List<String> permissions = permissionMapper.selectPermissionCodesByAdminId(adminId);
        if (permissions.isEmpty() && admin.getRoleId() != null) {
            Role bindRole = roleMapper.selectById(admin.getRoleId());
            if (bindRole != null && "super_admin".equals(bindRole.getCode())) {
                permissions = permissionMapper.selectAllPermissionCodes();
            }
        }
        data.put("permissions", permissions);
        Role role = admin.getRoleId() != null ? roleMapper.selectById(admin.getRoleId()) : null;
        if (role != null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", role.getId());
            r.put("name", role.getName());
            r.put("code", role.getCode());
            data.put("role_obj", r);
        }
        return Result.success(data);
    }

    private Map<String, Object> adminMap(Admin admin) {
        Map<String, Object> adminMap = new LinkedHashMap<>();
        adminMap.put("id", admin.getId());
        adminMap.put("username", admin.getUsername());
        adminMap.put("nickname", admin.getNickname());
        adminMap.put("role", admin.getRole() != null ? admin.getRole() : "admin");
        adminMap.put("role_id", admin.getRoleId());
        if (admin.getRoleId() != null) {
            Role role = roleMapper.selectById(admin.getRoleId());
            if (role != null) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", role.getId());
                r.put("name", role.getName());
                r.put("code", role.getCode());
                adminMap.put("role_obj", r);
            }
        }
        return adminMap;
    }
}