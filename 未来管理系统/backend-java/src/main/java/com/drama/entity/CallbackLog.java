package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "回传日志")
@Data
public class CallbackLog {

    private Long id;
    private String orderNo;
    private String orderId;
    private String event;
    private String eventType;
    private String pixelId;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime sendTime;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
