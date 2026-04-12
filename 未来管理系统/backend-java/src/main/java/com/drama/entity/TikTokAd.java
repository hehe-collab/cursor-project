package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TikTok 广告（对齐 tiktok_ads） */
@Schema(description = "TikTok广告")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TikTokAd {

    private Long id;
    private String advertiserId;
    private String campaignId;
    private String adgroupId;
    private String adId;
    private String adName;
    private String creativeType;
    private String videoId;
    private String imageIds;
    private String adText;
    private String callToAction;
    private String landingPageUrl;
    private String displayName;
    private String pixelId;
    private String operationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
