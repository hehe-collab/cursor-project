package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TikTok 广告组（对齐 tiktok_adgroups） */
@Schema(description = "TikTok广告组")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TikTokAdGroup {

    private Long id;
    private String advertiserId;
    private String campaignId;
    private String adgroupId;
    private String adgroupName;
    private String placementType;
    private String placements;
    private BigDecimal budget;
    private String budgetMode;
    private String billingEvent;
    private String bidType;
    private BigDecimal bidPrice;
    /** TikTok optimization_goal：VALUE(ROAS) / CONVERT 等 */
    private String optimizationGoal;
    /** ROAS 目标（optimization_goal=VALUE 时使用） */
    private BigDecimal roasBid;
    /** 深度出价类型，如 VO_MIN_ROAS */
    private String deepBidType;
    private String locationIds;
    private String ageGroups;
    private String gender;
    private String languages;
    private String interestCategoryIds;
    private String scheduleType;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
    private String operationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
