package com.drama.mapper;

import com.drama.entity.UserWatchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserWatchHistoryMapper {
    UserWatchHistory selectById(@Param("id") Long id);

    int insert(UserWatchHistory row);

    int update(UserWatchHistory row);

    int deleteById(@Param("id") Long id);
}
