package com.drama.mapper;

import com.drama.entity.TikTokCampaign;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TikTokCampaignMapper {

    int insert(TikTokCampaign row);

    TikTokCampaign selectById(@Param("id") Long id);

    TikTokCampaign selectByCampaignId(@Param("campaignId") String campaignId);

    List<TikTokCampaign> selectByAdvertiserId(@Param("advertiserId") String advertiserId);

    List<TikTokCampaign> selectAll();

    int update(TikTokCampaign row);

    int updateOperationStatus(@Param("campaignId") String campaignId, @Param("operationStatus") String operationStatus);

    int deleteById(@Param("id") Long id);

    int deleteByCampaignId(@Param("campaignId") String campaignId);

    int batchUpsert(@Param("list") List<TikTokCampaign> list);
}
