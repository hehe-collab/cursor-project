package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理员操作日志")
@Data
public class AdminLog {
    private Long id;
    private Integer adminId;
    private String adminUsername;
    private String operationType;
    private String targetType;
    private String targetId;
    private String operationDesc;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private Integer responseStatus;
    private String errorMsg;
    private String ipAddress;
    private String userAgent;
    private Integer executionTime;
    private LocalDateTime createdAt;
}
