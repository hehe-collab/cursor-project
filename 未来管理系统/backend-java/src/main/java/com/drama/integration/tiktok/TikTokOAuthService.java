package com.drama.integration.tiktok;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.drama.config.TikTokIntegrationProperties;
import com.drama.entity.TikTokAccount;
import com.drama.mapper.TikTokAccountMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * TikTok Marketing API v1.3 OAuth：换票与 Refresh（指令 #096）。
 *
 * <p>文档入口：<a href="https://business-api.tiktok.com/portal/docs/about-the-guide/v1.3">v1.3 Guide</a>。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TikTokOAuthService {

    private final TikTokIntegrationProperties props;
    private final TikTokAccountMapper tikTokAccountMapper;

    /** 授权页 URL（用户浏览器打开） */
    public String getAuthorizeUrl(String state) {
        if (!StringUtils.hasText(props.getAppId())) {
            throw new IllegalStateException("未配置 drama.tiktok.app-id (TIKTOK_APP_ID)");
        }
        if (!StringUtils.hasText(props.getRedirectUri())) {
            throw new IllegalStateException("未配置 drama.tiktok.redirect-uri (TIKTOK_REDIRECT_URI)");
        }
        String s = StringUtils.hasText(state) ? state : "state";
        long rid = System.currentTimeMillis();
        return "https://business-api.tiktok.com/portal/auth?app_id="
                + java.net.URLEncoder.encode(props.getAppId(), StandardCharsets.UTF_8)
                + "&state="
                + java.net.URLEncoder.encode(s, StandardCharsets.UTF_8)
                + "&redirect_uri="
                + java.net.URLEncoder.encode(props.getRedirectUri(), StandardCharsets.UTF_8)
                + "&rid="
                + rid;
    }

    /**
     * 用授权码换 Token，并写入 {@code tiktok_accounts}（支持 {@code advertiser_ids} 多账户或单个 {@code advertiser_id}）。
     */
    public List<TikTokAccount> exchangeAuthCode(String authCode) {
        if (!StringUtils.hasText(props.getAppId()) || !StringUtils.hasText(props.getAppSecret())) {
            throw new IllegalStateException("未配置 TIKTOK_APP_ID / TIKTOK_APP_SECRET");
        }
        if (!StringUtils.hasText(authCode)) {
            throw new IllegalArgumentException("auth_code 为空");
        }

        JSONObject body = new JSONObject();
        body.put("app_id", props.getAppId());
        body.put("secret", props.getAppSecret());
        body.put("auth_code", authCode.trim());

        String url = trimTrailingSlash(props.getBaseUrl()) + "/oauth2/access_token/";
        JSONObject root = postJson(url, body.toString());
        int code = root.getIntValue("code", -1);
        if (code != 0) {
            throw new IllegalStateException("TikTok OAuth access_token: " + code + " " + root.getString("message"));
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            throw new IllegalStateException("TikTok OAuth 响应缺少 data");
        }

        String accessToken = data.getString("access_token");
        String refreshToken = data.getString("refresh_token");
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("TikTok OAuth 响应缺少 access_token");
        }
        int expiresIn = data.getIntValue("expires_in", 0);
        LocalDateTime exp =
                expiresIn > 0 ? LocalDateTime.now().plusSeconds(expiresIn) : LocalDateTime.now().plusHours(23);

        Set<String> advertiserIds = new LinkedHashSet<>();
        JSONArray arr = data.getJSONArray("advertiser_ids");
        if (arr != null && !arr.isEmpty()) {
            for (int i = 0; i < arr.size(); i++) {
                String id = arr.getString(i);
                if (StringUtils.hasText(id)) {
                    advertiserIds.add(id.trim());
                }
            }
        }
        String single = data.getString("advertiser_id");
        if (StringUtils.hasText(single)) {
            advertiserIds.add(single.trim());
        }
        if (advertiserIds.isEmpty()) {
            throw new IllegalStateException(
                    "TikTok OAuth 响应未包含 advertiser_id / advertiser_ids，请确认应用授权范围与返回结构");
        }

        String sharedName = data.getString("advertiser_name");
        List<TikTokAccount> saved = new ArrayList<>();
        for (String advId : advertiserIds) {
            TikTokAccount row = new TikTokAccount();
            row.setAdvertiserId(advId);
            row.setAdvertiserName(sharedName);
            row.setAccessToken(accessToken);
            row.setRefreshToken(refreshToken);
            row.setTokenExpiresAt(exp);
            row.setStatus("active");
            tikTokAccountMapper.upsert(row);
            TikTokAccount loaded = tikTokAccountMapper.selectByAdvertiserId(advId);
            if (loaded != null) {
                saved.add(loaded);
            }
            log.info("[TikTok OAuth] upsert advertiser_id={}", advId);
        }
        return saved;
    }

    /** 按广告主刷新 Access Token */
    public TikTokAccount refreshAccessToken(String advertiserId) {
        TikTokAccount account = tikTokAccountMapper.selectByAdvertiserId(advertiserId);
        if (account == null) {
            throw new IllegalArgumentException("未找到广告主: " + advertiserId);
        }
        if (!StringUtils.hasText(account.getRefreshToken())) {
            throw new IllegalStateException("广告主 " + advertiserId + " 无 refresh_token，请重新授权");
        }
        if (!StringUtils.hasText(props.getAppId()) || !StringUtils.hasText(props.getAppSecret())) {
            throw new IllegalStateException("未配置 TIKTOK_APP_ID / TIKTOK_APP_SECRET");
        }

        JSONObject body = new JSONObject();
        body.put("app_id", props.getAppId());
        body.put("secret", props.getAppSecret());
        body.put("refresh_token", account.getRefreshToken().trim());

        String url = trimTrailingSlash(props.getBaseUrl()) + "/oauth2/refresh_token/";
        JSONObject root = postJson(url, body.toString());
        int code = root.getIntValue("code", -1);
        if (code != 0) {
            throw new IllegalStateException("TikTok OAuth refresh: " + code + " " + root.getString("message"));
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            throw new IllegalStateException("TikTok OAuth refresh 响应缺少 data");
        }
        String accessToken = data.getString("access_token");
        String newRefresh = data.getString("refresh_token");
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("TikTok refresh 响应缺少 access_token");
        }
        int expiresIn = data.getIntValue("expires_in", 0);
        LocalDateTime exp =
                expiresIn > 0 ? LocalDateTime.now().plusSeconds(expiresIn) : LocalDateTime.now().plusHours(23);

        account.setAccessToken(accessToken);
        if (StringUtils.hasText(newRefresh)) {
            account.setRefreshToken(newRefresh);
        }
        account.setTokenExpiresAt(exp);
        tikTokAccountMapper.updateTokens(account);
        log.info("[TikTok OAuth] refreshed advertiser_id={}", advertiserId);
        return tikTokAccountMapper.selectByAdvertiserId(advertiserId);
    }

    /** 距离过期不足 1 小时（或已过期）时尝试刷新 */
    public void checkAndRefreshToken(TikTokAccount account) {
        if (account == null || !StringUtils.hasText(account.getAdvertiserId())) {
            return;
        }
        LocalDateTime exp = account.getTokenExpiresAt();
        if (exp == null) {
            return;
        }
        LocalDateTime threshold = LocalDateTime.now().plusHours(1);
        if (exp.isAfter(threshold)) {
            return;
        }
        try {
            log.info("[TikTok OAuth] token 即将过期，刷新 advertiser_id={}", account.getAdvertiserId());
            refreshAccessToken(account.getAdvertiserId());
        } catch (Exception e) {
            log.error("[TikTok OAuth] 刷新失败 advertiser_id={}: {}", account.getAdvertiserId(), e.getMessage());
        }
    }

    private static String trimTrailingSlash(String base) {
        if (base == null) {
            return "";
        }
        return base.replaceAll("/+$", "");
    }

    private JSONObject postJson(String url, String jsonBody) {
        try {
            HttpClient client =
                    HttpClient.newBuilder()
                            .connectTimeout(Duration.ofMillis(props.getHttpTimeoutMs()))
                            .build();
            HttpRequest req =
                    HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofMillis(props.getHttpTimeoutMs()))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                            .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new IllegalStateException("HTTP " + res.statusCode() + ": " + res.body());
            }
            return JSON.parseObject(res.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("TikTok OAuth HTTP 失败: " + e.getMessage(), e);
        }
    }
}
