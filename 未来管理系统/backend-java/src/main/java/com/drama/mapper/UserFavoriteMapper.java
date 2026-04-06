package com.drama.mapper;

import com.drama.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserFavoriteMapper {
    UserFavorite selectById(@Param("id") Long id);

    int insert(UserFavorite row);

    int deleteById(@Param("id") Long id);
}
