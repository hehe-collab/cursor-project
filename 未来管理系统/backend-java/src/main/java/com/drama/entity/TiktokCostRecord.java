package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "TikTok费用记录")
@Data
public class TiktokCostRecord {

    private Integer id;
    private String promotionId;
    private String accountId;
    private String accountName;
    private BigDecimal balance;
    private String campaignName;
    private BigDecimal cost;
    private Long impressions;
    private LocalDateTime recordTime;
    private LocalDateTime createdAt;
}
