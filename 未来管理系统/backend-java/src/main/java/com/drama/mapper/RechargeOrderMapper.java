package com.drama.mapper;

import com.drama.dto.PromotionRechargeAggRow;
import com.drama.entity.RechargeOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RechargeOrderMapper {

    int insert(RechargeOrder order);

    List<RechargeOrder> selectByPromotionAndDate(
            @Param("promotionId") String promotionId, @Param("date") LocalDate date);

    List<PromotionRechargeAggRow> selectAggByDate(@Param("date") LocalDate date);

    /** 指定推广、自然日内的充值总额 */
    BigDecimal sumAmountByPromotionAndDate(
            @Param("promotionId") String promotionId, @Param("date") LocalDate date);

    /**
     * 按小时桶汇总充值金额（start/end 为半开区间 [start, end) 可用字符串或 Timestamp，此处用 DATETIME）
     */
    BigDecimal sumAmountByPromotionAndHourStart(
            @Param("promotionId") String promotionId, @Param("hourStart") String hourStart, @Param("hourEnd") String hourEnd);

    /** #086：多推广同一小时桶内充值合计 */
    BigDecimal sumAmountByPromotionsAndHourStart(
            @Param("promotionIds") List<String> promotionIds,
            @Param("hourStart") String hourStart,
            @Param("hourEnd") String hourEnd);
}
