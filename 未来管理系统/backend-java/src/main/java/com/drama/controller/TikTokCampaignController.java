package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokCampaign;
import com.drama.service.TikTokCampaignService;
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

@Tag(name = "TikTok广告系列", description = "广告系列的同步与 CRUD")
@Slf4j
@RestController
@RequestMapping("/api/tiktok/campaigns")
@RequiredArgsConstructor
public class TikTokCampaignController {

    private final TikTokCampaignService campaignService;

    @Operation(summary = "获取广告系列列表", description = "获取TikTok广告系列列表")
    @GetMapping
    public Result<List<TikTokCampaign>> getCampaigns(
            @Parameter(description = "广告主ID") @RequestParam(required = false) String advertiserId) {
        try {
            return Result.success(campaignService.getCampaigns(advertiserId));
        } catch (Exception e) {
            log.error("Failed to get campaigns: {}", e.getMessage(), e);
            return Result.error("获取广告系列列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取广告系列详情", description = "根据ID获取广告系列详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<TikTokCampaign> getCampaignById(@PathVariable Long id) {
        try {
            return Result.success(campaignService.getCampaignById(id));
        } catch (Exception e) {
            log.error("Failed to get campaign {}: {}", id, e.getMessage(), e);
            return Result.error("获取广告系列失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据广告系列ID获取详情", description = "根据TikTok广告系列ID获取详细信息")
    @GetMapping("/campaign/{campaignId}")
    public Result<TikTokCampaign> getCampaignByCampaignId(@PathVariable String campaignId) {
        try {
            return Result.success(campaignService.getCampaignByCampaignId(campaignId));
        } catch (Exception e) {
            log.error("Failed to get campaign by campaign_id {}: {}", campaignId, e.getMessage(), e);
            return Result.error("获取广告系列失败: " + e.getMessage());
        }
    }

    @Operation(summary = "同步广告系列", description = "从TikTok同步广告系列到本地")
    @PostMapping("/sync")
    public Result<List<TikTokCampaign>> syncCampaigns(@RequestBody Map<String, String> params) {
        try {
            String advertiserId = params.get("advertiser_id");
            if (advertiserId == null || advertiserId.isEmpty()) {
                return Result.error("advertiser_id 不能为空");
            }
            return Result.success(campaignService.syncCampaignsFromTikTok(advertiserId));
        } catch (Exception e) {
            log.error("Failed to sync campaigns: {}", e.getMessage(), e);
            return Result.error("同步广告系列失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建广告系列", description = "创建一个新的TikTok广告系列")
    @PostMapping
    public Result<TikTokCampaign> createCampaign(@RequestBody TikTokCampaign campaign) {
        try {
            return Result.success(campaignService.createCampaign(campaign));
        } catch (Exception e) {
            log.error("Failed to create campaign: {}", e.getMessage(), e);
            return Result.error("创建广告系列失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新广告系列", description = "更新指定广告系列的信息")
    @PutMapping("/{campaignId}")
    public Result<TikTokCampaign> updateCampaign(
            @PathVariable String campaignId, @RequestBody TikTokCampaign campaign) {
        try {
            return Result.success(campaignService.updateCampaign(campaignId, campaign));
        } catch (Exception e) {
            log.error("Failed to update campaign {}: {}", campaignId, e.getMessage(), e);
            return Result.error("更新广告系列失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新广告系列状态", description = "更新指定广告系列的启用状态")
    @PutMapping("/{campaignId}/status")
    public Result<TikTokCampaign> updateCampaignStatus(
            @PathVariable String campaignId, @RequestBody Map<String, String> params) {
        try {
            String status = params.get("status");
            if (status == null || status.isEmpty()) {
                return Result.error("status 不能为空");
            }
            return Result.success(campaignService.updateCampaignStatus(campaignId, status));
        } catch (Exception e) {
            log.error("Failed to update campaign status {}: {}", campaignId, e.getMessage(), e);
            return Result.error("更新广告系列状态失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除广告系列", description = "删除指定的广告系列")
    @DeleteMapping("/{campaignId}")
    public Result<Void> deleteCampaign(@PathVariable String campaignId) {
        try {
            campaignService.deleteCampaign(campaignId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to delete campaign {}: {}", campaignId, e.getMessage(), e);
            return Result.error("删除广告系列失败: " + e.getMessage());
        }
    }
}
