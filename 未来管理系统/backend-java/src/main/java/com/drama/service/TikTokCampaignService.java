package com.drama.service;

import com.drama.dto.TikTokApiResponseDTO;
import com.drama.entity.TikTokCampaign;
import com.drama.mapper.TikTokCampaignMapper;
import com.drama.util.TikTokApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TikTokCampaignService {

    private final TikTokCampaignMapper campaignMapper;
    private final TikTokAccountService accountService;
    private final TikTokApiClient apiClient;

    private static final TypeReference<TikTokApiResponseDTO<Map<String, Object>>> MAP_RESPONSE =
            new TypeReference<>() {};

    public List<TikTokCampaign> getCampaigns(String advertiserId) {
        if (StringUtils.hasText(advertiserId)) {
            return campaignMapper.selectByAdvertiserId(advertiserId);
        }
        return campaignMapper.selectAll();
    }

    public TikTokCampaign getCampaignById(Long id) {
        TikTokCampaign c = campaignMapper.selectById(id);
        if (c == null) {
            throw new IllegalStateException("Campaign not found: " + id);
        }
        return c;
    }

    public TikTokCampaign getCampaignByCampaignId(String campaignId) {
        TikTokCampaign c = campaignMapper.selectByCampaignId(campaignId);
        if (c == null) {
            throw new IllegalStateException("Campaign not found: " + campaignId);
        }
        return c;
    }

    @Transactional
    public List<TikTokCampaign> syncCampaignsFromTikTok(String advertiserId) {
        String accessToken = accountService.getValidAccessToken(advertiserId);
        Map<String, Object> params = new HashMap<>();
        params.put("advertiser_id", advertiserId);
        params.put("page", 1);
        params.put("page_size", 1000);

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.get(advertiserId, accessToken, "campaign/get/", params, MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to sync campaigns: " + response.getMessage());
        }
        Map<String, Object> data = response.getData();
        if (data == null) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> campaignList = (List<Map<String, Object>>) data.get("list");
        if (campaignList == null || campaignList.isEmpty()) {
            log.info("No campaigns for advertiser: {}", advertiserId);
            return List.of();
        }
        List<TikTokCampaign> campaigns =
                campaignList.stream().map(m -> mapToCampaign(advertiserId, m)).toList();
        campaignMapper.batchUpsert(campaigns);
        log.info("Synced {} campaigns advertiser={}", campaigns.size(), advertiserId);
        return campaigns;
    }

    @Transactional
    public TikTokCampaign createCampaign(TikTokCampaign campaign) {
        String accessToken = accountService.getValidAccessToken(campaign.getAdvertiserId());
        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", campaign.getAdvertiserId());
        body.put("campaign_name", campaign.getCampaignName());
        body.put("objective_type", campaign.getObjective());
        body.put("budget", campaign.getBudget());
        body.put("budget_mode", campaign.getBudgetMode());

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        campaign.getAdvertiserId(),
                        accessToken,
                        "campaign/create/",
                        body,
                        MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to create campaign: " + response.getMessage());
        }
        Map<String, Object> data = response.getData();
        if (data == null) {
            throw new IllegalStateException("TikTok campaign/create missing data");
        }
        Object cid = data.get("campaign_id");
        if (cid == null) {
            throw new IllegalStateException("TikTok campaign/create missing campaign_id");
        }
        String campaignId = cid.toString();
        campaign.setCampaignId(campaignId);
        campaign.setOperationStatus("ENABLE");

        campaignMapper.batchUpsert(List.of(campaign));
        log.info("Created campaign: {} ({})", campaignId, campaign.getCampaignName());
        return getCampaignByCampaignId(campaignId);
    }

    @Transactional
    public TikTokCampaign updateCampaign(String campaignId, TikTokCampaign patch) {
        TikTokCampaign existing = getCampaignByCampaignId(campaignId);
        String accessToken = accountService.getValidAccessToken(existing.getAdvertiserId());

        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", existing.getAdvertiserId());
        body.put("campaign_id", campaignId);
        if (patch.getCampaignName() != null) {
            body.put("campaign_name", patch.getCampaignName());
        }
        if (patch.getBudget() != null) {
            body.put("budget", patch.getBudget());
        }
        if (patch.getBudgetMode() != null) {
            body.put("budget_mode", patch.getBudgetMode());
        }

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        existing.getAdvertiserId(),
                        accessToken,
                        "campaign/update/",
                        body,
                        MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to update campaign: " + response.getMessage());
        }
        if (patch.getCampaignName() != null) {
            existing.setCampaignName(patch.getCampaignName());
        }
        if (patch.getBudget() != null) {
            existing.setBudget(patch.getBudget());
        }
        if (patch.getBudgetMode() != null) {
            existing.setBudgetMode(patch.getBudgetMode());
        }
        if (patch.getObjective() != null) {
            existing.setObjective(patch.getObjective());
        }
        campaignMapper.update(existing);
        log.info("Updated campaign: {}", campaignId);
        return getCampaignByCampaignId(campaignId);
    }

    @Transactional
    public TikTokCampaign updateCampaignStatus(String campaignId, String operationStatus) {
        TikTokCampaign campaign = getCampaignByCampaignId(campaignId);
        String accessToken = accountService.getValidAccessToken(campaign.getAdvertiserId());

        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", campaign.getAdvertiserId());
        body.put("campaign_ids", List.of(campaignId));
        body.put("opt_status", operationStatus);

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        campaign.getAdvertiserId(),
                        accessToken,
                        "campaign/update/status/",
                        body,
                        MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to update campaign status: " + response.getMessage());
        }
        campaignMapper.updateOperationStatus(campaignId, operationStatus);
        log.info("Updated campaign status: {} -> {}", campaignId, operationStatus);
        return getCampaignByCampaignId(campaignId);
    }

    @Transactional
    public void deleteCampaign(String campaignId) {
        TikTokCampaign campaign = getCampaignByCampaignId(campaignId);
        String accessToken = accountService.getValidAccessToken(campaign.getAdvertiserId());

        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", campaign.getAdvertiserId());
        body.put("campaign_ids", List.of(campaignId));

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        campaign.getAdvertiserId(),
                        accessToken,
                        "campaign/delete/",
                        body,
                        MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to delete campaign: " + response.getMessage());
        }
        campaignMapper.deleteByCampaignId(campaignId);
        log.info("Deleted campaign: {}", campaignId);
    }

    private TikTokCampaign mapToCampaign(String advertiserId, Map<String, Object> data) {
        String adv = data.get("advertiser_id") != null ? data.get("advertiser_id").toString() : advertiserId;
        return TikTokCampaign.builder()
                .advertiserId(adv)
                .campaignId(str(data.get("campaign_id")))
                .campaignName(str(data.get("campaign_name")))
                .objective(str(data.get("objective_type")))
                .budget(decimal(data.get("budget")))
                .budgetMode(str(data.get("budget_mode")))
                .operationStatus(str(data.get("operation_status")))
                .build();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static BigDecimal decimal(Object o) {
        if (o == null) {
            return null;
        }
        return new BigDecimal(o.toString());
    }
}
