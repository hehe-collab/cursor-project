package com.drama.service;

import com.drama.dto.TikTokApiResponseDTO;
import com.drama.entity.TikTokConversionLog;
import com.drama.mapper.TikTokConversionLogMapper;
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

/**
 * TikTok 回传日志与事件发送（事件 API 路径/字段需与当前 TikTok 文档核对后调整）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TikTokConversionService {

    private final TikTokConversionLogMapper conversionLogMapper;
    private final TikTokAccountService accountService;
    private final TikTokApiClient apiClient;

    private static final TypeReference<TikTokApiResponseDTO<Map<String, Object>>> MAP_RESPONSE =
            new TypeReference<>() {};

    public List<TikTokConversionLog> getConversionLogs(String advertiserId, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * pageSize;
        if (StringUtils.hasText(advertiserId)) {
            return conversionLogMapper.selectByAdvertiserId(advertiserId, pageSize, offset);
        }
        return List.of();
    }

    public TikTokConversionLog getConversionLogById(Long id) {
        return getById(id);
    }

    public TikTokConversionLog getById(Long id) {
        TikTokConversionLog row = conversionLogMapper.selectById(id);
        if (row == null) {
            throw new IllegalStateException("Conversion log not found: " + id);
        }
        return row;
    }

    /** 不存在时返回 {@code null}（用于幂等去重） */
    public TikTokConversionLog getConversionLogByEventId(String eventId) {
        return conversionLogMapper.selectByEventId(eventId);
    }

    public TikTokConversionLog getByEventId(String eventId) {
        TikTokConversionLog row = conversionLogMapper.selectByEventId(eventId);
        if (row == null) {
            throw new IllegalStateException("Conversion log not found: eventId=" + eventId);
        }
        return row;
    }

    @Transactional
    public TikTokConversionLog createLog(TikTokConversionLog logRow) {
        if (logRow.getRetryCount() == null) {
            logRow.setRetryCount(0);
        }
        if (logRow.getStatus() == null) {
            logRow.setStatus("pending");
        }
        conversionLogMapper.insert(logRow);
        return conversionLogMapper.selectById(logRow.getId());
    }

    @Transactional
    public TikTokConversionLog sendConversionEvent(TikTokConversionLog conversionLog) {
        if (conversionLog.getEventId() == null || conversionLog.getEventId().isBlank()) {
            throw new IllegalArgumentException("eventId 必填");
        }
        TikTokConversionLog existing = conversionLogMapper.selectByEventId(conversionLog.getEventId());
        if (existing != null) {
            log.warn("Conversion event already exists: {}", conversionLog.getEventId());
            return existing;
        }

        String accessToken = accountService.getValidAccessToken(conversionLog.getAdvertiserId());
        Map<String, Object> body = buildConversionBody(conversionLog);

        conversionLog.setStatus("pending");
        if (conversionLog.getRetryCount() == null) {
            conversionLog.setRetryCount(0);
        }

        try {
            TikTokApiResponseDTO<Map<String, Object>> response =
                    apiClient.post(
                            conversionLog.getAdvertiserId(),
                            accessToken,
                            "event/track/",
                            body,
                            MAP_RESPONSE);
            if (response.isSuccess()) {
                conversionLog.setStatus("success");
                conversionLog.setResponseCode(response.getCode());
                conversionLog.setResponseMessage(response.getMessage());
            } else {
                conversionLog.setStatus("failed");
                conversionLog.setResponseCode(response.getCode());
                conversionLog.setResponseMessage(response.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send conversion event: {}", e.getMessage(), e);
            conversionLog.setStatus("failed");
            conversionLog.setResponseMessage(e.getMessage());
        }

        conversionLogMapper.insert(conversionLog);
        log.info("Sent conversion event: {} - Status: {}", conversionLog.getEventId(), conversionLog.getStatus());
        return conversionLogMapper.selectById(conversionLog.getId());
    }

    @Transactional
    public void retryFailedConversions(int limit) {
        List<TikTokConversionLog> failedLogs = conversionLogMapper.selectPendingRetry(limit);
        for (TikTokConversionLog conv : failedLogs) {
            try {
                String accessToken = accountService.getValidAccessToken(conv.getAdvertiserId());
                Map<String, Object> body = buildConversionBody(conv);
                TikTokApiResponseDTO<Map<String, Object>> response =
                        apiClient.post(conv.getAdvertiserId(), accessToken, "event/track/", body, MAP_RESPONSE);
                if (response.isSuccess()) {
                    conv.setStatus("success");
                    conv.setResponseCode(response.getCode());
                    conv.setResponseMessage(response.getMessage());
                } else {
                    conv.setStatus("failed");
                    conv.setResponseCode(response.getCode());
                    conv.setResponseMessage(response.getMessage());
                    conv.setRetryCount((conv.getRetryCount() != null ? conv.getRetryCount() : 0) + 1);
                }
            } catch (Exception e) {
                log.error("Failed to retry conversion event: {} - {}", conv.getEventId(), e.getMessage());
                conv.setStatus("failed");
                conv.setResponseMessage(e.getMessage());
                conv.setRetryCount((conv.getRetryCount() != null ? conv.getRetryCount() : 0) + 1);
            }
            conversionLogMapper.update(conv);
        }
        log.info("Retried {} failed conversion events", failedLogs.size());
    }

    /** 与 Mapper 一致：仅统计 status=success 条数 */
    public int countConversions(String advertiserId, String eventType) {
        return conversionLogMapper.countSuccessByAdvertiserAndEventType(advertiserId, eventType);
    }

    public List<TikTokConversionLog> listPendingRetry(int limit) {
        return conversionLogMapper.selectPendingRetry(limit);
    }

    @Transactional
    public TikTokConversionLog updateLog(TikTokConversionLog row) {
        conversionLogMapper.update(row);
        return getById(row.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        getById(id);
        conversionLogMapper.deleteById(id);
    }

    private Map<String, Object> buildConversionBody(TikTokConversionLog conv) {
        Map<String, Object> body = new HashMap<>();
        body.put("pixel_code", conv.getPixelId());
        body.put("event", conv.getEventType());
        body.put("event_id", conv.getEventId());
        if (conv.getEventTime() != null) {
            body.put("timestamp", conv.getEventTime().toString());
        }
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        if (conv.getExternalId() != null) {
            user.put("external_id", conv.getExternalId());
        }
        if (conv.getClickId() != null) {
            user.put("ttclid", conv.getClickId());
        }
        context.put("user", user);
        body.put("context", context);

        Map<String, Object> properties = new HashMap<>();
        if (conv.getEventValue() != null) {
            properties.put("value", conv.getEventValue());
        }
        if (conv.getCurrency() != null) {
            properties.put("currency", conv.getCurrency());
        }
        if (conv.getContentType() != null) {
            properties.put("content_type", conv.getContentType());
        }
        if (conv.getContentId() != null) {
            properties.put("content_id", conv.getContentId());
        }
        body.put("properties", properties);
        return body;
    }
}
