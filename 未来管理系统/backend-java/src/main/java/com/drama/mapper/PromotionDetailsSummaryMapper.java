package com.drama.mapper;

import com.drama.dto.ProfitChartDataDTO;
import com.drama.dto.PromotionDetailsQueryDTO;
import com.drama.entity.PromotionDetailsSummary;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PromotionDetailsSummaryMapper {

    int upsert(PromotionDetailsSummary summary);

    int update(PromotionDetailsSummary summary);

    PromotionDetailsSummary selectByDateAndPromotion(
            @Param("date") LocalDate date, @Param("promotionId") String promotionId);

    List<PromotionDetailsSummary> selectByQuery(PromotionDetailsQueryDTO query);

    long countByQuery(PromotionDetailsQueryDTO query);

    /** 聚合行：衍生 ROI/首充率/人均/COMP 等在 Service 重算 */
    PromotionDetailsSummary selectSummaryAggregateByQuery(PromotionDetailsQueryDTO query);

    List<PromotionDetailsSummary> selectForProfitChart(
            @Param("promotionId") String promotionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** #086：筛选条件下按日汇总利润（多推广合计曲线） */
    List<ProfitChartDataDTO.ChartPoint> selectAggregatedProfitChartByQuery(PromotionDetailsQueryDTO query);

    /** #086：筛选条件下出现的推广 ID（用于按小时多推广聚合） */
    List<String> selectDistinctPromotionIdsByQuery(PromotionDetailsQueryDTO query);
}
