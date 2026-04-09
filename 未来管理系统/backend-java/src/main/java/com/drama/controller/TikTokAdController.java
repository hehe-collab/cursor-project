package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokAd;
import com.drama.service.TikTokAdService;
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

@Slf4j
@RestController
@RequestMapping("/api/tiktok/ads")
@RequiredArgsConstructor
public class TikTokAdController {

    private final TikTokAdService adService;

    @GetMapping
    public Result<List<TikTokAd>> getAds(
            @RequestParam(required = false) String advertiserId,
            @RequestParam(required = false) String campaignId,
            @RequestParam(required = false) String adgroupId) {
        try {
            return Result.success(adService.getAds(advertiserId, campaignId, adgroupId));
        } catch (Exception e) {
            log.error("Failed to get ads: {}", e.getMessage(), e);
            return Result.error("获取广告列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id:\\d+}")
    public Result<TikTokAd> getAdById(@PathVariable Long id) {
        try {
            return Result.success(adService.getAdById(id));
        } catch (Exception e) {
            log.error("Failed to get ad {}: {}", id, e.getMessage(), e);
            return Result.error("获取广告失败: " + e.getMessage());
        }
    }

    @GetMapping("/ad/{adId}")
    public Result<TikTokAd> getAdByAdId(@PathVariable String adId) {
        try {
            return Result.success(adService.getAdByAdId(adId));
        } catch (Exception e) {
            log.error("Failed to get ad by ad_id {}: {}", adId, e.getMessage(), e);
            return Result.error("获取广告失败: " + e.getMessage());
        }
    }

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

    @PostMapping
    public Result<TikTokAd> createAd(@RequestBody TikTokAd ad) {
        try {
            return Result.success(adService.createAd(ad));
        } catch (Exception e) {
            log.error("Failed to create ad: {}", e.getMessage(), e);
            return Result.error("创建广告失败: " + e.getMessage());
        }
    }

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
