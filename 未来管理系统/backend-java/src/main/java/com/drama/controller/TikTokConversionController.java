package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokConversionLog;
import com.drama.service.TikTokConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** TikTok 回传管理（{@code /api/tiktok/conversions}，需 Bearer） */
@Tag(name = "TikTok回传", description = "TikTok 事件回传日志与统计")
@Slf4j
@RestController
@RequestMapping("/api/tiktok/conversions")
@RequiredArgsConstructor
public class TikTokConversionController {

    private final TikTokConversionService conversionService;

    @Operation(summary = "获取回传日志列表", description = "获取TikTok事件回传日志列表")
    @GetMapping
    public Result<Map<String, Object>> getConversionLogs(
            @Parameter(description = "广告主ID") @RequestParam(required = false) String advertiserId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        try {
            List<TikTokConversionLog> logs = conversionService.getConversionLogs(advertiserId, page, pageSize);
            int total = conversionService.countConversionLogs(advertiserId);
            Map<String, Object> data = new HashMap<>();
            data.put("list", logs);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("total", total);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get conversion logs: {}", e.getMessage(), e);
            return Result.error("获取回传日志列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取回传日志详情", description = "根据ID获取回传日志详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<TikTokConversionLog> getConversionLogById(@PathVariable Long id) {
        try {
            TikTokConversionLog row = conversionService.getConversionLogById(id);
            return Result.success(row);
        } catch (Exception e) {
            log.error("Failed to get conversion log {}: {}", id, e.getMessage(), e);
            return Result.error("获取回传日志失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据事件ID获取回传日志", description = "根据事件ID获取回传日志详情")
    @GetMapping("/event/{eventId}")
    public Result<TikTokConversionLog> getConversionLogByEventId(@PathVariable String eventId) {
        try {
            TikTokConversionLog row = conversionService.getConversionLogByEventId(eventId);
            if (row == null) {
                return Result.error("回传日志不存在: " + eventId);
            }
            return Result.success(row);
        } catch (Exception e) {
            log.error("Failed to get conversion log by event_id {}: {}", eventId, e.getMessage(), e);
            return Result.error("获取回传日志失败: " + e.getMessage());
        }
    }

    @Operation(summary = "发送回传事件", description = "向TikTok发送回传事件")
    @PostMapping
    public Result<TikTokConversionLog> sendConversionEvent(@RequestBody TikTokConversionLog conversionLog) {
        try {
            TikTokConversionLog result = conversionService.sendConversionEvent(conversionLog);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to send conversion event: {}", e.getMessage(), e);
            return Result.error("发送回传事件失败: " + e.getMessage());
        }
    }

    @Operation(summary = "重试失败的回传", description = "重试发送失败的回传事件")
    @PostMapping("/retry")
    public Result<Void> retryFailedConversions(@Parameter(description = "重试数量限制") @RequestParam(defaultValue = "100") int limit) {
        try {
            conversionService.retryFailedConversions(limit);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to retry failed conversions: {}", e.getMessage(), e);
            return Result.error("重试失败的回传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取回传统计", description = "获取回传事件的统计信息")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getConversionStats(
            @Parameter(description = "广告主ID") @RequestParam String advertiserId,
            @Parameter(description = "事件类型") @RequestParam(required = false) String eventType) {
        try {
            int count = conversionService.countConversions(advertiserId, eventType);
            Map<String, Object> stats = new HashMap<>();
            stats.put("advertiser_id", advertiserId);
            stats.put("event_type", eventType);
            stats.put("count", count);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to get conversion stats: {}", e.getMessage(), e);
            return Result.error("获取回传统计失败: " + e.getMessage());
        }
    }
}
