package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokAd;
import com.drama.service.TikTokAdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "TikTok广告", description = "广告的同步与 CRUD")
@Slf4j
@RestController
@RequestMapping("/api/tiktok/ads")
@RequiredArgsConstructor
public class TikTokAdController {

    private final TikTokAdService adService;

    @Operation(summary = "获取广告列表", description = "获取TikTok广告列表")
    @GetMapping
    public Result<List<TikTokAd>> getAds(
            @Parameter(description = "广告主ID") @RequestParam(required = false) String advertiserId,
            @Parameter(description = "广告系列ID") @RequestParam(required = false) String campaignId,
            @Parameter(description = "广告组ID") @RequestParam(required = false) String adgroupId) {
        try {
            return Result.success(adService.getAds(advertiserId, campaignId, adgroupId));
        } catch (Exception e) {
            log.error("Failed to get ads: {}", e.getMessage(), e);
            return Result.error("获取广告列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取广告详情", description = "根据ID获取广告详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<TikTokAd> getAdById(@PathVariable Long id) {
        try {
            return Result.success(adService.getAdById(id));
        } catch (Exception e) {
            log.error("Failed to get ad {}: {}", id, e.getMessage(), e);
            return Result.error("获取广告失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据广告ID获取详情", description = "根据TikTok广告ID获取详细信息")
    @GetMapping("/ad/{adId}")
    public Result<TikTokAd> getAdByAdId(@PathVariable String adId) {
        try {
            return Result.success(adService.getAdByAdId(adId));
        } catch (Exception e) {
            log.error("Failed to get ad by ad_id {}: {}", adId, e.getMessage(), e);
            return Result.error("获取广告失败: " + e.getMessage());
        }
    }

    @Operation(summary = "同步广告", description = "从TikTok同步广告到本地")
    @PostMapping("/sync")
    public Result<List<TikTokAd>> syncAds(@RequestBody Map<String, String> params) {
        try {
            String advertiserId = params.get("advertiser_id");
            String campaignId = params.get("campaign_id");
            String adgroupId = params.get("adgroup_id");
            if (advertiserId == null || advertiserId.isEmpty()) {
                return Result.error("advertiser_id 不能为空");
            }
            return Result.success(adService.syncAdsFromTikTok(advertiserId, campaignId, adgroupId));
        } catch (Exception e) {
            log.error("Failed to sync ads: {}", e.getMessage(), e);
            return Result.error("同步广告失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建广告", description = "创建一个新的TikTok广告")
    @PostMapping
    public Result<TikTokAd> createAd(@RequestBody TikTokAd ad) {
        try {
            return Result.success(adService.createAd(ad));
        } catch (Exception e) {
            log.error("Failed to create ad: {}", e.getMessage(), e);
            return Result.error("创建广告失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新广告状态", description = "更新指定广告的启用状态")
    @PutMapping("/{adId}/status")
    public Result<TikTokAd> updateAdStatus(
            @PathVariable String adId, @RequestBody Map<String, String> params) {
        try {
            String status = params.get("status");
            if (status == null || status.isEmpty()) {
                return Result.error("status 不能为空");
            }
            return Result.success(adService.updateAdStatus(adId, status));
        } catch (Exception e) {
            log.error("Failed to update ad status {}: {}", adId, e.getMessage(), e);
            return Result.error("更新广告状态失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除广告", description = "删除指定的广告")
    @DeleteMapping("/{adId}")
    public Result<Void> deleteAd(@PathVariable String adId) {
        try {
            adService.deleteAd(adId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to delete ad {}: {}", adId, e.getMessage(), e);
            return Result.error("删除广告失败: " + e.getMessage());
        }
    }
}
