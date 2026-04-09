package com.drama.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TikTok Marketing API 基础配置（前缀 {@code tiktok.api}）。
 *
 * <p>与既有 {@link TikTokIntegrationProperties}（{@code drama.tiktok}）并存：OAuth 实现仍优先读 {@code drama.tiktok}；
 * 本类供 {@link com.drama.util.TikTokApiClient} 等统一拼 URL、超时与重试参数。
 */
@Data
@ConfigurationProperties(prefix = "tiktok.api")
public class TikTokConfig {

    /** TikTok App ID */
    private String appId = "";

    /** TikTok App Secret */
    private String appSecret = "";

    /**
     * API 根域名（不含 {@code /open_api/...}）。
     *
     * <p>默认：<a href="https://business-api.tiktok.com">business-api.tiktok.com</a>
     */
    private String baseUrl = "https://business-api.tiktok.com";

    /** Open API 路径中的版本段，如 v1.3 */
    private String version = "v1.3";

    /** 连接/读取超时（毫秒） */
    private Integer timeout = 30000;

    /** 是否允许通过 {@link com.drama.util.TikTokApiClient} 发起请求 */
    private Boolean enabled = true;

    /** 是否打印请求/响应摘要日志 */
    private Boolean debug = false;

    /** Access Token 过期前提前刷新的时间窗口（分钟） */
    private Integer tokenRefreshAdvanceMinutes = 5;

    /** 失败重试次数（不含首次请求） */
    private Integer maxRetries = 3;

    /** 重试间隔（毫秒） */
    private Integer retryInterval = 1000;

    /**
     * 组装完整 Open API URL，例如 {@code report/integrated/get/} →
     * {@code https://business-api.tiktok.com/open_api/v1.3/report/integrated/get/}
     */
    public String getFullApiUrl(String endpoint) {
        String path = endpoint == null ? "" : endpoint.trim();
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        String ver = version == null ? "v1.3" : version.replaceAll("^/+|/+$", "");
        return String.format("%s/open_api/%s/%s", base, ver, path);
    }
}
