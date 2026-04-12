package com.drama.controller;

import com.drama.annotation.LogOperation;
import com.drama.common.Result;
import com.drama.entity.Admin;
import com.drama.entity.Role;
import com.drama.mapper.AdminMapper;
import com.drama.mapper.RoleMapper;
import com.drama.mapper.PermissionMapper;
import com.drama.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "管理员管理", description = "管理员账号的 CRUD 操作")
@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminMapper adminMapper;
    private final RoleMapper roleMapper;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "获取管理员列表", description = "获取所有管理员账号列表")
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Admin> admins = adminMapper.selectAll();
        List<Map<String, Object>> result = admins.stream().map(this::toMap).toList();
        return Result.success(result);
    }

    @Operation(summary = "获取管理员详情", description = "根据ID获取管理员详细信息")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Integer id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) return Result.error("管理员不存在");
        return Result.success(toMap(admin));
    }

    @Operation(summary = "创建管理员", description = "创建一个新的管理员账号")
    @PostMapping
    @LogOperation(type = "CREATE", desc = "创建管理员", targetType = "admin")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            checkAdminPermission(request, "admin:create");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }

        String username = str(body.get("username"));
        if (username.isBlank()) return Result.error("用户名不能为空");

        if (adminMapper.selectByUsername(username) != null) {
            return Result.error("用户名已存在");
        }
        String password = str(body.get("password"));
        if (password.length() < 6) return Result.error("密码至少 6 位");

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setNickname(str(body.get("nickname")));
        admin.setRoleId(toInteger(body.get("role_id")));
        admin.setRole(toRoleCode(admin.getRoleId()));
        adminMapper.insert(admin);

        Admin saved = adminMapper.selectById(admin.getId());
        return Result.success("创建成功", toMap(saved));
    }

    @Operation(summary = "更新管理员", description = "更新指定管理员的信息")
    @PutMapping("/{id}")
    @LogOperation(type = "UPDATE", desc = "更新管理员", targetType = "admin")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            checkAdminPermission(request, "admin:edit");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }

        Admin existing = adminMapper.selectById(id);
        if (existing == null) return Result.error("管理员不存在");

        if (body.containsKey("nickname")) existing.setNickname(str(body.get("nickname")));
        if (body.containsKey("role_id")) {
            existing.setRoleId(toInteger(body.get("role_id")));
            existing.setRole(toRoleCode(existing.getRoleId()));
        }
        adminMapper.update(existing);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "重置管理员密码", description = "重置指定管理员的密码")
    @PostMapping("/{id}/reset-password")
    @LogOperation(type = "RESET_PWD", desc = "重置管理员密码", targetType = "admin")
    public Result<Void> resetPassword(@PathVariable Integer id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            checkAdminPermission(request, "admin:edit");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }

        if (adminMapper.selectById(id) == null) return Result.error("管理员不存在");
        String newPassword = str(body.get("password"));
        if (newPassword.length() < 6) return Result.error("密码至少 6 位");
        adminMapper.updatePassword(id, passwordEncoder.encode(newPassword));
        return Result.success("密码已重置", null);
    }

    @Operation(summary = "删除管理员", description = "删除指定的管理员账号")
    @DeleteMapping("/{id}")
    @LogOperation(type = "DELETE", desc = "删除管理员", targetType = "admin")
    public Result<Void> delete(@PathVariable Integer id, HttpServletRequest request) {
        try {
            checkAdminPermission(request, "admin:delete");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }

        if (id == 1) return Result.error("不能删除超级管理员");
        if (adminMapper.selectById(id) == null) return Result.error("管理员不存在");
        adminMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    // -------- helpers --------

    private void checkAdminPermission(HttpServletRequest request, String permission) {
        Integer adminId = getAdminId(request);
        if (adminId == null) throw new RuntimeException("未授权");
        List<String> permissions = permissionService.getPermissionCodesByAdminId(adminId);
        if (!permissions.contains(permission)) {
            throw new RuntimeException("权限不足");
        }
    }

    private Integer getAdminId(HttpServletRequest request) {
        try {
            Object adminId = request.getAttribute("adminId");
            if (adminId instanceof Integer) return (Integer) adminId;
            if (adminId instanceof Number) return ((Number) adminId).intValue();
        } catch (Exception ignored) {}
        return null;
    }

    private Map<String, Object> toMap(Admin a) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("username", a.getUsername());
        m.put("nickname", a.getNickname());
        m.put("role", a.getRole());
        m.put("role_id", a.getRoleId());
        m.put("created_at", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        Role role = a.getRoleId() != null ? roleMapper.selectById(a.getRoleId()) : null;
        if (role != null) {
            Map<String, Object> r = new java.util.LinkedHashMap<>();
            r.put("id", role.getId());
            r.put("name", role.getName());
            r.put("code", role.getCode());
            m.put("role_obj", r);
        }
        return m;
    }

    private String toRoleCode(Integer roleId) {
        if (roleId == null) return null;
        Role r = roleMapper.selectById(roleId);
        return r != null ? r.getCode() : null;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static Integer toInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString().trim()); }
        catch (Exception e) { return null; }
    }
}
