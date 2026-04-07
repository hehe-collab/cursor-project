package com.drama.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PromotionRechargeAggRow {

    private String promotionId;
    private Integer orderCount;
    private BigDecimal rechargeAmount;
    private Integer firstRechargeCount;
    private Integer repeatRechargeCount;
}
