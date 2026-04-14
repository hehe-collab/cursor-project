package com.drama.service;

import com.alibaba.fastjson2.JSON;
import com.drama.entity.AdMaterial;
import com.drama.entity.TikTokAd;
import com.drama.entity.TikTokAdGroup;
import com.drama.entity.TikTokCampaign;
import com.drama.entity.TitlePack;
import com.drama.mapper.AdMaterialMapper;
import com.drama.mapper.TitlePackMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchAdLaunchService {

    private static final BigDecimal DEFAULT_CAMPAIGN_BUDGET = new BigDecimal("1000");
    private static final BigDecimal DEFAULT_ADGROUP_BUDGET = new BigDecimal("500");
    private static final String DEFAULT_OBJECTIVE = "TRAFFIC";
    private static final String DEFAULT_BUDGET_MODE = "BUDGET_MODE_DAY";
    private static final String DEFAULT_BILLING_EVENT = "CPC";
    private static final String DEFAULT_BID_TYPE = "BID_TYPE_CUSTOM";
    private static final String DEFAULT_PLACEMENT = "PLACEMENT_TIKTOK";
    private static final String DEFAULT_CALL_TO_ACTION = "SHOP_NOW";

    private final AdAccountService adAccountService;
    private final AdMaterialMapper adMaterialMapper;
    private final TitlePackMapper titlePackMapper;
    private final TikTokCampaignService campaignService;
    private final TikTokAdGroupService adGroupService;
    private final TikTokAdService adService;

    public Map<String, Object> execute(Map<String, Object> config) {
        Map<String, Object> execution = new LinkedHashMap<>();
        List<Map<String, Object>> campaignResults = new ArrayList<>();
        List<Map<String, Object>> adGroupResults = new ArrayList<>();
        List<Map<String, Object>> adResults = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();

        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        int totalCount = 0;

        try {
            Map<String, Map<String, Object>> executableAccounts = loadExecutableAccounts();
            List<Map<String, Object>> projects = mapList(config.get("projects"));
            List<Map<String, Object>> adGroups = mapList(config.get("adGroups"));
            List<Map<String, Object>> ads = mapList(config.get("ads"));

            Map<String, Map<String, Object>> projectById = indexBy(projects, "id");
            Map<String, Map<String, Object>> adGroupByProjectId = indexBy(adGroups, "projectId");
            Map<String, String> campaignIdByProject = new LinkedHashMap<>();
            Map<String, List<String>> adgroupIdsByProject = new LinkedHashMap<>();

            for (Map<String, Object> project : projects) {
                String projectId = text(project.get("id"));
                String advertiserId = text(project.get("accountId"));
                String projectName =
                        firstNonBlank(text(project.get("projectName")), text(project.get("campaignName")));
                String existingCampaignId = text(project.get("existingProject"));

                totalCount++;
                if (!StringUtils.hasText(projectId)) {
                    failedCount++;
                    errors.add(error("campaign", "", advertiserId, "项目缺少 projectId"));
                    continue;
                }
                if (!executableAccounts.containsKey(advertiserId)) {
                    failedCount++;
                    errors.add(error("campaign", projectId, advertiserId, "账户不可执行或未完成 TikTok OAuth"));
                    continue;
                }
                try {
                    if (StringUtils.hasText(existingCampaignId)) {
                        campaignIdByProject.put(projectId, existingCampaignId);
                        campaignResults.add(campaignRecord(
                                projectId, advertiserId, existingCampaignId, projectName, "existing"));
                    } else {
                        if (!StringUtils.hasText(projectName)) {
                            throw new IllegalStateException("项目名称为空，无法创建广告系列");
                        }
                        TikTokCampaign created = campaignService.createCampaign(TikTokCampaign.builder()
                                .advertiserId(advertiserId)
                                .campaignName(projectName)
                                .objective(DEFAULT_OBJECTIVE)
                                .budget(decimal(project.get("dailyBudget"), DEFAULT_CAMPAIGN_BUDGET))
                                .budgetMode(DEFAULT_BUDGET_MODE)
                                .build());
                        campaignIdByProject.put(projectId, created.getCampaignId());
                        campaignResults.add(campaignRecord(
                                projectId,
                                advertiserId,
                                created.getCampaignId(),
                                firstNonBlank(created.getCampaignName(), projectName),
                                "created"));
                    }
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.add(error("campaign", projectId, advertiserId, e.getMessage()));
                    log.error("Batch create campaign failed project={} advertiser={}: {}", projectId, advertiserId, e.getMessage(), e);
                }
            }

            for (Map<String, Object> adGroupRow : adGroups) {
                String projectId = text(adGroupRow.get("projectId"));
                Map<String, Object> project = projectById.get(projectId);
                String advertiserId =
                        firstNonBlank(text(adGroupRow.get("accountId")), project != null ? text(project.get("accountId")) : "");
                String campaignId = campaignIdByProject.get(projectId);
                String projectName = project != null ? text(project.get("projectName")) : "";
                String adGroupName =
                        firstNonBlank(text(adGroupRow.get("adGroupName")), projectName + "-广告组");
                List<String> existingAdgroupIds = stringList(adGroupRow.get("existingAdGroups"));

                if (!StringUtils.hasText(projectId)) {
                    totalCount++;
                    failedCount++;
                    errors.add(error("adgroup", "", advertiserId, "广告组缺少 projectId"));
                    continue;
                }
                if (!StringUtils.hasText(campaignId)) {
                    totalCount++;
                    failedCount++;
                    errors.add(error("adgroup", projectId, advertiserId, "未找到对应广告系列，无法继续创建广告组"));
                    continue;
                }
                if (!executableAccounts.containsKey(advertiserId)) {
                    totalCount++;
                    failedCount++;
                    errors.add(error("adgroup", projectId, advertiserId, "账户不可执行或未完成 TikTok OAuth"));
                    continue;
                }
                try {
                    if (!existingAdgroupIds.isEmpty()) {
                        adgroupIdsByProject.put(projectId, new ArrayList<>(existingAdgroupIds));
                        for (String adgroupId : existingAdgroupIds) {
                            totalCount++;
                            successCount++;
                            adGroupResults.add(adGroupRecord(
                                    projectId, advertiserId, campaignId, adgroupId, adGroupName, "existing"));
                        }
                        continue;
                    }

                    totalCount++;
                    TikTokAdGroup created = adGroupService.createAdGroup(TikTokAdGroup.builder()
                            .advertiserId(advertiserId)
                            .campaignId(campaignId)
                            .adgroupName(adGroupName)
                            .placements(JSON.toJSONString(List.of(DEFAULT_PLACEMENT)))
                            .budget(decimal(project != null ? project.get("dailyBudget") : null, DEFAULT_ADGROUP_BUDGET))
                            .budgetMode(DEFAULT_BUDGET_MODE)
                            .billingEvent(DEFAULT_BILLING_EVENT)
                            .bidType(DEFAULT_BID_TYPE)
                            .bidPrice(decimal(adGroupRow.get("price"), null))
                            .build());
                    adgroupIdsByProject.put(projectId, new ArrayList<>(List.of(created.getAdgroupId())));
                    adGroupResults.add(adGroupRecord(
                            projectId,
                            advertiserId,
                            campaignId,
                            created.getAdgroupId(),
                            firstNonBlank(created.getAdgroupName(), adGroupName),
                            "created"));
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.add(error("adgroup", projectId, advertiserId, e.getMessage()));
                    log.error("Batch create adgroup failed project={} advertiser={}: {}", projectId, advertiserId, e.getMessage(), e);
                }
            }

            for (Map<String, Object> adRow : ads) {
                String projectId = text(adRow.get("projectId"));
                Map<String, Object> project = projectById.get(projectId);
                Map<String, Object> adGroupRow = adGroupByProjectId.get(projectId);
                String advertiserId =
                        firstNonBlank(text(adRow.get("accountId")), project != null ? text(project.get("accountId")) : "");
                String campaignId = campaignIdByProject.get(projectId);
                List<String> adgroupIds = adgroupIdsByProject.get(projectId);

                if (!hasAdInputs(adRow)) {
                    skippedCount++;
                    skipped.add(skipRecord("ad", projectId, advertiserId, "未配置广告素材或落地页，已跳过广告创建"));
                    continue;
                }
                if (!StringUtils.hasText(campaignId) || adgroupIds == null || adgroupIds.isEmpty()) {
                    totalCount++;
                    failedCount++;
                    errors.add(error("ad", projectId, advertiserId, "未找到对应广告组，无法继续创建广告"));
                    continue;
                }
                if (!executableAccounts.containsKey(advertiserId)) {
                    totalCount++;
                    failedCount++;
                    errors.add(error("ad", projectId, advertiserId, "账户不可执行或未完成 TikTok OAuth"));
                    continue;
                }

                AdMaterial material = resolveMaterial(adRow.get("materialId"));
                if (material == null) {
                    totalCount++;
                    failedCount++;
                    errors.add(error("ad", projectId, advertiserId, "请选择有效的素材"));
                    continue;
                }
                if (StringUtils.hasText(material.getAccountId()) && !advertiserId.equals(material.getAccountId().trim())) {
                    totalCount++;
                    failedCount++;
                    errors.add(error("ad", projectId, advertiserId, "所选素材不属于当前账户，无法创建广告"));
                    continue;
                }

                String creativeType = "video".equalsIgnoreCase(text(material.getType())) ? "VIDEO" : "IMAGE";
                String landingPage = firstNonBlank(text(adRow.get("landingPage")), text(adRow.get("authPage")));
                if (!StringUtils.hasText(landingPage)) {
                    totalCount++;
                    failedCount++;
                    errors.add(error("ad", projectId, advertiserId, "缺少落地页链接"));
                    continue;
                }

                String adName =
                        firstNonBlank(text(adRow.get("adName")), project != null ? text(project.get("projectName")) : "", "广告");
                String adText = resolveAdText(adRow.get("titlePackId"), adName);
                String displayName =
                        firstNonBlank(text(adRow.get("authPage")), text(config.get("entity")), advertiserId);
                String pixelId = adGroupRow != null ? text(adGroupRow.get("pixel")) : "";

                for (String adgroupId : adgroupIds) {
                    totalCount++;
                    try {
                        TikTokAd created = adService.createAd(TikTokAd.builder()
                                .advertiserId(advertiserId)
                                .campaignId(campaignId)
                                .adgroupId(adgroupId)
                                .adName(adName)
                                .creativeType(creativeType)
                                .videoId("VIDEO".equals(creativeType)
                                        ? firstNonBlank(text(material.getVideoId()), text(material.getMaterialId()))
                                        : null)
                                .imageIds("IMAGE".equals(creativeType)
                                        ? JSON.toJSONString(List.of(text(material.getMaterialId())))
                                        : null)
                                .adText(adText)
                                .callToAction(DEFAULT_CALL_TO_ACTION)
                                .landingPageUrl(landingPage)
                                .displayName(displayName)
                                .pixelId(pixelId)
                                .build());
                        adResults.add(adRecord(
                                projectId,
                                advertiserId,
                                campaignId,
                                adgroupId,
                                created.getAdId(),
                                firstNonBlank(created.getAdName(), adName),
                                "created"));
                        successCount++;
                    } catch (Exception e) {
                        failedCount++;
                        errors.add(error(
                                "ad",
                                projectId,
                                advertiserId,
                                "广告组 " + adgroupId + " 创建广告失败: " + e.getMessage()));
                        log.error(
                                "Batch create ad failed project={} advertiser={} adgroup={}: {}",
                                projectId,
                                advertiserId,
                                adgroupId,
                                e.getMessage(),
                                e);
                    }
                }
            }
        } catch (Exception e) {
            failedCount++;
            errors.add(error("fatal", "", "", e.getMessage()));
            log.error("Batch execute failed: {}", e.getMessage(), e);
        }

        execution.put("triggeredAt", LocalDateTime.now().toString());
        execution.put("totalCount", totalCount);
        execution.put("successCount", successCount);
        execution.put("failedCount", failedCount);
        execution.put("skippedCount", skippedCount);
        execution.put("status", resolveStatus(successCount, failedCount));
        execution.put("campaigns", campaignResults);
        execution.put("adGroups", adGroupResults);
        execution.put("ads", adResults);
        execution.put("skipped", skipped);
        execution.put("errors", errors);
        return execution;
    }

    private Map<String, Map<String, Object>> loadExecutableAccounts() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map<String, Object> item : adAccountService.executableOptions("tiktok", "active")) {
            String accountId = text(item.get("accountId"));
            if (StringUtils.hasText(accountId) && !out.containsKey(accountId)) {
                out.put(accountId, item);
            }
        }
        return out;
    }

    private Map<String, Map<String, Object>> indexBy(List<Map<String, Object>> rows, String key) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String value = text(row.get(key));
            if (StringUtils.hasText(value) && !out.containsKey(value)) {
                out.put(value, row);
            }
        }
        return out;
    }

    private List<Map<String, Object>> mapList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    row.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                out.add(row);
            }
        }
        return out;
    }

    private boolean hasAdInputs(Map<String, Object> adRow) {
        return StringUtils.hasText(text(adRow.get("materialId")))
                || StringUtils.hasText(text(adRow.get("landingPage")))
                || StringUtils.hasText(text(adRow.get("authPage")))
                || StringUtils.hasText(text(adRow.get("titlePackId")));
    }

    private AdMaterial resolveMaterial(Object rawId) {
        Integer id = intVal(rawId);
        return id == null ? null : adMaterialMapper.selectById(id);
    }

    private String resolveAdText(Object rawTitlePackId, String fallback) {
        Integer id = intVal(rawTitlePackId);
        if (id == null) {
            return fallback;
        }
        TitlePack titlePack = titlePackMapper.selectById(id);
        if (titlePack == null) {
            return fallback;
        }
        return firstNonBlank(titlePack.getContent(), titlePack.getName(), fallback);
    }

    private List<String> stringList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            String value = text(item);
            if (StringUtils.hasText(value)) {
                out.add(value);
            }
        }
        return out;
    }

    private BigDecimal decimal(Object value, BigDecimal fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Integer intVal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> campaignRecord(
            String projectId, String advertiserId, String campaignId, String campaignName, String mode) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("projectId", projectId);
        item.put("advertiserId", advertiserId);
        item.put("campaignId", campaignId);
        item.put("campaignName", campaignName);
        item.put("mode", mode);
        return item;
    }

    private Map<String, Object> adGroupRecord(
            String projectId,
            String advertiserId,
            String campaignId,
            String adgroupId,
            String adgroupName,
            String mode) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("projectId", projectId);
        item.put("advertiserId", advertiserId);
        item.put("campaignId", campaignId);
        item.put("adgroupId", adgroupId);
        item.put("adgroupName", adgroupName);
        item.put("mode", mode);
        return item;
    }

    private Map<String, Object> adRecord(
            String projectId,
            String advertiserId,
            String campaignId,
            String adgroupId,
            String adId,
            String adName,
            String mode) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("projectId", projectId);
        item.put("advertiserId", advertiserId);
        item.put("campaignId", campaignId);
        item.put("adgroupId", adgroupId);
        item.put("adId", adId);
        item.put("adName", adName);
        item.put("mode", mode);
        return item;
    }

    private Map<String, Object> skipRecord(String stage, String projectId, String advertiserId, String reason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("stage", stage);
        item.put("projectId", projectId);
        item.put("advertiserId", advertiserId);
        item.put("reason", reason);
        return item;
    }

    private Map<String, Object> error(String stage, String projectId, String advertiserId, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("stage", stage);
        item.put("projectId", projectId);
        item.put("advertiserId", advertiserId);
        item.put("message", firstNonBlank(message, "未知错误"));
        return item;
    }

    private String resolveStatus(int successCount, int failedCount) {
        if (failedCount == 0) {
            return "success";
        }
        if (successCount > 0) {
            return "partial";
        }
        return "failed";
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
