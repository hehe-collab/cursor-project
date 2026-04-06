package com.drama.mapper;

import com.drama.entity.UserDailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDailyStatMapper {
    UserDailyStat selectById(@Param("id") Long id);

    int insert(UserDailyStat row);
}
