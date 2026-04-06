package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.DashboardService;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 数据概览：须与 {@code Dashboard.vue} 图表字段一致（含 {@code chart_data}、{@code recharge_status_dist}）。
     * 未传 {@code start_date}/{@code end_date} 时默认近 7 天。固定路径须在裸 {@code @GetMapping} 之前。
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate) {
        LocalDate[] r = parseRangeOrDefault7(startDate, endDate);
        return Result.success(dashboardService.stats(r[0], r[1]));
    }

    /** 按日趋势：{@code user} | {@code recharge} | {@code view}；{@code days} 默认 7、最大 366。 */
    @GetMapping("/trends")
    public Result<Map<String, Object>> trends(
            @RequestParam(defaultValue = "user") String type, @RequestParam(required = false) Integer days) {
        return Result.success(dashboardService.trends(type, days));
    }

    /**
     * 推广明细分页（演示数据）。筛选参数均为可选，组合过滤。
     */
    @GetMapping("/promotion-details")
    public Result<Map<String, Object>> promotionDetails(
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String promotion_id,
            @RequestParam(required = false) String promotion_name,
            @RequestParam(required = false) String drama_id,
            @RequestParam(required = false) String drama_name,
            @RequestParam(required = false) String account,
            @RequestParam(required = false) String media,
            @RequestParam(required = false) String country) {
        LocalDate start = null;
        LocalDate end = null;
        if (start_date != null && !start_date.isBlank()) {
            start = LocalDate.parse(start_date.substring(0, 10));
        }
        if (end_date != null && !end_date.isBlank()) {
            end = LocalDate.parse(end_date.substring(0, 10));
        }
        return Result.success(
                dashboardService.getPromotionDetails(
                        start,
                        end,
                        page,
                        pageSize,
                        promotion_id,
                        promotion_name,
                        drama_id,
                        drama_name,
                        account,
                        media,
                        country));
    }

    /**
     * 与 {@link #stats} 相同聚合；未传日期时默认近 7 天（含今天），便于 {@code curl} 与指令验收。
     */
    @GetMapping
    public Result<Map<String, Object>> summary(
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate) {
        LocalDate[] r = parseRangeOrDefault7(startDate, endDate);
        return Result.success(dashboardService.stats(r[0], r[1]));
    }

    private static LocalDate[] parseRangeOrDefault7(String startDate, String endDate) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        if (startDate != null && !startDate.isBlank()) {
            start = LocalDate.parse(startDate.substring(0, 10));
        }
        if (endDate != null && !endDate.isBlank()) {
            end = LocalDate.parse(endDate.substring(0, 10));
        }
        if (end.isBefore(start)) {
            LocalDate t = start;
            start = end;
            end = t;
        }
        return new LocalDate[] {start, end};
    }
}
