package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据统计", description = "按日聚合充值与 ROI 统计")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 对齐 Node GET /api/stats：按日聚合充值与 ROI */
    @Operation(summary = "获取日统计列表", description = "按日聚合充值与 ROI 统计数据")
    @GetMapping
    public Result<?> list(
            @Parameter(description = "短剧ID") @RequestParam(required = false) String dramaId,
            @Parameter(description = "媒体平台") @RequestParam(required = false) String media,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(statsService.dailyRechargeStats(dramaId, media, page, pageSize));
    }
}
