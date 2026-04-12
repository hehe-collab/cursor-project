package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.AdminLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "操作日志", description = "管理员操作日志查询与统计")
@RestController
@RequestMapping("/api/admin-logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    @Operation(summary = "分页查询操作日志", description = "根据条件分页查询管理员操作日志")
    @GetMapping
    public Result<Map<String, Object>> queryPage(
        @Parameter(description = "管理员ID") @RequestParam(required = false) Integer adminId,
        @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
        @Parameter(description = "目标类型") @RequestParam(required = false) String targetType,
        @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> data = adminLogService.queryPage(
            adminId, operationType, targetType, startDate, endDate, page, size);
        return Result.success(data);
    }

    @Operation(summary = "按类型统计操作日志", description = "按操作类型统计日志数量")
    @GetMapping("/stats/by-type")
    public Result<List<Map<String, Object>>> statsByType(
        @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.success(adminLogService.statsByOperationType(startDate, endDate));
    }
}
