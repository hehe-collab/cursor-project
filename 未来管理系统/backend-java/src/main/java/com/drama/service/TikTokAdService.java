package com.drama.service;

import com.drama.dto.TikTokApiResponseDTO;
import com.drama.entity.TikTokAd;
import com.drama.mapper.TikTokAdMapper;
import com.drama.util.TikTokApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class TikTokAdService {

    private final TikTokAdMapper adMapper;
    private final TikTokAccountService accountService;
    private final TikTokApiClient apiClient;
    private final ObjectMapper objectMapper;

    private static final TypeReference<TikTokApiResponseDTO<Map<String, Object>>> MAP_RESPONSE =
            new TypeReference<>() {};

    public List<TikTokAd> getAds(String advertiserId, String campaignId, String adgroupId) {
        if (StringUtils.hasText(adgroupId)) {
            return adMapper.selectByAdgroupId(adgroupId);
        }
        if (StringUtils.hasText(campaignId)) {
            return adMapper.selectByCampaignId(campaignId);
        }
        if (StringUtils.hasText(advertiserId)) {
            return adMapper.selectByAdvertiserId(advertiserId);
        }
        return adMapper.selectAll();
    }

    public TikTokAd getAdById(Long id) {
        TikTokAd ad = adMapper.selectById(id);
        if (ad == null) {
            throw new IllegalStateException("Ad not found: " + id);
        }
        return ad;
    }

    public TikTokAd getAdByAdId(String adId) {
        TikTokAd ad = adMapper.selectByAdId(adId);
        if (ad == null) {
            throw new IllegalStateException("Ad not found: " + adId);
        }
        return ad;
    }

    @Transactional
    public List<TikTokAd> syncAdsFromTikTok(String advertiserId, String campaignId, String adgroupId) {
        String accessToken = accountService.getValidAccessToken(advertiserId);
        Map<String, Object> params = new HashMap<>();
        params.put("advertiser_id", advertiserId);
        if (StringUtils.hasText(campaignId)) {
            params.put("campaign_id", campaignId);
        }
        if (StringUtils.hasText(adgroupId)) {
            params.put("adgroup_id", adgroupId);
        }
        params.put("page", 1);
        params.put("page_size", 1000);

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.get(advertiserId, accessToken, "ad/get/", params, MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to sync ads: " + response.getMessage());
        }
        Map<String, Object> data = response.getData();
        if (data == null) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
        if (list == null || list.isEmpty()) {
            log.info("No ads advertiser={} campaign={} adgroup={}", advertiserId, campaignId, adgroupId);
            return List.of();
        }
        List<TikTokAd> ads = list.stream().map(this::mapToAd).toList();
        adMapper.batchUpsert(ads);
        log.info("Synced {} ads advertiser={}", ads.size(), advertiserId);
        return ads;
    }

    @Transactional
    public TikTokAd createAd(TikTokAd ad) {
        String accessToken = accountService.getValidAccessToken(ad.getAdvertiserId());
        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", ad.getAdvertiserId());
        body.put("adgroup_id", ad.getAdgroupId());
        body.put("ad_name", ad.getAdName());
        body.put("ad_text", ad.getAdText());
        body.put("landing_page_url", ad.getLandingPageUrl());
        body.put("display_name", ad.getDisplayName());
        body.put("creative_type", ad.getCreativeType());
        body.put("video_id", ad.getVideoId());
        body.put("image_ids", parseImageIds(ad.getImageIds()));
        body.put("call_to_action", ad.getCallToAction());

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        ad.getAdvertiserId(), accessToken, "ad/create/", body, MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to create ad: " + response.getMessage());
        }
        Map<String, Object> data = response.getData();
        if (data == null || data.get("ad_id") == null) {
            throw new IllegalStateException("TikTok ad/create missing ad_id");
        }
        String adId = data.get("ad_id").toString();
        ad.setAdId(adId);
        ad.setOperationStatus("ENABLE");
        adMapper.batchUpsert(List.of(ad));
        log.info("Created ad: {} ({})", adId, ad.getAdName());
        return getAdByAdId(adId);
    }

    @Transactional
    public TikTokAd updateAdStatus(String adId, String operationStatus) {
        TikTokAd ad = getAdByAdId(adId);
        String accessToken = accountService.getValidAccessToken(ad.getAdvertiserId());

        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", ad.getAdvertiserId());
        body.put("ad_ids", List.of(adId));
        body.put("opt_status", operationStatus);

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        ad.getAdvertiserId(),
                        accessToken,
                        "ad/update/status/",
                        body,
                        MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to update ad status: " + response.getMessage());
        }
        adMapper.updateOperationStatus(adId, operationStatus);
        log.info("Updated ad status: {} -> {}", adId, operationStatus);
        return getAdByAdId(adId);
    }

    @Transactional
    public void deleteAd(String adId) {
        TikTokAd ad = getAdByAdId(adId);
        String accessToken = accountService.getValidAccessToken(ad.getAdvertiserId());

        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", ad.getAdvertiserId());
        body.put("ad_ids", List.of(adId));

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        ad.getAdvertiserId(), accessToken, "ad/delete/", body, MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to delete ad: " + response.getMessage());
        }
        adMapper.deleteByAdId(adId);
        log.info("Deleted ad: {}", adId);
    }

    private TikTokAd mapToAd(Map<String, Object> data) {
        try {
            Object creatives = data.get("creatives");
            String imageIdsJson = null;
            String videoId = str(data.get("video_id"));
            if (creatives != null) {
                imageIdsJson = objectMapper.writeValueAsString(creatives);
            }
            return TikTokAd.builder()
                    .advertiserId(str(data.get("advertiser_id")))
                    .campaignId(str(data.get("campaign_id")))
                    .adgroupId(str(data.get("adgroup_id")))
                    .adId(str(data.get("ad_id")))
                    .adName(str(data.get("ad_name")))
                    .adText(str(data.get("ad_text")))
                    .landingPageUrl(str(data.get("landing_page_url")))
                    .displayName(str(data.get("display_name")))
                    .creativeType(str(data.get("creative_type")))
                    .videoId(videoId)
                    .imageIds(imageIdsJson != null ? imageIdsJson : str(data.get("image_ids")))
                    .callToAction(str(data.get("call_to_action")))
                    .pixelId(str(data.get("pixel_id")))
                    .operationStatus(str(data.get("operation_status")))
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to map ad row", e);
        }
    }

    private List<String> parseImageIds(String imageIds) {
        if (!StringUtils.hasText(imageIds)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(imageIds, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(imageIds);
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
