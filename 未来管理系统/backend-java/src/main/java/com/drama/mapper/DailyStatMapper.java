package com.drama.mapper;

import com.drama.entity.DailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DailyStatMapper {
    DailyStat selectById(@Param("id") Long id);

    int insert(DailyStat row);

    int update(DailyStat row);
}
