package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TikTok 广告系列（对齐 tiktok_campaigns.operation_status） */
@Schema(description = "TikTok广告系列")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TikTokCampaign {

    private Long id;
    private String advertiserId;
    private String campaignId;
    private String campaignName;
    private String objective;
    private BigDecimal budget;
    private String budgetMode;
    private String operationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
