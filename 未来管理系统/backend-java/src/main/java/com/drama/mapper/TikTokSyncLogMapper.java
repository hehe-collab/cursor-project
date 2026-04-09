package com.drama.mapper;

import com.drama.entity.TikTokSyncLog;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TikTokSyncLogMapper {

    int insert(TikTokSyncLog row);

    TikTokSyncLog selectById(@Param("id") Long id);

    List<TikTokSyncLog> selectByAdvertiserId(
            @Param("advertiserId") String advertiserId, @Param("limit") int limit, @Param("offset") int offset);

    List<TikTokSyncLog> selectRecentFailed(@Param("limit") int limit);

    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff, @Param("successOnly") boolean successOnly);

    int deleteById(@Param("id") Long id);
}
