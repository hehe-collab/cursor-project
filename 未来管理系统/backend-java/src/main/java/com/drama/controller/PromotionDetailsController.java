package com.drama.controller;

import com.drama.common.Result;
import com.drama.dto.*;
import com.drama.service.PromotionDetailsService;
import com.drama.service.PromotionTiktokSyncService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotion-details")
@RequiredArgsConstructor
public class PromotionDetailsController {

    private final PromotionDetailsService promotionDetailsService;
    private final PromotionTiktokSyncService promotionTiktokSyncService;

    /** 列表：蛇形 query 与 #079 说明一致 */
    @GetMapping
    public Result<PromotionDetailsResponseDTO> list(
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date,
            @RequestParam(required = false) String promotion_id,
            @RequestParam(required = false) String promotion_name,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String drama_id,
            @RequestParam(required = false) String drama_name,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String account_id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
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
    @GetMapping("/profit-chart-all")
    public Result<ProfitChartDataDTO> profitChartAll(
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date,
            @RequestParam(defaultValue = "hour") String granularity,
            @RequestParam(required = false) String promotion_id,
            @RequestParam(required = false) String promotion_name,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String drama_id,
            @RequestParam(required = false) String drama_name,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String account_id) {
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

    @GetMapping("/{promotionId}/profit-chart")
    public Result<ProfitChartDataDTO> profitChart(
            @PathVariable("promotionId") String promotionId,
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date,
            @RequestParam(defaultValue = "day") String granularity) {
        ProfitChartQueryDTO q = new ProfitChartQueryDTO();
        q.setStartDate(parseDate(start_date));
        q.setEndDate(parseDate(end_date));
        q.setGranularity(granularity);
        return Result.success(promotionDetailsService.getProfitChart(promotionId, q));
    }

    @PostMapping("/sync")
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
