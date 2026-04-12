package com.drama.mapper;

import com.drama.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AdminMapper {

    Admin selectByUsername(@Param("username") String username);

    Admin selectById(@Param("id") int id);

    List<Admin> selectAll();

    int insert(Admin row);

    int update(Admin row);

    /** 仅更新 role_id 和 role 字段（保留 nickname 不动） */
    int updateRole(@Param("id") Integer id, @Param("roleId") Integer roleId, @Param("role") String role);

    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    int updatePasswordChanged(@Param("id") Integer id, @Param("passwordChanged") Integer passwordChanged);

    int deleteById(@Param("id") Integer id);
}