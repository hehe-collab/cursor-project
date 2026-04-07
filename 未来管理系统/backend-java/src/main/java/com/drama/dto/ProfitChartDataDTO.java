package com.drama.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ProfitChartDataDTO {

    private List<ChartPoint> chartData;

    @Data
    public static class ChartPoint {
        private String time;
        private BigDecimal profit;
    }
}
