package com.drama.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class StatsDailyAggRow {

    private LocalDate statDate;
    private Long orderCount;
    private BigDecimal rechargeAmount;
}
