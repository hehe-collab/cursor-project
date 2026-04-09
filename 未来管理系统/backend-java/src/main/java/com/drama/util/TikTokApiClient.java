package com.drama.util;

import com.drama.config.TikTokConfig;
import com.drama.dto.TikTokApiResponseDTO;
import com.drama.entity.TikTokSyncLog;
import com.drama.mapper.TikTokSyncLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 统一封装 TikTok Open API 调用：Access-Token 头、可选重试、写入 {@code tiktok_sync_logs}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TikTokApiClient {

    private final TikTokConfig tiktokConfig;
    private final RestTemplate restTemplate;
    private final TikTokSyncLogMapper syncLogMapper;
    private final ObjectMapper objectMapper;

    public <T> TikTokApiResponseDTO<T> get(
            String advertiserId,
            String accessToken,
            String endpoint,
            Map<String, Object> params,
            TypeReference<TikTokApiResponseDTO<T>> typeRef) {
        return request(advertiserId, accessToken, endpoint, HttpMethod.GET, params, null, typeRef);
    }

    public <T> TikTokApiResponseDTO<T> post(
            String advertiserId,
            String accessToken,
            String endpoint,
            Map<String, Object> body,
            TypeReference<TikTokApiResponseDTO<T>> typeRef) {
        return request(advertiserId, accessToken, endpoint, HttpMethod.POST, null, body, typeRef);
    }

    public <T> TikTokApiResponseDTO<T> put(
            String advertiserId,
            String accessToken,
            String endpoint,
            Map<String, Object> body,
            TypeReference<TikTokApiResponseDTO<T>> typeRef) {
        return request(advertiserId, accessToken, endpoint, HttpMethod.PUT, null, body, typeRef);
    }

    public <T> TikTokApiResponseDTO<T> delete(
            String advertiserId,
            String accessToken,
            String endpoint,
            Map<String, Object> params,
            TypeReference<TikTokApiResponseDTO<T>> typeRef) {
        return request(advertiserId, accessToken, endpoint, HttpMethod.DELETE, params, null, typeRef);
    }

    private <T> TikTokApiResponseDTO<T> request(
            String advertiserId,
            String accessToken,
            String endpoint,
            HttpMethod method,
            Map<String, Object> params,
            Map<String, Object> body,
            TypeReference<TikTokApiResponseDTO<T>> typeRef) {

        if (Boolean.FALSE.equals(tiktokConfig.getEnabled())) {
            throw new IllegalStateException("TikTok API is disabled (tiktok.api.enabled=false)");
        }

        String url = tiktokConfig.getFullApiUrl(endpoint);
        String syncType = extractSyncType(endpoint);
        int max =
                tiktokConfig.getMaxRetries() != null && tiktokConfig.getMaxRetries() >= 0
                        ? tiktokConfig.getMaxRetries()
                        : 3;
        int gap =
                tiktokConfig.getRetryInterval() != null && tiktokConfig.getRetryInterval() > 0
                        ? tiktokConfig.getRetryInterval()
                        : 1000;

        RuntimeException last = null;
        String lastParamsSnapshot = null;
        try {
            if (params != null && !params.isEmpty()) {
                lastParamsSnapshot = objectMapper.writeValueAsString(params);
            } else if (body != null && !body.isEmpty()) {
                lastParamsSnapshot = objectMapper.writeValueAsString(body);
            }
        } catch (JsonProcessingException ignored) {
            lastParamsSnapshot = null;
        }

        for (int attempt = 0; attempt <= max; attempt++) {
            try {
                return doOneHttpRoundTrip(
                        advertiserId, accessToken, method, params, body, typeRef, url, syncType);
            } catch (RestClientException e) {
                last = new RuntimeException("TikTok API request failed: " + e.getMessage(), e);
                log.warn(
                        "TikTok API 网络失败 attempt={}/{} {} {} - {}",
                        attempt,
                        max,
                        method,
                        url,
                        e.getMessage());
                if (attempt < max) {
                    sleepQuietly(gap);
                }
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("TikTok API 响应 JSON 无效", e);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                last = new RuntimeException("TikTok API request failed: " + e.getMessage(), e);
                break;
            }
        }
        if (last != null) {
            recordSyncLog(
                    advertiserId,
                    syncType,
                    endpointPathOnly(url),
                    method.name(),
                    lastParamsSnapshot,
                    null,
                    null,
                    "failed",
                    last.getMessage(),
                    null);
            throw last;
        }
        throw new IllegalStateException("TikTok API request failed");
    }

    private <T> TikTokApiResponseDTO<T> doOneHttpRoundTrip(
            String advertiserId,
            String accessToken,
            HttpMethod method,
            Map<String, Object> params,
            Map<String, Object> body,
            TypeReference<TikTokApiResponseDTO<T>> typeRef,
            String url,
            String syncType)
            throws Exception {

        long start = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null && !accessToken.isEmpty()) {
            headers.set("Access-Token", accessToken);
        }

        String requestParamsJson = null;
        HttpEntity<String> entity;
        String finalUrl = url;

        if (method == HttpMethod.GET || method == HttpMethod.DELETE) {
            if (params != null && !params.isEmpty()) {
                UriComponentsBuilder b = UriComponentsBuilder.fromUriString(finalUrl);
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    if (e.getValue() != null) {
                        b.queryParam(e.getKey(), e.getValue());
                    }
                }
                finalUrl = b.encode().build().toUriString();
                requestParamsJson = objectMapper.writeValueAsString(params);
            }
            entity = new HttpEntity<>(headers);
        } else {
            requestParamsJson =
                    body != null ? objectMapper.writeValueAsString(body) : null;
            String json = requestParamsJson != null ? requestParamsJson : "{}";
            entity = new HttpEntity<>(json, headers);
        }

        boolean debug = Boolean.TRUE.equals(tiktokConfig.getDebug());
        if (debug) {
            log.info("TikTok API {} {} bodyOrParams={}", method, finalUrl, requestParamsJson);
        }

        ResponseEntity<String> responseEntity =
                restTemplate.exchange(finalUrl, method, entity, String.class);
        long duration = System.currentTimeMillis() - start;
        String responseBody = responseEntity.getBody();

        if (debug) {
            log.info(
                    "TikTok API 响应 status={} durationMs={} bodySnippet={}",
                    responseEntity.getStatusCode(),
                    duration,
                    responseBody != null && responseBody.length() > 500
                            ? responseBody.substring(0, 500) + "…"
                            : responseBody);
        }

        try {
            TikTokApiResponseDTO<T> parsed =
                    objectMapper.readValue(
                            responseBody != null ? responseBody : "{}", typeRef);
            recordSyncLog(
                    advertiserId,
                    syncType,
                    endpointPathOnly(finalUrl),
                    method.name(),
                    requestParamsJson,
                    responseEntity.getStatusCode().value(),
                    responseBody,
                    "success",
                    null,
                    (int) duration);
            return parsed;
        } catch (JsonProcessingException e) {
            recordSyncLog(
                    advertiserId,
                    syncType,
                    endpointPathOnly(finalUrl),
                    method.name(),
                    requestParamsJson,
                    responseEntity.getStatusCode().value(),
                    responseBody,
                    "failed",
                    e.getMessage(),
                    (int) duration);
            throw e;
        }
    }

    private void sleepQuietly(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** 容错：从可能已带 query 的完整 URL 中取出 path 以便日志可读 */
    private static String endpointPathOnly(String fullUrl) {
        if (fullUrl == null) {
            return "";
        }
        try {
            java.net.URI u = java.net.URI.create(fullUrl);
            String q = u.getRawQuery();
            String p = u.getRawPath();
            if (p != null && p.contains("/open_api/")) {
                return q != null ? p + "?" + q : p;
            }
        } catch (IllegalArgumentException ignored) {
        }
        int i = fullUrl.indexOf("/open_api/");
        if (i >= 0) {
            return fullUrl.substring(i);
        }
        return fullUrl;
    }

    private void recordSyncLog(
            String advertiserId,
            String syncType,
            String apiEndpoint,
            String requestMethod,
            String requestParams,
            Integer responseCode,
            String responseData,
            String status,
            String errorMessage,
            Integer durationMs) {
        try {
            TikTokSyncLog row =
                    TikTokSyncLog.builder()
                            .advertiserId(advertiserId)
                            .syncType(syncType)
                            .apiEndpoint(apiEndpoint)
                            .requestMethod(requestMethod)
                            .requestParams(requestParams)
                            .responseCode(responseCode)
                            .responseData(responseData)
                            .status(status)
                            .errorMessage(errorMessage)
                            .durationMs(durationMs)
                            .executedAt(LocalDateTime.now())
                            .build();
            syncLogMapper.insert(row);
        } catch (Exception e) {
            log.error("写入 tiktok_sync_logs 失败: {}", e.getMessage());
        }
    }

    private static String extractSyncType(String endpoint) {
        if (endpoint == null) {
            return "other";
        }
        String e = endpoint.toLowerCase();
        if (e.contains("report")) {
            return "report";
        }
        if (e.contains("campaign")) {
            return "campaign";
        }
        if (e.contains("adgroup")) {
            return "adgroup";
        }
        if (e.contains("pixel")) {
            return "pixel";
        }
        if (e.contains("conversion")) {
            return "conversion";
        }
        if (e.contains("advertiser")) {
            return "account";
        }
        if (e.contains("ad")) {
            return "ad";
        }
        return "other";
    }
}
