package com.drama.mapper;

import com.drama.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PermissionMapper {

    List<Permission> selectAll();

    Permission selectById(@Param("id") Integer id);

    List<Permission> selectByModule(@Param("module") String module);

    /** 根据管理员 ID 获取其所有权限代码 */
    List<String> selectPermissionCodesByAdminId(@Param("adminId") Integer adminId);

    /** 全部权限代码（用于超级管理员兜底） */
    List<String> selectAllPermissionCodes();

    /** 查找某角色已分配的权限 ID 列表 */
    List<Integer> selectPermissionIdsByRoleId(@Param("roleId") Integer roleId);
}