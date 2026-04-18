package com.drama.controller;

import com.drama.annotation.RateLimit;
import com.drama.common.Result;
import com.drama.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据看板", description = "数据概览、趋势图表、推广明细")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 数据概览：须与 {@code Dashboard.vue} 图表字段一致（含 {@code chart_data}、{@code recharge_status_dist}）。
     * 未传 {@code start_date}/{@code end_date} 时默认近 7 天。固定路径须在裸 {@code @GetMapping} 之前。
     */
    @Operation(summary = "获取数据概览", description = "获取数据概览统计信息，默认近7天")
    @GetMapping("/stats")
    @RateLimit(key = "dashboard:stats", max = 50, timeout = 1, limitType = RateLimit.LimitType.GLOBAL)
    public Result<Map<String, Object>> stats(
            @Parameter(description = "开始日期") @RequestParam(value = "start_date", required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(value = "end_date", required = false) String endDate) {
        LocalDate[] r = parseRangeOrDefault7(startDate, endDate);
        return Result.success(dashboardService.stats(r[0], r[1]));
    }

    /** 按日趋势：{@code user} | {@code recharge} | {@code view}；{@code days} 默认 7、最大 366。 */
    @Operation(summary = "获取趋势数据", description = "获取用户/充值/观看趋势数据")
    @GetMapping("/trends")
    public Result<Map<String, Object>> trends(
            @Parameter(description = "趋势类型：user/recharge/view") @RequestParam(defaultValue = "user") String type,
            @Parameter(description = "天数") @RequestParam(required = false) Integer days) {
        return Result.success(dashboardService.trends(type, days));
    }

    /**
     * 推广明细分页（演示数据）。筛选参数均为可选，组合过滤。
     */
    @Operation(summary = "获取推广明细", description = "获取推广明细列表，支持多条件筛选")
    @GetMapping("/promotion-details")
    public Result<Map<String, Object>> promotionDetails(
            @Parameter(description = "开始日期") @RequestParam(required = false) String start_date,
            @Parameter(description = "结束日期") @RequestParam(required = false) String end_date,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "推广ID") @RequestParam(required = false) String promotion_id,
            @Parameter(description = "推广名称") @RequestParam(required = false) String promotion_name,
            @Parameter(description = "短剧ID") @RequestParam(required = false) String drama_id,
            @Parameter(description = "短剧名称") @RequestParam(required = false) String drama_name,
            @Parameter(description = "账户") @RequestParam(required = false) String account,
            @Parameter(description = "媒体平台") @RequestParam(required = false) String media,
            @Parameter(description = "国家") @RequestParam(required = false) String country) {
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
    @Operation(summary = "获取数据摘要", description = "获取数据摘要统计信息，默认近7天")
    @GetMapping
    public Result<Map<String, Object>> summary(
            @Parameter(description = "开始日期") @RequestParam(value = "start_date", required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(value = "end_date", required = false) String endDate) {
        LocalDate[] r = parseRangeOrDefault7(startDate, endDate);
        return Result.success(dashboardService.stats(r[0], r[1]));
    }

    /** 短剧概览：按剧维度聚合充值排行、KPI、分类分布。 */
    @Operation(summary = "短剧概览", description = "按剧维度聚合充值排行、KPI、分类分布")
    @GetMapping("/drama-stats")
    public Result<Map<String, Object>> dramaStats(
            @Parameter(description = "开始日期") @RequestParam(value = "start_date", required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(value = "end_date", required = false) String endDate,
            @Parameter(description = "短剧名称") @RequestParam(value = "drama_name", required = false) String dramaName,
            @Parameter(description = "分类ID") @RequestParam(value = "category_id", required = false) Integer categoryId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        LocalDate[] r = parseRangeOrDefault7(startDate, endDate);
        return Result.success(dashboardService.dramaStats(r[0], r[1], dramaName, categoryId, page, pageSize));
    }

    /** 单剧每日充值趋势（弹窗折线图）。 */
    @Operation(summary = "单剧充值趋势", description = "指定短剧的每日充值趋势数据")
    @GetMapping("/drama-daily-recharge")
    public Result<Map<String, Object>> dramaDailyRecharge(
            @Parameter(description = "短剧ID") @RequestParam("drama_id") Integer dramaId,
            @Parameter(description = "开始日期") @RequestParam(value = "start_date", required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(value = "end_date", required = false) String endDate) {
        LocalDate[] r = parseRangeOrDefault7(startDate, endDate);
        return Result.success(dashboardService.getDramaDailyRecharge(dramaId, r[0], r[1]));
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
