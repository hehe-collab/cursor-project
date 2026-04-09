package com.drama.config;

import java.nio.charset.StandardCharsets;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * TikTok OAuth 2.0 相关 URL 与回调配置（前缀 {@code tiktok.oauth}）。
 */
@Data
@ConfigurationProperties(prefix = "tiktok.oauth")
public class TikTokOAuthConfig {

    private String authUrl = "https://business-api.tiktok.com/portal/auth";
    private String tokenUrl = "https://business-api.tiktok.com/open_api/v1.3/oauth2/access_token/";
    private String refreshTokenUrl = "https://business-api.tiktok.com/open_api/v1.3/oauth2/refresh_token/";

    /** 须在 TikTok 应用后台与白名单一致 */
    private String redirectUri = "http://localhost:3001/api/tiktok/oauth/callback";

    private String scope = "user.info.basic,ad.management,reporting";

    /**
     * 构造浏览器授权 URL（参数 URL 编码）。
     *
     * <p>说明：线上授权入口以官方文档为准；若需 {@code rid} 等额外参数可在 Controller 层拼接。
     */
    public String getAuthorizationUrl(String appId, String state) {
        String s = state != null ? state : "";
        return UriComponentsBuilder.fromUriString(authUrl)
                .queryParam("app_id", appId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", s)
                .queryParam("scope", scope)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }
}
