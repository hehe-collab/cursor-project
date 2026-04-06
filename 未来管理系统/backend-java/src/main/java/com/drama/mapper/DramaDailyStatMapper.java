package com.drama.mapper;

import com.drama.entity.DramaDailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DramaDailyStatMapper {
    DramaDailyStat selectById(@Param("id") Long id);

    int insert(DramaDailyStat row);
}
