package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokReport;
import com.drama.service.TikTokReportService;
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
@Slf4j
@RestController
@RequestMapping("/api/tiktok/reports")
@RequiredArgsConstructor
public class TikTokReportController {

    private final TikTokReportService reportService;

    @GetMapping
    public Result<List<TikTokReport>> getReports(
            @RequestParam String advertiserId,
            @RequestParam(required = false) String dimensions,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
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

    @GetMapping("/summary")
    public Result<Map<String, Object>> getReportSummary(
            @RequestParam String advertiserId,
            @RequestParam(required = false) String dimensions,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
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
