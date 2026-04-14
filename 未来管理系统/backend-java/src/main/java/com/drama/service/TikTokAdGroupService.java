package com.drama.service;

import com.drama.dto.TikTokApiResponseDTO;
import com.drama.entity.TikTokAdGroup;
import com.drama.mapper.TikTokAdGroupMapper;
import com.drama.util.TikTokApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class TikTokAdGroupService {

    private final TikTokAdGroupMapper adGroupMapper;
    private final TikTokAccountService accountService;
    private final TikTokApiClient apiClient;
    private final ObjectMapper objectMapper;

    private static final TypeReference<TikTokApiResponseDTO<Map<String, Object>>> MAP_RESPONSE =
            new TypeReference<>() {};
    private static final TypeReference<List<String>> STR_LIST = new TypeReference<>() {};

    public List<TikTokAdGroup> getAdGroups(String advertiserId, String campaignId) {
        if (StringUtils.hasText(campaignId)) {
            return adGroupMapper.selectByCampaignId(campaignId);
        }
        if (StringUtils.hasText(advertiserId)) {
            return adGroupMapper.selectByAdvertiserId(advertiserId);
        }
        return adGroupMapper.selectAll();
    }

    public TikTokAdGroup getAdGroupById(Long id) {
        TikTokAdGroup g = adGroupMapper.selectById(id);
        if (g == null) {
            throw new IllegalStateException("AdGroup not found: " + id);
        }
        return g;
    }

    public TikTokAdGroup getAdGroupByAdgroupId(String adgroupId) {
        TikTokAdGroup g = adGroupMapper.selectByAdgroupId(adgroupId);
        if (g == null) {
            throw new IllegalStateException("AdGroup not found: " + adgroupId);
        }
        return g;
    }

    @Transactional
    public List<TikTokAdGroup> syncAdGroupsFromTikTok(String advertiserId, String campaignId) {
        String accessToken = accountService.getValidAccessToken(advertiserId);
        Map<String, Object> params = new HashMap<>();
        params.put("advertiser_id", advertiserId);
        if (StringUtils.hasText(campaignId)) {
            params.put("campaign_id", campaignId);
        }
        params.put("page", 1);
        params.put("page_size", 1000);

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.get(advertiserId, accessToken, "adgroup/get/", params, MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to sync adgroups: " + response.getMessage());
        }
        Map<String, Object> data = response.getData();
        if (data == null) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
        if (list == null || list.isEmpty()) {
            log.info("No adgroups advertiser={} campaign={}", advertiserId, campaignId);
            return List.of();
        }
        List<TikTokAdGroup> rows = list.stream().map(this::mapToAdGroup).toList();
        adGroupMapper.batchUpsert(rows);
        log.info("Synced {} adgroups advertiser={} campaign={}", rows.size(), advertiserId, campaignId);
        return rows;
    }

    @Transactional
    public TikTokAdGroup createAdGroup(TikTokAdGroup adGroup) {
        String accessToken = accountService.getValidAccessToken(adGroup.getAdvertiserId());
        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", adGroup.getAdvertiserId());
        body.put("campaign_id", adGroup.getCampaignId());
        body.put("adgroup_name", adGroup.getAdgroupName());
        putIfHasText(body, "placement_type", adGroup.getPlacementType());
        List<String> placements = parseJsonStringList(adGroup.getPlacements());
        if (!placements.isEmpty()) {
            body.put("placements", placements);
        }
        List<String> locationIds = parseJsonStringList(adGroup.getLocationIds());
        if (!locationIds.isEmpty()) {
            body.put("location_ids", locationIds);
        }
        putIfHasText(body, "billing_event", adGroup.getBillingEvent());
        putIfHasText(body, "bid_type", adGroup.getBidType());
        if (adGroup.getBidPrice() != null) {
            body.put("bid_price", adGroup.getBidPrice());
        }
        if (adGroup.getBudget() != null) {
            body.put("budget", adGroup.getBudget());
        }
        putIfHasText(body, "budget_mode", adGroup.getBudgetMode());

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        adGroup.getAdvertiserId(),
                        accessToken,
                        "adgroup/create/",
                        body,
                        MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to create adgroup: " + response.getMessage());
        }
        Map<String, Object> data = response.getData();
        if (data == null || data.get("adgroup_id") == null) {
            throw new IllegalStateException("TikTok adgroup/create missing adgroup_id");
        }
        String adgroupId = data.get("adgroup_id").toString();
        adGroup.setAdgroupId(adgroupId);
        adGroup.setOperationStatus("ENABLE");
        adGroupMapper.batchUpsert(List.of(adGroup));
        log.info("Created adgroup: {} ({})", adgroupId, adGroup.getAdgroupName());
        return getAdGroupByAdgroupId(adgroupId);
    }

    @Transactional
    public TikTokAdGroup updateAdGroupStatus(String adgroupId, String operationStatus) {
        TikTokAdGroup adGroup = getAdGroupByAdgroupId(adgroupId);
        String accessToken = accountService.getValidAccessToken(adGroup.getAdvertiserId());

        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", adGroup.getAdvertiserId());
        body.put("adgroup_ids", List.of(adgroupId));
        body.put("opt_status", operationStatus);

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        adGroup.getAdvertiserId(),
                        accessToken,
                        "adgroup/update/status/",
                        body,
                        MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to update adgroup status: " + response.getMessage());
        }
        adGroupMapper.updateOperationStatus(adgroupId, operationStatus);
        log.info("Updated adgroup status: {} -> {}", adgroupId, operationStatus);
        return getAdGroupByAdgroupId(adgroupId);
    }

    @Transactional
    public void deleteAdGroup(String adgroupId) {
        TikTokAdGroup adGroup = getAdGroupByAdgroupId(adgroupId);
        String accessToken = accountService.getValidAccessToken(adGroup.getAdvertiserId());

        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", adGroup.getAdvertiserId());
        body.put("adgroup_ids", List.of(adgroupId));

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        adGroup.getAdvertiserId(),
                        accessToken,
                        "adgroup/delete/",
                        body,
                        MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to delete adgroup: " + response.getMessage());
        }
        adGroupMapper.deleteByAdgroupId(adgroupId);
        log.info("Deleted adgroup: {}", adgroupId);
    }

    private TikTokAdGroup mapToAdGroup(Map<String, Object> data) {
        try {
            return TikTokAdGroup.builder()
                    .advertiserId(str(data.get("advertiser_id")))
                    .campaignId(str(data.get("campaign_id")))
                    .adgroupId(str(data.get("adgroup_id")))
                    .adgroupName(str(data.get("adgroup_name")))
                    .placementType(str(data.get("placement_type")))
                    .placements(jsonOrNull(data.get("placements")))
                    .locationIds(jsonOrNull(data.get("location_ids")))
                    .bidType(str(data.get("bid_type")))
                    .bidPrice(decimal(data.get("bid_price")))
                    .budget(decimal(data.get("budget")))
                    .budgetMode(str(data.get("budget_mode")))
                    .operationStatus(str(data.get("operation_status")))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to map adgroup row", e);
        }
    }

    private String jsonOrNull(Object raw) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (raw == null) {
            return null;
        }
        return objectMapper.writeValueAsString(raw);
    }

    private List<String> parseJsonStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STR_LIST);
        } catch (Exception e) {
            log.warn("parseJsonStringList failed: {}", e.getMessage());
            return List.of();
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static void putIfHasText(Map<String, Object> body, String key, String value) {
        if (StringUtils.hasText(value)) {
            body.put(key, value);
        }
    }

    private static BigDecimal decimal(Object o) {
        if (o == null) {
            return null;
        }
        return new BigDecimal(o.toString());
    }
}
