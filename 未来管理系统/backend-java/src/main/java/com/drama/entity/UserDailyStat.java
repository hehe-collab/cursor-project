package com.drama.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserDailyStat {
    private Long id;
    private Integer userPk;
    private LocalDate statDate;
    private String metric;
    private Long metricValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
