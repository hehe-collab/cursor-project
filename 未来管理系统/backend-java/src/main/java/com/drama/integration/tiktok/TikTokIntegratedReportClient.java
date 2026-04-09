package com.drama.integration.tiktok;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.drama.config.TikTokIntegrationProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * TikTok Marketing API v1.3 — 同步报表 {@code GET /open_api/v1.3/report/integrated/get/}。
 *
 * <p>参数形态对齐官方 Open API（见 tiktok-business-api-sdk {@code ReportingApi#reportIntegratedGet}）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TikTokIntegratedReportClient {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final TikTokIntegrationProperties props;

    /**
     * 使用配置中的单广告主 Token（兼容环境变量旧版）。
     */
    public List<TiktokCampaignReportRow> fetchAuctionCampaignDay(LocalDate day, int pageSize) {
        String token = props.getAdvertiserAccessToken() != null ? props.getAdvertiserAccessToken().trim() : "";
        String adv = props.getAdvertiserId() != null ? props.getAdvertiserId().trim() : "";
        if (token.isEmpty() || adv.isEmpty()) {
            log.warn("TikTok report skipped: missing advertiserAccessToken or advertiserId");
            return List.of();
        }
        return fetchAuctionCampaignDay(adv, token, day, pageSize);
    }

    /**
     * 拉取指定广告主、指定日、竞价推广系列粒度的消耗与曝光（分页汇总）。
     */
    public List<TiktokCampaignReportRow> fetchAuctionCampaignDay(
            String advertiserId, String accessToken, LocalDate day, int pageSize) {
        String adv = advertiserId != null ? advertiserId.trim() : "";
        String token = accessToken != null ? accessToken.trim() : "";
        if (adv.isEmpty() || token.isEmpty()) {
            log.warn("TikTok report skipped: missing advertiserId or accessToken");
            return List.of();
        }

        String dayStr = day.format(DAY);
        List<TiktokCampaignReportRow> all = new ArrayList<>();
        int page = 1;
        while (true) {
            URI uri = buildReportUri(adv, dayStr, dayStr, page, pageSize);
            String body = httpGet(uri, token);
            JSONObject root = JSON.parseObject(body);
            int code = root.getIntValue("code", -1);
            if (code != 0) {
                String msg = root.getString("message");
                log.error("TikTok report/integrated/get error code={} message={}", code, msg);
                throw new IllegalStateException("TikTok API error: " + code + " " + msg);
            }
            JSONObject data = root.getJSONObject("data");
            if (data == null) {
                break;
            }
            JSONArray list = data.getJSONArray("list");
            if (list == null || list.isEmpty()) {
                break;
            }
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                JSONObject dimensions = item.getJSONObject("dimensions");
                JSONObject metrics = item.getJSONObject("metrics");
                if (dimensions == null || metrics == null) {
                    continue;
                }
                String campaignId = dimensions.getString("campaign_id");
                String campaignName = dimensions.getString("campaign_name");
                String statDay = dimensions.getString("stat_time_day");
                BigDecimal spend = parseDecimal(metrics.get("spend"));
                long impressions = parseLong(metrics.get("impressions"));
                all.add(
                        TiktokCampaignReportRow.builder()
                                .campaignId(campaignId)
                                .campaignName(campaignName != null ? campaignName : "")
                                .statTimeDay(statDay != null ? statDay : dayStr)
                                .spend(spend != null ? spend : BigDecimal.ZERO)
                                .impressions(impressions)
                                .build());
            }
            JSONObject pageInfo = data.getJSONObject("page_info");
            int totalPage = pageInfo != null ? pageInfo.getIntValue("total_page", 0) : 0;
            if (totalPage > 0 && page >= totalPage) {
                break;
            }
            if (list.size() < pageSize) {
                break;
            }
            page++;
        }
        return all;
    }

    private URI buildReportUri(String advertiserId, String startDate, String endDate, int page, int pageSize) {
        String dimensionsJson = "[\"campaign_id\",\"campaign_name\",\"stat_time_day\"]";
        String metricsJson = "[\"spend\",\"impressions\"]";
        return UriComponentsBuilder.fromUriString(props.getBaseUrl().replaceAll("/$", "") + "/report/integrated/get/")
                .queryParam("advertiser_id", advertiserId)
                .queryParam("report_type", props.getReportType())
                .queryParam("service_type", props.getServiceType())
                .queryParam("data_level", props.getDataLevel())
                .queryParam("dimensions", dimensionsJson)
                .queryParam("metrics", metricsJson)
                .queryParam("start_date", startDate)
                .queryParam("end_date", endDate)
                .queryParam("page", page)
                .queryParam("page_size", pageSize)
                .build(true)
                .toUri();
    }

    private String httpGet(URI uri, String accessToken) {
        try {
            HttpClient client =
                    HttpClient.newBuilder().connectTimeout(Duration.ofMillis(props.getHttpTimeoutMs())).build();
            HttpRequest req =
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofMillis(props.getHttpTimeoutMs()))
                            .header("Access-Token", accessToken)
                            .GET()
                            .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new IllegalStateException("HTTP " + res.statusCode() + ": " + res.body());
            }
            return res.body();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("TikTok HTTP failed: " + e.getMessage(), e);
        }
    }

    private static BigDecimal parseDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String s = v.toString().trim();
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static long parseLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        String s = v.toString().trim().replace(",", "");
        if (s.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
