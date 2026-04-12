package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TikTok 回传日志（对齐 tiktok_conversion_logs） */
@Schema(description = "TikTok回传日志")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TikTokConversionLog {

    private Long id;
    private String advertiserId;
    private String pixelId;
    private String eventType;
    private String eventId;
    private String userId;
    private String clickId;
    private String externalId;
    private BigDecimal eventValue;
    private String currency;
    private String contentType;
    private String contentId;
    private String status;
    private Integer responseCode;
    private String responseMessage;
    private Integer retryCount;
    private LocalDateTime eventTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
