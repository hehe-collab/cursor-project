package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BatchTask {

    private Long id;
    private String taskId;
    private String taskType;
    private Integer userId;
    private String userName;
    private String adTaskId;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private String status;
    private Integer progress;
    private String errorMessage;
    private String configJson;
    private String resultJson;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
