package com.drama.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PromotionDailyStat {
    private Long id;
    private String promotionId;
    private LocalDate statDate;
    private String metric;
    private Long metricValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
