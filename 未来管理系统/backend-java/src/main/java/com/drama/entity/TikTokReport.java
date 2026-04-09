package com.drama.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TikTok 报表快照（对齐 tiktok_reports） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TikTokReport {

    private Long id;
    private String advertiserId;
    private String dimensions;
    private String dimensionId;
    private LocalDate statDate;
    private BigDecimal spend;
    private Integer impressions;
    private Integer clicks;
    private BigDecimal ctr;
    private BigDecimal cpc;
    private BigDecimal cpm;
    private Integer conversions;
    private BigDecimal conversionRate;
    private BigDecimal costPerConversion;
    private BigDecimal conversionValue;
    private BigDecimal roi;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
