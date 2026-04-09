package com.drama.mapper;

import com.drama.entity.TikTokConversionLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TikTokConversionLogMapper {

    int insert(TikTokConversionLog row);

    TikTokConversionLog selectById(@Param("id") Long id);

    TikTokConversionLog selectByEventId(@Param("eventId") String eventId);

    List<TikTokConversionLog> selectByAdvertiserId(
            @Param("advertiserId") String advertiserId, @Param("limit") int limit, @Param("offset") int offset);

    List<TikTokConversionLog> selectPendingRetry(@Param("limit") int limit);

    int update(TikTokConversionLog row);

    int deleteById(@Param("id") Long id);

    int countSuccessByAdvertiserAndEventType(
            @Param("advertiserId") String advertiserId, @Param("eventType") String eventType);
}
