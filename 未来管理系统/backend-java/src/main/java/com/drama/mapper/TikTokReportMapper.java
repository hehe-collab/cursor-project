package com.drama.mapper;

import com.drama.entity.TikTokReport;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TikTokReportMapper {

    int upsert(TikTokReport row);

    TikTokReport selectById(@Param("id") Long id);

    TikTokReport selectByUniqueKey(
            @Param("advertiserId") String advertiserId,
            @Param("dimensions") String dimensions,
            @Param("dimensionId") String dimensionId,
            @Param("statDate") LocalDate statDate);

    List<TikTokReport> selectByAdvertiserAndDateRange(
            @Param("advertiserId") String advertiserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<TikTokReport> selectByAdvertiserDimensionsAndDateRange(
            @Param("advertiserId") String advertiserId,
            @Param("dimensions") String dimensions,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    int deleteById(@Param("id") Long id);

    int batchUpsert(@Param("list") List<TikTokReport> list);
}
