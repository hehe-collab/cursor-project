package com.drama.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DailyStat {
    private Long id;
    private LocalDate statDate;
    private String metric;
    private Long metricValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
