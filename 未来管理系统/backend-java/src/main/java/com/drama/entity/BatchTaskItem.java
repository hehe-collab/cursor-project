package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BatchTaskItem {

    private Long id;
    private String taskId;
    private Integer itemIndex;
    private String stage;
    private String projectId;
    private String advertiserId;
    private String itemData;
    private String resultId;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
