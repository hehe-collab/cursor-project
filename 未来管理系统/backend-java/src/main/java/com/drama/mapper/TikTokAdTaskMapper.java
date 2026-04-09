package com.drama.mapper;

import com.drama.entity.TikTokAdTask;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TikTokAdTaskMapper {

    int insert(TikTokAdTask row);

    TikTokAdTask selectById(@Param("id") Long id);

    List<TikTokAdTask> selectPendingTasks(@Param("limit") int limit);

    List<TikTokAdTask> selectByAdvertiserId(
            @Param("advertiserId") String advertiserId, @Param("limit") int limit, @Param("offset") int offset);

    List<TikTokAdTask> selectByStatus(
            @Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);

    int update(TikTokAdTask row);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int deleteById(@Param("id") Long id);

    int countByAdvertiserIdAndStatus(@Param("advertiserId") String advertiserId, @Param("status") String status);
}
