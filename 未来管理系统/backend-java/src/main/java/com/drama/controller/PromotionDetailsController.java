package com.drama.controller;

import com.drama.annotation.RateLimit;
import com.drama.common.Result;
import com.drama.dto.*;
import com.drama.service.PromotionDetailsService;
import com.drama.service.PromotionTiktokSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "推广明细", description = "推广明细列表与利润图表")
@RestController
@RequestMapping("/api/promotion-details")
@RequiredArgsConstructor
public class PromotionDetailsController {

    private final PromotionDetailsService promotionDetailsService;
    private final PromotionTiktokSyncService promotionTiktokSyncService;

    /** 列表：蛇形 query 与 #079 说明一致 */
    @Operation(summary = "获取推广明细列表", description = "获取推广明细列表，支持多条件筛选和分页")
    @GetMapping
    public Result<PromotionDetailsResponseDTO> list(
            @Parameter(description = "开始日期") @RequestParam(required = false) String start_date,
            @Parameter(description = "结束日期") @RequestParam(required = false) String end_date,
            @Parameter(description = "推广ID") @RequestParam(required = false) String promotion_id,
            @Parameter(description = "推广名称") @RequestParam(required = false) String promotion_name,
            @Parameter(description = "平台") @RequestParam(required = false) String platform,
            @Parameter(description = "短剧ID") @RequestParam(required = false) String drama_id,
            @Parameter(description = "短剧名称") @RequestParam(required = false) String drama_name,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "账户ID") @RequestParam(required = false) String account_id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int page_size) {
        PromotionDetailsQueryDTO q = new PromotionDetailsQueryDTO();
        q.setStartDate(parseDate(start_date));
        q.setEndDate(parseDate(end_date));
        q.setPromotionId(trimToNull(promotion_id));
        q.setPromotionName(trimToNull(promotion_name));
        q.setPlatform(trimToNull(platform));
        q.setDramaId(trimToNull(drama_id));
        q.setDramaName(trimToNull(drama_name));
        q.setCountry(trimToNull(country));
        q.setAccountId(trimToNull(account_id));
        q.setPage(page);
        q.setPageSize(page_size);
        return Result.success(promotionDetailsService.getPromotionDetails(q));
    }

    /**
     * #086：列表筛选条件下全部推广的汇总利润图（须放在 /{promotionId}/profit-chart 之前避免路径歧义）。
     */
    @Operation(summary = "获取全部推广利润图表", description = "获取筛选条件下所有推广的汇总利润图表")
    @GetMapping("/profit-chart-all")
    public Result<ProfitChartDataDTO> profitChartAll(
            @Parameter(description = "开始日期") @RequestParam(required = false) String start_date,
            @Parameter(description = "结束日期") @RequestParam(required = false) String end_date,
            @Parameter(description = "数据粒度：hour/day") @RequestParam(defaultValue = "hour") String granularity,
            @Parameter(description = "推广ID") @RequestParam(required = false) String promotion_id,
            @Parameter(description = "推广名称") @RequestParam(required = false) String promotion_name,
            @Parameter(description = "平台") @RequestParam(required = false) String platform,
            @Parameter(description = "短剧ID") @RequestParam(required = false) String drama_id,
            @Parameter(description = "短剧名称") @RequestParam(required = false) String drama_name,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "账户ID") @RequestParam(required = false) String account_id) {
        ProfitChartQueryDTO cq = new ProfitChartQueryDTO();
        cq.setStartDate(parseDate(start_date));
        cq.setEndDate(parseDate(end_date));
        cq.setGranularity(granularity);
        PromotionDetailsQueryDTO fq = new PromotionDetailsQueryDTO();
        fq.setPromotionId(trimToNull(promotion_id));
        fq.setPromotionName(trimToNull(promotion_name));
        fq.setPlatform(trimToNull(platform));
        fq.setDramaId(trimToNull(drama_id));
        fq.setDramaName(trimToNull(drama_name));
        fq.setCountry(trimToNull(country));
        fq.setAccountId(trimToNull(account_id));
        return Result.success(promotionDetailsService.getProfitChartAll(cq, fq));
    }

    @Operation(summary = "获取单推广利润图表", description = "获取指定推广的利润图表")
    @GetMapping("/{promotionId}/profit-chart")
    public Result<ProfitChartDataDTO> profitChart(
            @PathVariable("promotionId") String promotionId,
            @Parameter(description = "开始日期") @RequestParam(required = false) String start_date,
            @Parameter(description = "结束日期") @RequestParam(required = false) String end_date,
            @Parameter(description = "数据粒度：hour/day") @RequestParam(defaultValue = "day") String granularity) {
        ProfitChartQueryDTO q = new ProfitChartQueryDTO();
        q.setStartDate(parseDate(start_date));
        q.setEndDate(parseDate(end_date));
        q.setGranularity(granularity);
        return Result.success(promotionDetailsService.getProfitChart(promotionId, q));
    }

    @Operation(summary = "同步推广明细", description = "从TikTok同步推广明细数据")
    @PostMapping("/sync")
    @RateLimit(key = "promotion:sync", max = 10, timeout = 300, limitType = RateLimit.LimitType.USER)
    public Result<Void> sync() {
        promotionTiktokSyncService.runSyncAll();
        return Result.success(null);
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
