package com.drama.service;

import com.drama.dto.TikTokApiResponseDTO;
import com.drama.entity.TikTokPixel;
import com.drama.mapper.TikTokPixelMapper;
import com.drama.util.TikTokApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
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
public class TikTokPixelService {

    private final TikTokPixelMapper pixelMapper;
    private final TikTokAccountService accountService;
    private final TikTokApiClient apiClient;

    private static final TypeReference<TikTokApiResponseDTO<Map<String, Object>>> MAP_RESPONSE =
            new TypeReference<>() {};

    public List<TikTokPixel> getPixels(String advertiserId) {
        if (StringUtils.hasText(advertiserId)) {
            return pixelMapper.selectByAdvertiserId(advertiserId);
        }
        return pixelMapper.selectAll();
    }

    public TikTokPixel getPixelById(Long id) {
        TikTokPixel pixel = pixelMapper.selectById(id);
        if (pixel == null) {
            throw new IllegalStateException("Pixel not found: " + id);
        }
        return pixel;
    }

    public TikTokPixel getPixelByPixelId(String pixelId) {
        TikTokPixel pixel = pixelMapper.selectByPixelId(pixelId);
        if (pixel == null) {
            throw new IllegalStateException("Pixel not found: " + pixelId);
        }
        return pixel;
    }

    @Transactional
    public List<TikTokPixel> syncPixelsFromTikTok(String advertiserId) {
        String accessToken = accountService.getValidAccessToken(advertiserId);
        Map<String, Object> params = new HashMap<>();
        params.put("advertiser_id", advertiserId);
        params.put("page", 1);
        params.put("page_size", 1000);

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.get(advertiserId, accessToken, "pixel/list/", params, MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to sync pixels: " + response.getMessage());
        }
        Map<String, Object> data = response.getData();
        if (data == null) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pixelList = (List<Map<String, Object>>) data.get("pixels");
        List<Map<String, Object>> list = pixelList;
        if (list == null || list.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> alt = (List<Map<String, Object>>) data.get("list");
            list = alt;
        }
        if (list == null || list.isEmpty()) {
            log.info("No pixels found for advertiser: {}", advertiserId);
            return List.of();
        }
        List<TikTokPixel> pixels = list.stream().map(m -> mapToPixel(advertiserId, m)).toList();

        for (TikTokPixel pixel : pixels) {
            if (!StringUtils.hasText(pixel.getPixelId())) {
                continue;
            }
            TikTokPixel existing = pixelMapper.selectByPixelId(pixel.getPixelId());
            if (existing != null) {
                pixel.setId(existing.getId());
                pixelMapper.update(pixel);
            } else {
                pixelMapper.insert(pixel);
            }
        }
        log.info("Synced {} pixels for advertiser: {}", pixels.size(), advertiserId);
        return pixelMapper.selectByAdvertiserId(advertiserId);
    }

    @Transactional
    public TikTokPixel createPixel(TikTokPixel pixel) {
        String accessToken = accountService.getValidAccessToken(pixel.getAdvertiserId());
        Map<String, Object> body = new HashMap<>();
        body.put("advertiser_id", pixel.getAdvertiserId());
        body.put("pixel_name", pixel.getPixelName());

        TikTokApiResponseDTO<Map<String, Object>> response =
                apiClient.post(
                        pixel.getAdvertiserId(),
                        accessToken,
                        "pixel/create/",
                        body,
                        MAP_RESPONSE);

        if (!response.isSuccess()) {
            throw new IllegalStateException("Failed to create pixel: " + response.getMessage());
        }
        Map<String, Object> data = response.getData();
        if (data == null) {
            throw new IllegalStateException("TikTok pixel/create missing data");
        }
        Object pid = data.get("pixel_id");
        Object pcode = data.get("pixel_code");
        pixel.setPixelId(pid != null ? pid.toString() : null);
        pixel.setPixelCode(pcode != null ? pcode.toString() : null);
        pixel.setStatus("active");
        pixelMapper.insert(pixel);
        log.info("Created pixel: {} ({})", pixel.getPixelId(), pixel.getPixelName());
        return getPixelByPixelId(pixel.getPixelId());
    }

    /** 与 {@link #savePixel(TikTokPixel)} 并存：仅按 ID 局部更新名称/状态 */
    @Transactional
    public TikTokPixel updatePixel(Long id, TikTokPixel patch) {
        TikTokPixel existing = getPixelById(id);
        if (patch.getPixelName() != null) {
            existing.setPixelName(patch.getPixelName());
        }
        if (patch.getStatus() != null) {
            existing.setStatus(patch.getStatus());
        }
        pixelMapper.update(existing);
        log.info("Updated pixel: {}", id);
        return getPixelById(id);
    }

    @Transactional
    public TikTokPixel savePixel(TikTokPixel pixel) {
        if (pixel.getId() == null) {
            pixelMapper.insert(pixel);
            return getPixelByPixelId(pixel.getPixelId());
        }
        pixelMapper.update(pixel);
        return getPixelById(pixel.getId());
    }

    @Transactional
    public void updatePixelStatus(String pixelId, String status) {
        getPixelByPixelId(pixelId);
        pixelMapper.updateStatus(pixelId, status);
    }

    @Transactional
    public void deletePixel(Long id) {
        getPixelById(id);
        pixelMapper.deleteById(id);
    }

    @Transactional
    public void deletePixelByPixelId(String pixelId) {
        pixelMapper.deleteByPixelId(pixelId);
        log.info("Deleted pixel: {}", pixelId);
    }

    private TikTokPixel mapToPixel(String advertiserId, Map<String, Object> data) {
        return TikTokPixel.builder()
                .advertiserId(advertiserId)
                .pixelId(str(data.get("pixel_id")))
                .pixelName(str(data.get("pixel_name")))
                .pixelCode(str(data.get("pixel_code")))
                .status("active")
                .build();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
