package com.drama.mapper;

import com.drama.entity.PromotionDailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PromotionDailyStatMapper {
    PromotionDailyStat selectById(@Param("id") Long id);

    int insert(PromotionDailyStat row);
}
