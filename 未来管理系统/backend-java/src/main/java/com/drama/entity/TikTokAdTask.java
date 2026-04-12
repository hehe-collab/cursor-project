package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TikTok 广告任务队列（对齐 tiktok_ad_tasks）。JSON 列在 Java 中用 String 承载，由业务层序列化。
 */
@Schema(description = "TikTok广告任务")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TikTokAdTask {

    private Long id;
    private String advertiserId;
    private String taskType;
    private String taskName;
    private String taskParams;
    private String targetId;
    private String status;
    private String resultId;
    private String resultData;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private Integer priority;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
