package com.drama.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TikTok Marketing API（Open API v1.3）接入配置。
 *
 * <p>官方文档总览：<a href="https://business-api.tiktok.com/portal/docs/about-the-guide/v1.3">About the guide v1.3</a>。
 * 同步报表使用：<a href="https://business-api.tiktok.com/portal/docs?id=1740302848100353">Report Integrated Get</a>（GET）。
 */
@Data
@ConfigurationProperties(prefix = "drama.tiktok")
public class TikTokIntegrationProperties {

    /** 为 true 时与国内既有逻辑一致：仅跑 Mock，不调 TikTok */
    private boolean mockEnabled = true;

    /** 若真实请求失败，是否回退到 Mock（默认 false，避免掩盖生产问题） */
    private boolean fallbackMockOnError = false;

    private String baseUrl = "https://business-api.tiktok.com/open_api/v1.3";

    private String appId = "";

    private String appSecret = "";

    /**
     * OAuth 授权完成后的回调地址（须与 TikTok 应用后台配置一致，含端口；默认对齐本服务 {@code server.port} 常见值 3001）。
     */
    private String redirectUri = "http://localhost:3001/api/tiktok/oauth/callback";

    /**
     * 广告主维度 Access Token（OAuth 或开发者工具获取；勿提交仓库，用环境变量注入）。
     */
    private String advertiserAccessToken = "";

    /** TikTok 广告主 ID（BC 授权下的 advertiser_id） */
    private String advertiserId = "";

    private int httpTimeoutMs = 30000;

    /** Report Integrated Get — report_type，常见 BASIC */
    private String reportType = "BASIC";

    private String serviceType = "AUCTION";

    /** 推广系列粒度，与 Monica/常见竞价报表一致 */
    private String dataLevel = "AUCTION_CAMPAIGN";

    /** 同步拉取报表时的分页大小（最大受官方限制） */
    private int reportPageSize = 1000;
}
