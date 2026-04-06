package com.drama.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DramaDailyStat {
    private Long id;
    private Integer dramaId;
    private LocalDate statDate;
    private String metric;
    private Long metricValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
