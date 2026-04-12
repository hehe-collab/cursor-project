package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "推广每日统计")
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
