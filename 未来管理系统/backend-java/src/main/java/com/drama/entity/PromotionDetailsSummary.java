package com.drama.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PromotionDetailsSummary {

    private Integer id;
    private LocalDate date;
    private String promotionId;
    private String promotionName;
    /** 投放媒体（与 promotion_links.platform 对齐） */
    private String platform;
    /** 国家代码（与 promotion_links.country 对齐） */
    private String country;
    private Integer dramaId;
    private String dramaName;
    private String accountId;
    private String accountName;
    private BigDecimal balance;
    private String campaignName;
    private BigDecimal cost;
    private BigDecimal speed;
    private BigDecimal roi;
    private Integer userCount;
    private BigDecimal rechargeAmount;
    private BigDecimal profit;
    private Integer orderCount;
    private Integer firstRechargeCount;
    private BigDecimal firstRechargeRate;
    private Integer repeatRechargeCount;
    /** 日曝光（冗余，便于汇总 CPM） */
    private Long impressions;
    private BigDecimal cpm;
    private BigDecimal avgRechargePerUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
