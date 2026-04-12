package com.drama.service;

import com.drama.entity.Role;
import com.drama.entity.Permission;
import com.drama.mapper.RoleMapper;
import com.drama.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    public List<Role> listAll() {
        List<Role> roles = roleMapper.selectAll();
        for (Role role : roles) {
            role.setPermissions(roleMapper.selectPermissionsByRoleId(role.getId()));
        }
        return roles;
    }

    public Role getById(Integer id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        role.setPermissions(roleMapper.selectPermissionsByRoleId(id));
        return role;
    }

    @Transactional
    public Role create(Role role) {
        if (role.getIsSystem() == null) {
            role.setIsSystem(false);
        }
        roleMapper.insert(role);
        return getById(role.getId());
    }

    @Transactional
    public Role update(Integer id, Role role) {
        Role existing = roleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "角色不存在");
        }
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new BusinessException(400, "系统内置角色不允许修改");
        }
        role.setId(id);
        roleMapper.update(role);
        return getById(id);
    }

    @Transactional
    public void delete(Integer id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BusinessException(400, "系统内置角色不允许删除");
        }
        roleMapper.deleteRolePermissions(id);
        roleMapper.deleteById(id, 0);
    }

    @Transactional
    public void assignPermissions(Integer roleId, List<Integer> permissionIds) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        roleMapper.deleteRolePermissions(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Integer permissionId : permissionIds) {
                roleMapper.insertRolePermission(roleId, permissionId);
            }
        }
    }
}