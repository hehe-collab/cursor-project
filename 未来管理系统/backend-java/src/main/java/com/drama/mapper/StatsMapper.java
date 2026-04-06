package com.drama.mapper;

import com.drama.dto.StatsDailyAggRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StatsMapper {

    List<StatsDailyAggRow> selectRechargeDailyAgg(
            @Param("dramaId") Integer dramaId, @Param("media") String media);
}
