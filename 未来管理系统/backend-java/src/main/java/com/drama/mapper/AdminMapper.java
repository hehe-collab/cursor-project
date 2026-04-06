package com.drama.mapper;

import com.drama.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {

    Admin selectByUsername(@Param("username") String username);

    Admin selectById(@Param("id") int id);

    int insert(Admin row);
}
