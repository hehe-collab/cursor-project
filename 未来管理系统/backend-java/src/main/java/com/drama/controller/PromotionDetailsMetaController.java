package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.PromotionDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推广明细筛选项（#082/#083）：独立 Controller，与列表/利润图同前缀，避免路径注册被误解析。
 */
@Tag(name = "推广筛选项", description = "推广明细筛选项（平台、国家）")
@RestController
@RequestMapping("/api/promotion-details")
@RequiredArgsConstructor
public class PromotionDetailsMetaController {

    private final PromotionDetailsService promotionDetailsService;

    @Operation(summary = "获取平台列表", description = "获取推广明细可选的平台列表")
    @GetMapping("/platforms")
    public Result<List<String>> platforms() {
        return Result.success(promotionDetailsService.getPlatforms());
    }

    @Operation(summary = "获取国家列表", description = "获取推广明细可选的国家列表")
    @GetMapping("/countries")
    public Result<List<Map<String, String>>> countries() {
        return Result.success(promotionDetailsService.getCountries());
    }
}
