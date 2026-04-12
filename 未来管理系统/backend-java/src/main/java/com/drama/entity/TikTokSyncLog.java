package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TikTok API 同步 / 调用日志（对齐 tiktok_sync_logs） */
@Schema(description = "TikTok API同步日志")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TikTokSyncLog {

    private Long id;
    private String advertiserId;
    private String syncType;
    private String apiEndpoint;
    private String requestMethod;
    private String requestParams;
    private Integer responseCode;
    private String responseData;
    private String status;
    private String errorMessage;
    private Integer durationMs;
    private LocalDateTime executedAt;
    private LocalDateTime createdAt;
}
