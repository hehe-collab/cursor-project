package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokReport;
import com.drama.service.TikTokReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** TikTok 数据报告（{@code /api/tiktok/reports}，需 Bearer） */
@Tag(name = "TikTok报表", description = "TikTok 广告数据报表查询与同步")
@Slf4j
@RestController
@RequestMapping("/api/tiktok/reports")
@RequiredArgsConstructor
public class TikTokReportController {

    private final TikTokReportService reportService;

    @Operation(summary = "获取报表列表", description = "获取TikTok广告数据报表")
    @GetMapping
    public Result<List<TikTokReport>> getReports(
            @Parameter(description = "广告主ID") @RequestParam String advertiserId,
            @Parameter(description = "维度") @RequestParam(required = false) String dimensions,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(7);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            List<TikTokReport> reports = reportService.getReports(advertiserId, dimensions, startDate, endDate);
            return Result.success(reports);
        } catch (Exception e) {
            log.error("Failed to get reports: {}", e.getMessage(), e);
            return Result.error("获取报告列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取报表详情", description = "根据ID获取报表详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<TikTokReport> getReportById(@PathVariable Long id) {
        try {
            TikTokReport report = reportService.getReportById(id);
            return Result.success(report);
        } catch (Exception e) {
            log.error("Failed to get report {}: {}", id, e.getMessage(), e);
            return Result.error("获取报告失败: " + e.getMessage());
        }
    }

    @Operation(summary = "同步报表数据", description = "从TikTok同步广告数据报表到本地")
    @PostMapping("/sync")
    public Result<List<TikTokReport>> syncReports(@RequestBody Map<String, Object> params) {
        try {
            String advertiserId = stringParam(params.get("advertiser_id"));
            if (advertiserId == null || advertiserId.isEmpty()) {
                return Result.error("advertiser_id 不能为空");
            }
            String dimensions = stringParam(params.get("dimensions"));
            if (dimensions == null || dimensions.isEmpty()) {
                dimensions = "campaign";
            }
            LocalDate startDate = parseDateParam(params.get("start_date"), LocalDate.now().minusDays(7));
            LocalDate endDate = parseDateParam(params.get("end_date"), LocalDate.now());
            List<TikTokReport> reports =
                    reportService.syncReportsFromTikTok(advertiserId, dimensions, startDate, endDate);
            return Result.success(reports);
        } catch (Exception e) {
            log.error("Failed to sync reports: {}", e.getMessage(), e);
            return Result.error("同步报告数据失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取报表汇总", description = "获取TikTok广告数据报表的汇总统计")
    @GetMapping("/summary")
    public Result<Map<String, Object>> getReportSummary(
            @Parameter(description = "广告主ID") @RequestParam String advertiserId,
            @Parameter(description = "维度") @RequestParam(required = false) String dimensions,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(7);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            List<TikTokReport> reports = reportService.getReports(advertiserId, dimensions, startDate, endDate);
            BigDecimal totalSpend = BigDecimal.ZERO;
            int totalImpressions = 0;
            int totalClicks = 0;
            int totalConversions = 0;
            BigDecimal totalConversionValue = BigDecimal.ZERO;
            for (TikTokReport r : reports) {
                if (r.getSpend() != null) {
                    totalSpend = totalSpend.add(r.getSpend());
                }
                if (r.getImpressions() != null) {
                    totalImpressions += r.getImpressions();
                }
                if (r.getClicks() != null) {
                    totalClicks += r.getClicks();
                }
                if (r.getConversions() != null) {
                    totalConversions += r.getConversions();
                }
                if (r.getConversionValue() != null) {
                    totalConversionValue = totalConversionValue.add(r.getConversionValue());
                }
            }
            Map<String, Object> summary = new HashMap<>();
            summary.put("total_spend", totalSpend);
            summary.put("total_impressions", totalImpressions);
            summary.put("total_clicks", totalClicks);
            summary.put("total_conversions", totalConversions);
            summary.put("total_conversion_value", totalConversionValue);
            summary.put("count", reports.size());
            return Result.success(summary);
        } catch (Exception e) {
            log.error("Failed to get report summary: {}", e.getMessage(), e);
            return Result.error("获取报告汇总失败: " + e.getMessage());
        }
    }

    private static String stringParam(Object o) {
        return o == null ? null : o.toString().trim();
    }

    private static LocalDate parseDateParam(Object o, LocalDate defaultVal) {
        if (o == null) {
            return defaultVal;
        }
        if (o instanceof LocalDate d) {
            return d;
        }
        String s = o.toString().trim();
        if (s.isEmpty()) {
            return defaultVal;
        }
        return LocalDate.parse(s);
    }
}
