package com.drama.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PromotionTiktokDayAggRow {

    private String promotionId;
    /** 当日内累计消耗曲线：MAX(cost)-MIN(cost) 近似当日增量 */
    private BigDecimal dayCost;
    private Long maxImpressions;
    /** 日内任一条消耗记录的账户（仅用于关联 ad_accounts） */
    private String accountId;
}
