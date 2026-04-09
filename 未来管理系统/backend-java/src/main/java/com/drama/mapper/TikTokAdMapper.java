package com.drama.mapper;

import com.drama.entity.TikTokAd;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TikTokAdMapper {

    int insert(TikTokAd row);

    TikTokAd selectById(@Param("id") Long id);

    TikTokAd selectByAdId(@Param("adId") String adId);

    List<TikTokAd> selectByAdgroupId(@Param("adgroupId") String adgroupId);

    List<TikTokAd> selectByAdvertiserId(@Param("advertiserId") String advertiserId);

    List<TikTokAd> selectByCampaignId(@Param("campaignId") String campaignId);

    List<TikTokAd> selectAll();

    int update(TikTokAd row);

    int updateOperationStatus(@Param("adId") String adId, @Param("operationStatus") String operationStatus);

    int deleteById(@Param("id") Long id);

    int deleteByAdId(@Param("adId") String adId);

    int batchUpsert(@Param("list") List<TikTokAd> list);
}
