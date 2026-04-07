package com.drama.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ProfitChartQueryDTO {

    private LocalDate startDate;
    private LocalDate endDate;
    /** day 或 hour */
    private String granularity = "day";
}
