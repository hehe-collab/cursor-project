package com.drama.service;

import com.drama.entity.Permission;
import com.drama.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;

    public List<Permission> listAll() {
        return permissionMapper.selectAll();
    }

    /** 按模块分组返回权限 */
    public Map<String, List<Permission>> listGroupedByModule() {
        return permissionMapper.selectAll().stream()
                .collect(Collectors.groupingBy(Permission::getModule));
    }

    /** 获取管理员的所有权限代码 */
    public List<String> getPermissionCodesByAdminId(Integer adminId) {
        return permissionMapper.selectPermissionCodesByAdminId(adminId);
    }

    /** 获取某角色已分配的权限 ID 列表 */
    public List<Integer> getPermissionIdsByRoleId(Integer roleId) {
        return permissionMapper.selectPermissionIdsByRoleId(roleId);
    }
}