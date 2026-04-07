package com.drama.mapper;

import com.drama.dto.RechargeStatsRow;
import com.drama.entity.RechargeRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RechargeRecordMapper {

    long selectMaxId();

    int insert(RechargeRecord row);

    int updateById(RechargeRecord row);

    int deleteById(@Param("id") long id);

    RechargeStatsRow selectStatsAggregate(
            @Param("userId") String userId,
            @Param("promotionId") String promotionId,
            @Param("orderKey") String orderKey,
            @Param("externalKey") String externalKey,
            @Param("country") String country,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("status") String status,
            @Param("platform") String platform,
            @Param("adAccountId") String adAccountId);

    long countByParam(
            @Param("userId") String userId,
            @Param("promotionId") String promotionId,
            @Param("orderKey") String orderKey,
            @Param("externalKey") String externalKey,
            @Param("country") String country,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("status") String status,
            @Param("platform") String platform,
            @Param("adAccountId") String adAccountId);

    List<RechargeRecord> selectByParam(
            @Param("userId") String userId,
            @Param("promotionId") String promotionId,
            @Param("orderKey") String orderKey,
            @Param("externalKey") String externalKey,
            @Param("country") String country,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("status") String status,
            @Param("platform") String platform,
            @Param("adAccountId") String adAccountId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    List<RechargeRecord> selectByParamWithUser(
            @Param("userId") String userId,
            @Param("promotionId") String promotionId,
            @Param("orderKey") String orderKey,
            @Param("externalKey") String externalKey,
            @Param("country") String country,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("status") String status,
            @Param("platform") String platform,
            @Param("adAccountId") String adAccountId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    RechargeRecord selectById(@Param("id") long id);

    long countAll();

    long countPendingApprox();

    int countEarlierPaid(
            @Param("userId") String userId,
            @Param("before") LocalDateTime before,
            @Param("excludeId") long excludeId);
}
