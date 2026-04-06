package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 对齐 Node GET /api/stats：按日聚合充值与 ROI */
    @GetMapping
    public Result<?> list(
            @RequestParam(required = false) String dramaId,
            @RequestParam(required = false) String media,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(statsService.dailyRechargeStats(dramaId, media, page, pageSize));
    }
}
