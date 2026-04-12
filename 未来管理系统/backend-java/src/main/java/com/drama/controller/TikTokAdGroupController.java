package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokAdGroup;
import com.drama.service.TikTokAdGroupService;
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

@Tag(name = "TikTok广告组", description = "广告组的同步与 CRUD")
@Slf4j
@RestController
@RequestMapping("/api/tiktok/adgroups")
@RequiredArgsConstructor
public class TikTokAdGroupController {

    private final TikTokAdGroupService adGroupService;

    @Operation(summary = "获取广告组列表", description = "获取TikTok广告组列表")
    @GetMapping
    public Result<List<TikTokAdGroup>> getAdGroups(
            @Parameter(description = "广告主ID") @RequestParam(required = false) String advertiserId,
            @Parameter(description = "广告系列ID") @RequestParam(required = false) String campaignId) {
        try {
            return Result.success(adGroupService.getAdGroups(advertiserId, campaignId));
        } catch (Exception e) {
            log.error("Failed to get adgroups: {}", e.getMessage(), e);
            return Result.error("获取广告组列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取广告组详情", description = "根据ID获取广告组详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<TikTokAdGroup> getAdGroupById(@PathVariable Long id) {
        try {
            return Result.success(adGroupService.getAdGroupById(id));
        } catch (Exception e) {
            log.error("Failed to get adgroup {}: {}", id, e.getMessage(), e);
            return Result.error("获取广告组失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据广告组ID获取详情", description = "根据TikTok广告组ID获取详细信息")
    @GetMapping("/adgroup/{adgroupId}")
    public Result<TikTokAdGroup> getAdGroupByAdgroupId(@PathVariable String adgroupId) {
        try {
            return Result.success(adGroupService.getAdGroupByAdgroupId(adgroupId));
        } catch (Exception e) {
            log.error("Failed to get adgroup by adgroup_id {}: {}", adgroupId, e.getMessage(), e);
            return Result.error("获取广告组失败: " + e.getMessage());
        }
    }

    @Operation(summary = "同步广告组", description = "从TikTok同步广告组到本地")
    @PostMapping("/sync")
    public Result<List<TikTokAdGroup>> syncAdGroups(@RequestBody Map<String, String> params) {
        try {
            String advertiserId = params.get("advertiser_id");
            String campaignId = params.get("campaign_id");
            if (advertiserId == null || advertiserId.isEmpty()) {
                return Result.error("advertiser_id 不能为空");
            }
            return Result.success(adGroupService.syncAdGroupsFromTikTok(advertiserId, campaignId));
        } catch (Exception e) {
            log.error("Failed to sync adgroups: {}", e.getMessage(), e);
            return Result.error("同步广告组失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建广告组", description = "创建一个新的TikTok广告组")
    @PostMapping
    public Result<TikTokAdGroup> createAdGroup(@RequestBody TikTokAdGroup adGroup) {
        try {
            return Result.success(adGroupService.createAdGroup(adGroup));
        } catch (Exception e) {
            log.error("Failed to create adgroup: {}", e.getMessage(), e);
            return Result.error("创建广告组失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新广告组状态", description = "更新指定广告组的启用状态")
    @PutMapping("/{adgroupId}/status")
    public Result<TikTokAdGroup> updateAdGroupStatus(
            @PathVariable String adgroupId, @RequestBody Map<String, String> params) {
        try {
            String status = params.get("status");
            if (status == null || status.isEmpty()) {
                return Result.error("status 不能为空");
            }
            return Result.success(adGroupService.updateAdGroupStatus(adgroupId, status));
        } catch (Exception e) {
            log.error("Failed to update adgroup status {}: {}", adgroupId, e.getMessage(), e);
            return Result.error("更新广告组状态失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除广告组", description = "删除指定的广告组")
    @DeleteMapping("/{adgroupId}")
    public Result<Void> deleteAdGroup(@PathVariable String adgroupId) {
        try {
            adGroupService.deleteAdGroup(adgroupId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to delete adgroup {}: {}", adgroupId, e.getMessage(), e);
            return Result.error("删除广告组失败: " + e.getMessage());
        }
    }
}
