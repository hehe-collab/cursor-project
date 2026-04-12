package com.drama.controller;

import com.drama.annotation.LogOperation;
import com.drama.common.Result;
import com.drama.entity.Role;
import com.drama.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "角色管理", description = "角色与权限的 CRUD 操作")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "获取角色列表", description = "获取所有角色列表")
    @GetMapping
    public Result<List<Role>> list() {
        return Result.success(roleService.listAll());
    }

    @Operation(summary = "获取角色详情", description = "根据ID获取角色详细信息")
    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable Integer id) {
        return Result.success(roleService.getById(id));
    }

    @Operation(summary = "创建角色", description = "创建一个新的角色")
    @PostMapping
    @LogOperation(type = "CREATE", desc = "创建角色", targetType = "role")
    public Result<Role> create(@RequestBody Role role) {
        try {
            return Result.success(roleService.create(role));
        } catch (Exception e) {
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新角色", description = "更新指定角色的信息")
    @PutMapping("/{id}")
    @LogOperation(type = "UPDATE", desc = "更新角色", targetType = "role")
    public Result<Role> update(@PathVariable Integer id, @RequestBody Role role) {
        try {
            return Result.success(roleService.update(id, role));
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除角色", description = "删除指定的角色")
    @DeleteMapping("/{id}")
    @LogOperation(type = "DELETE", desc = "删除角色", targetType = "role")
    public Result<Void> delete(@PathVariable Integer id) {
        try {
            roleService.delete(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /** 为角色分配权限 */
    @Operation(summary = "分配角色权限", description = "为指定角色分配权限")
    @PostMapping("/{id}/permissions")
    @LogOperation(type = "ASSIGN_PERM", desc = "分配角色权限", targetType = "role")
    public Result<Void> assignPermissions(@PathVariable Integer id, @RequestBody Map<String, List<Integer>> payload) {
        try {
            List<Integer> ids = payload.get("permission_ids");
            roleService.assignPermissions(id, ids);
            return Result.success("权限分配成功", null);
        } catch (Exception e) {
            return Result.error("分配失败：" + e.getMessage());
        }
    }
}
