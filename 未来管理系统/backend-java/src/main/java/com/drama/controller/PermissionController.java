package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.Permission;
import com.drama.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "权限管理", description = "权限列表与当前用户权限查询")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /** 所有权限（按模块分组） */
    @Operation(summary = "获取权限列表", description = "获取所有权限，按模块分组")
    @GetMapping
    public Result<Map<String, List<Permission>>> list() {
        return Result.success(permissionService.listGroupedByModule());
    }

    /** 当前管理员的权限代码列表 */
    @Operation(summary = "获取当前用户权限", description = "获取当前登录管理员的权限代码列表")
    @GetMapping("/my")
    public Result<List<String>> getMyPermissions(HttpServletRequest request) {
        Integer adminId = getAdminId(request);
        if (adminId == null) {
            return Result.error(401, "未授权");
        }
        return Result.success(permissionService.getPermissionCodesByAdminId(adminId));
    }

    private Integer getAdminId(HttpServletRequest request) {
        try {
            Object adminId = request.getAttribute("adminId");
            if (adminId instanceof Integer) return (Integer) adminId;
            if (adminId instanceof Number) return ((Number) adminId).intValue();
        } catch (Exception ignored) {}
        return null;
    }
}
