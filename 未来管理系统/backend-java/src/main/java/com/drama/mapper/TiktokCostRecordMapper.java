package com.drama.mapper;

import com.drama.dto.PromotionTiktokDayAggRow;
import com.drama.entity.TiktokCostRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TiktokCostRecordMapper {

    int upsert(TiktokCostRecord record);

    TiktokCostRecord selectLatestByPromotion(@Param("promotionId") String promotionId);

    TiktokCostRecord selectByPromotionAndTime(
            @Param("promotionId") String promotionId, @Param("recordTime") LocalDateTime recordTime);

    List<TiktokCostRecord> selectByPromotionBetween(
            @Param("promotionId") String promotionId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** #086：多推广在时间窗内的消耗快照（按 promotion_id、record_time 有序） */
    List<TiktokCostRecord> selectByPromotionIdsBetween(
            @Param("promotionIds") List<String> promotionIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** 当日截止当前：桶内 MAX(cost)-MIN(cost) 近似当日消耗增量 */
    BigDecimal selectIntraDaySpendDelta(
            @Param("promotionId") String promotionId, @Param("date") LocalDate date);

    List<PromotionTiktokDayAggRow> selectDayAggByDate(@Param("date") LocalDate date);
}
