package com.drama.mapper;

import com.drama.entity.Role;
import com.drama.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RoleMapper {

    List<Role> selectAll();

    Role selectById(@Param("id") Integer id);

    Role selectByCode(@Param("code") String code);

    int insert(Role role);

    int update(Role role);

    int deleteById(@Param("id") Integer id, @Param("isSystem") Integer isSystem);

    List<Permission> selectPermissionsByRoleId(@Param("roleId") Integer roleId);

    int insertRolePermission(@Param("roleId") Integer roleId, @Param("permissionId") Integer permissionId);

    int deleteRolePermissions(@Param("roleId") Integer roleId);
}