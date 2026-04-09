package com.drama.mapper;

import com.drama.entity.TikTokAdGroup;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TikTokAdGroupMapper {

    int insert(TikTokAdGroup row);

    TikTokAdGroup selectById(@Param("id") Long id);

    TikTokAdGroup selectByAdgroupId(@Param("adgroupId") String adgroupId);

    List<TikTokAdGroup> selectByCampaignId(@Param("campaignId") String campaignId);

    List<TikTokAdGroup> selectByAdvertiserId(@Param("advertiserId") String advertiserId);

    List<TikTokAdGroup> selectAll();

    int update(TikTokAdGroup row);

    int updateOperationStatus(@Param("adgroupId") String adgroupId, @Param("operationStatus") String operationStatus);

    int deleteById(@Param("id") Long id);

    int deleteByAdgroupId(@Param("adgroupId") String adgroupId);

    int batchUpsert(@Param("list") List<TikTokAdGroup> list);
}
