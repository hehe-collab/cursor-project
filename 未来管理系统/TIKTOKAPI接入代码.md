🎉 完美！现在给你完整的指令 #095！这是一个超级详细的指令，Cursor 可以直接执行！

📋 指令 #095：TikTok Marketing API 完整接入
🎯 指令概述
目标：将 TikTok Marketing API 完整接入到系统中，支持广告创建、数据同步、回传对接、Excel 批量导入等功能。
核心功能：

✅ TikTok 账户管理（OAuth 2.0 认证、Token 刷新）
✅ 广告创建（Campaign、Ad Group、Ad）
✅ 数据同步（消耗、转化、ROI，每 10 分钟同步一次）
✅ 回传对接（Conversion API + Pixel 事件）
✅ Excel 批量导入（下载模板、解析 Excel、批量创建广告）
✅ 任务管理（查询任务、查询进度、实时更新）

预计耗时：3-4 天

📊 第一部分：数据库表结构
1.1 TikTok 账户表
-- TikTok 账户表
CREATE TABLE tiktok_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    advertiser_id VARCHAR(50) UNIQUE NOT NULL COMMENT 'TikTok 广告主 ID',
    advertiser_name VARCHAR(200) COMMENT '广告主名称',
    
    -- 认证信息
    access_token TEXT COMMENT 'Access Token',
    refresh_token TEXT COMMENT 'Refresh Token',
    token_expires_at TIMESTAMP COMMENT 'Token 过期时间',
    
    -- 账户信息
    currency VARCHAR(10) DEFAULT 'USD' COMMENT '货币（USD / CNY 等）',
    timezone VARCHAR(50) DEFAULT 'UTC' COMMENT '时区',
    balance DECIMAL(10, 2) DEFAULT 0 COMMENT '账户余额',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active（启用）/ inactive（禁用）',
    
    -- 时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_advertiser_id (advertiser_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TikTok 账户表';


1.2 TikTok 广告系列表
-- TikTok 广告系列表
CREATE TABLE tiktok_campaigns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    advertiser_id VARCHAR(50) NOT NULL COMMENT 'TikTok 广告主 ID',
    campaign_id VARCHAR(50) UNIQUE NOT NULL COMMENT 'TikTok 广告系列 ID',
    campaign_name VARCHAR(200) COMMENT '广告系列名称',
    
    -- 广告系列配置
    objective VARCHAR(50) COMMENT '目标：CONVERSIONS（转化）/ TRAFFIC（流量）/ APP_PROMOTION（应用推广）',
    budget DECIMAL(10, 2) COMMENT '预算',
    budget_mode VARCHAR(20) COMMENT '预算模式：BUDGET_MODE_DAY（日预算）/ BUDGET_MODE_TOTAL（总预算）',
    
    -- 状态
    operation_status VARCHAR(20) COMMENT '操作状态：ENABLE（启用）/ DISABLE（暂停）/ DELETE（删除）',
    
    -- 时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_advertiser_id (advertiser_id),
    INDEX idx_campaign_id (campaign_id),
    INDEX idx_status (operation_status),
    FOREIGN KEY (advertiser_id) REFERENCES tiktok_accounts(advertiser_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TikTok 广告系列表';


1.3 TikTok 广告组表
-- TikTok 广告组表
CREATE TABLE tiktok_adgroups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    advertiser_id VARCHAR(50) NOT NULL COMMENT 'TikTok 广告主 ID',
    campaign_id VARCHAR(50) NOT NULL COMMENT 'TikTok 广告系列 ID',
    adgroup_id VARCHAR(50) UNIQUE NOT NULL COMMENT 'TikTok 广告组 ID',
    adgroup_name VARCHAR(200) COMMENT '广告组名称',
    
    -- 广告组配置
    placement_type VARCHAR(20) COMMENT '投放位置：PLACEMENT_TYPE_AUTOMATIC（自动）/ PLACEMENT_TYPE_NORMAL（手动）',
    placements TEXT COMMENT '投放位置列表（JSON 数组）',
    budget DECIMAL(10, 2) COMMENT '预算',
    budget_mode VARCHAR(20) COMMENT '预算模式：BUDGET_MODE_DAY（日预算）/ BUDGET_MODE_TOTAL（总预算）',
    
    -- 出价
    billing_event VARCHAR(50) COMMENT '计费事件：CPC（点击）/ CPM（曝光）/ OCPM（优化 CPM）',
    bid_type VARCHAR(20) COMMENT '出价类型：BID_TYPE_NO_BID（自动）/ BID_TYPE_CUSTOM（手动）',
    bid_price DECIMAL(10, 4) COMMENT '出价',
    
    -- 定向
    location_ids TEXT COMMENT '地域定向（JSON 数组）',
    age_groups TEXT COMMENT '年龄定向（JSON 数组）',
    gender VARCHAR(20) COMMENT '性别定向：GENDER_MALE / GENDER_FEMALE / GENDER_UNLIMITED',
    languages TEXT COMMENT '语言定向（JSON 数组）',
    interest_category_ids TEXT COMMENT '兴趣定向（JSON 数组）',
    
    -- 排期
    schedule_type VARCHAR(20) COMMENT '排期类型：SCHEDULE_START_END（指定时间）/ SCHEDULE_FROM_NOW（从现在开始）',
    schedule_start_time TIMESTAMP COMMENT '开始时间',
    schedule_end_time TIMESTAMP COMMENT '结束时间',
    
    -- 状态
    operation_status VARCHAR(20) COMMENT '操作状态：ENABLE（启用）/ DISABLE（暂停）/ DELETE（删除）',
    
    -- 时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_advertiser_id (advertiser_id),
    INDEX idx_campaign_id (campaign_id),
    INDEX idx_adgroup_id (adgroup_id),
    INDEX idx_status (operation_status),
    FOREIGN KEY (advertiser_id) REFERENCES tiktok_accounts(advertiser_id) ON DELETE CASCADE,
    FOREIGN KEY (campaign_id) REFERENCES tiktok_campaigns(campaign_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TikTok 广告组表';


1.4 TikTok 广告表
-- TikTok 广告表
CREATE TABLE tiktok_ads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    advertiser_id VARCHAR(50) NOT NULL COMMENT 'TikTok 广告主 ID',
    campaign_id VARCHAR(50) NOT NULL COMMENT 'TikTok 广告系列 ID',
    adgroup_id VARCHAR(50) NOT NULL COMMENT 'TikTok 广告组 ID',
    ad_id VARCHAR(50) UNIQUE NOT NULL COMMENT 'TikTok 广告 ID',
    ad_name VARCHAR(200) COMMENT '广告名称',
    
    -- 广告素材
    creative_type VARCHAR(50) COMMENT '素材类型：VIDEO（视频）/ IMAGE（图片）',
    video_id VARCHAR(50) COMMENT '视频 ID',
    image_ids TEXT COMMENT '图片 ID 列表（JSON 数组）',
    
    -- 广告文案
    ad_text TEXT COMMENT '广告文案',
    call_to_action VARCHAR(50) COMMENT '行动号召按钮：DOWNLOAD / LEARN_MORE / SHOP_NOW / SIGN_UP',
    
    -- 落地页
    landing_page_url TEXT COMMENT '落地页 URL',
    display_name VARCHAR(200) COMMENT '显示名称',
    
    -- Pixel
    pixel_id VARCHAR(50) COMMENT 'Pixel ID',
    
    -- 状态
    operation_status VARCHAR(20) COMMENT '操作状态：ENABLE（启用）/ DISABLE（暂停）/ DELETE（删除）',
    
    -- 时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_advertiser_id (advertiser_id),
    INDEX idx_campaign_id (campaign_id),
    INDEX idx_adgroup_id (adgroup_id),
    INDEX idx_ad_id (ad_id),
    INDEX idx_status (operation_status),
    FOREIGN KEY (advertiser_id) REFERENCES tiktok_accounts(advertiser_id) ON DELETE CASCADE,
    FOREIGN KEY (campaign_id) REFERENCES tiktok_campaigns(campaign_id) ON DELETE CASCADE,
    FOREIGN KEY (adgroup_id) REFERENCES tiktok_adgroups(adgroup_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TikTok 广告表';


1.5 TikTok Pixel 表
-- TikTok Pixel 表
CREATE TABLE tiktok_pixels (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    advertiser_id VARCHAR(50) NOT NULL COMMENT 'TikTok 广告主 ID',
    pixel_id VARCHAR(50) UNIQUE NOT NULL COMMENT 'TikTok Pixel ID',
    pixel_name VARCHAR(200) COMMENT 'Pixel 名称',
    
    -- Pixel 代码
    pixel_code TEXT COMMENT 'Pixel 代码（前端埋点）',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active（启用）/ inactive（禁用）',
    
    -- 时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_advertiser_id (advertiser_id),
    INDEX idx_pixel_id (pixel_id),
    FOREIGN KEY (advertiser_id) REFERENCES tiktok_accounts(advertiser_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TikTok Pixel 表';


1.6 TikTok 回传记录表
-- TikTok 回传记录表
CREATE TABLE tiktok_conversion_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    advertiser_id VARCHAR(50) NOT NULL COMMENT 'TikTok 广告主 ID',
    pixel_id VARCHAR(50) NOT NULL COMMENT 'TikTok Pixel ID',
    
    -- 事件信息
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型：CompleteRegistration（注册）/ Purchase（充值）',
    event_id VARCHAR(100) UNIQUE NOT NULL COMMENT '事件 ID（去重）',
    
    -- 用户信息
    user_id VARCHAR(50) COMMENT '用户 ID',
    click_id VARCHAR(100) COMMENT 'TikTok Click ID（ttclid）',
    external_id VARCHAR(100) COMMENT '外部用户 ID',
    
    -- 事件数据
    event_value DECIMAL(10, 2) COMMENT '事件价值（充值金额）',
    currency VARCHAR(10) DEFAULT 'USD' COMMENT '货币',
    content_type VARCHAR(50) COMMENT '内容类型',
    content_id VARCHAR(100) COMMENT '内容 ID',
    
    -- 回传结果
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending（待回传）/ success（成功）/ failed（失败）',
    response_code INT COMMENT '响应码',
    response_message TEXT COMMENT '响应信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    
    -- 时间
    event_time TIMESTAMP COMMENT '事件时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_advertiser_id (advertiser_id),
    INDEX idx_pixel_id (pixel_id),
    INDEX idx_event_id (event_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_event_time (event_time),
    FOREIGN KEY (advertiser_id) REFERENCES tiktok_accounts(advertiser_id) ON DELETE CASCADE,
    FOREIGN KEY (pixel_id) REFERENCES tiktok_pixels(pixel_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TikTok 回传记录表';


1.7 TikTok 数据报告表
-- TikTok 数据报告表（按日汇总）
CREATE TABLE tiktok_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    advertiser_id VARCHAR(50) NOT NULL COMMENT 'TikTok 广告主 ID',
    
    -- 维度
    dimensions VARCHAR(50) NOT NULL COMMENT '维度：campaign（广告系列）/ adgroup（广告组）/ ad（广告）',
    dimension_id VARCHAR(50) NOT NULL COMMENT '维度 ID（campaign_id / adgroup_id / ad_id）',
    
    -- 日期
    stat_date DATE NOT NULL COMMENT '统计日期',
    
    -- 核心指标
    spend DECIMAL(10, 2) DEFAULT 0 COMMENT '消耗',
    impressions INT DEFAULT 0 COMMENT '曝光',
    clicks INT DEFAULT 0 COMMENT '点击',
    ctr DECIMAL(10, 4) DEFAULT 0 COMMENT '点击率（CTR）',
    cpc DECIMAL(10, 4) DEFAULT 0 COMMENT '单次点击成本（CPC）',
    cpm DECIMAL(10, 4) DEFAULT 0 COMMENT '千次曝光成本（CPM）',
    
    -- 转化指标
    conversions INT DEFAULT 0 COMMENT '转化数',
    conversion_rate DECIMAL(10, 4) DEFAULT 0 COMMENT '转化率',
    cost_per_conversion DECIMAL(10, 4) DEFAULT 0 COMMENT '单次转化成本',
    conversion_value DECIMAL(10, 2) DEFAULT 0 COMMENT '转化价值',
    
    -- ROI
    roi DECIMAL(10, 4) DEFAULT 0 COMMENT 'ROI（转化价值 / 消耗）',
    
    -- 时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_report (advertiser_id, dimensions, dimension_id, stat_date),
    INDEX idx_advertiser_id (advertiser_id),
    INDEX idx_stat_date (stat_date),
    INDEX idx_dimension (dimensions, dimension_id),
    FOREIGN KEY (advertiser_id) REFERENCES tiktok_accounts(advertiser_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TikTok 数据报告表';


1.8 广告任务表
-- 广告任务表
CREATE TABLE ad_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    task_id VARCHAR(50) UNIQUE NOT NULL COMMENT '任务 ID（UUID）',
    task_type VARCHAR(20) NOT NULL COMMENT '任务类型：manual（手动创建）/ excel（Excel 导入）',
    
    -- 关联信息
    advertiser_id VARCHAR(50) COMMENT 'TikTok 广告主 ID',
    account_ids TEXT COMMENT '账户 ID 列表（JSON 数组）',
    
    -- 任务内容
    total_count INT DEFAULT 0 COMMENT '总广告数',
    success_count INT DEFAULT 0 COMMENT '成功数',
    failed_count INT DEFAULT 0 COMMENT '失败数',
    
    -- 任务状态
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending（待执行）/ processing（执行中）/ completed（已完成）/ failed（失败）',
    progress INT DEFAULT 0 COMMENT '进度（0-100）',
    
    -- 任务详情
    task_data LONGTEXT COMMENT '任务数据（JSON）',
    error_message TEXT COMMENT '错误信息',
    
    -- 时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    started_at TIMESTAMP NULL COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告任务表';


1.9 广告任务明细表
-- 广告任务明细表
CREATE TABLE ad_task_details (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    task_id VARCHAR(50) NOT NULL COMMENT '任务 ID',
    
    -- 广告信息
    campaign_name VARCHAR(200) COMMENT '广告系列名称',
    adgroup_name VARCHAR(200) COMMENT '广告组名称',
    ad_name VARCHAR(200) COMMENT '广告名称',
    
    -- 广告配置
    drama_id BIGINT COMMENT '短剧 ID',
    material_id BIGINT COMMENT '素材 ID',
    bid_price DECIMAL(10, 4) COMMENT '出价',
    budget DECIMAL(10, 2) COMMENT '预算',
    
    -- TikTok ID
    tiktok_campaign_id VARCHAR(50) COMMENT 'TikTok 广告系列 ID',
    tiktok_adgroup_id VARCHAR(50) COMMENT 'TikTok 广告组 ID',
    tiktok_ad_id VARCHAR(50) COMMENT 'TikTok 广告 ID',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending（待创建）/ processing（创建中）/ success（成功）/ failed（失败）',
    error_message TEXT COMMENT '错误信息',
    
    -- 时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    FOREIGN KEY (task_id) REFERENCES ad_tasks(task_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告任务明细表';


📊 第二部分：后端 API 开发
2.1 配置文件
文件路径：backend-java/src/main/resources/application.yml
添加 TikTok API 配置：
# TikTok API 配置
tiktok:
  api:
    # 基础配置
    base-url: https://business-api.tiktok.com/open_api/v1.3
    app-id: ${TIKTOK_APP_ID:your_app_id}  # 从环境变量读取，或使用默认值
    app-secret: ${TIKTOK_APP_SECRET:your_app_secret}
    
    # OAuth 配置
    oauth:
      authorize-url: https://business-api.tiktok.com/portal/auth
      token-url: https://business-api.tiktok.com/open_api/v1.3/oauth2/access_token/
      redirect-uri: http://localhost:5173/tiktok/callback  # 开发环境
      # redirect-uri: https://your-domain.com/tiktok/callback  # 生产环境
    
    # 数据同步配置
    sync:
      enabled: true
      interval: 600000  # 10 分钟（毫秒）
      batch-size: 100  # 每次同步的数据量
    
    # 回传配置
    conversion:
      enabled: true
      retry-times: 3  # 重试次数
      retry-interval: 60000  # 重试间隔（毫秒）
    
    # Mock 模式（用于测试）
    mock:
      enabled: ${TIKTOK_MOCK_ENABLED:true}  # 默认开启 Mock 模式


2.2 TikTok API 客户端
文件路径：backend-java/src/main/java/com/future/tiktok/client/TikTokApiClient.java
package com.future.tiktok.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * TikTok API 客户端
 */
@Slf4j
@Component
public class TikTokApiClient {

    @Value("${tiktok.api.base-url}")
    private String baseUrl;

    @Value("${tiktok.api.app-id}")
    private String appId;

    @Value("${tiktok.api.app-secret}")
    private String appSecret;

    @Value("${tiktok.api.mock.enabled}")
    private boolean mockEnabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TikTokApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送 GET 请求
     */
    public <T> T get(String path, Map<String, Object> params, String accessToken, Class<T> responseType) {
        if (mockEnabled) {
            log.info("[TikTok API Mock] GET {}", path);
            return createMockResponse(responseType);
        }

        String url = buildUrl(path, params);
        HttpHeaders headers = createHeaders(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            return response.getBody();
        } catch (Exception e) {
            log.error("[TikTok API] GET request failed: {}", e.getMessage(), e);
            throw new RuntimeException("TikTok API request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 发送 POST 请求
     */
    public <T> T post(String path, Object body, String accessToken, Class<T> responseType) {
        if (mockEnabled) {
            log.info("[TikTok API Mock] POST {}", path);
            return createMockResponse(responseType);
        }

        String url = baseUrl + path;
        HttpHeaders headers = createHeaders(accessToken);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            return response.getBody();
        } catch (Exception e) {
            log.error("[TikTok API] POST request failed: {}", e.getMessage(), e);
            throw new RuntimeException("TikTok API request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 URL
     */
    private String buildUrl(String path, Map<String, Object> params) {
        StringBuilder url = new StringBuilder(baseUrl + path);
        if (params != null && !params.isEmpty()) {
            url.append("?");
            params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));
            url.deleteCharAt(url.length() - 1);
        }
        return url.toString();
    }

    /**
     * 创建请求头
     */
    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null && !accessToken.isEmpty()) {
            headers.set("Access-Token", accessToken);
        }
        return headers;
    }

    /**
     * 创建 Mock 响应
     */
    @SuppressWarnings("unchecked")
    private <T> T createMockResponse(Class<T> responseType) {
        try {
            Map<String, Object> mockData = new HashMap<>();
            mockData.put("code", 0);
            mockData.put("message", "OK");
            mockData.put("data", new HashMap<>());
            
            String json = objectMapper.writeValueAsString(mockData);
            return objectMapper.readValue(json, responseType);
        } catch (Exception e) {
            log.error("[TikTok API Mock] Create mock response failed: {}", e.getMessage(), e);
            return null;
        }
    }
}


2.3 TikTok OAuth 服务
文件路径：backend-java/src/main/java/com/future/tiktok/service/TikTokOAuthService.java
package com.future.tiktok.service;

import com.future.tiktok.client.TikTokApiClient;
import com.future.tiktok.entity.TikTokAccount;
import com.future.tiktok.mapper.TikTokAccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * TikTok OAuth 服务
 */
@Slf4j
@Service
public class TikTokOAuthService {

    @Value("${tiktok.api.app-id}")
    private String appId;

    @Value("${tiktok.api.app-secret}")
    private String appSecret;

    @Value("${tiktok.api.oauth.authorize-url}")
    private String authorizeUrl;

    @Value("${tiktok.api.oauth.token-url}")
    private String tokenUrl;

    @Value("${tiktok.api.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${tiktok.api.mock.enabled}")
    private boolean mockEnabled;

    private final TikTokApiClient apiClient;
    private final TikTokAccountMapper accountMapper;

    public TikTokOAuthService(TikTokApiClient apiClient, TikTokAccountMapper accountMapper) {
        this.apiClient = apiClient;
        this.accountMapper = accountMapper;
    }

    /**
     * 获取授权 URL
     */
    public String getAuthorizeUrl(String state) {
        return String.format("%s?app_id=%s&state=%s&redirect_uri=%s&rid=%s",
                authorizeUrl, appId, state, redirectUri, System.currentTimeMillis());
    }

    /**
     * 获取 Access Token
     */
    public TikTokAccount getAccessToken(String authCode) {
        if (mockEnabled) {
            log.info("[TikTok OAuth Mock] Get access token");
            return createMockAccount();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("app_id", appId);
        params.put("secret", appSecret);
        params.put("auth_code", authCode);

        try {
            Map<String, Object> response = apiClient.post(tokenUrl, params, null, Map.class);
            
            // 解析响应
            Integer code = (Integer) response.get("code");
            if (code != 0) {
                String message = (String) response.get("message");
                throw new RuntimeException("Get access token failed: " + message);
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String accessToken = (String) data.get("access_token");
            String refreshToken = (String) data.get("refresh_token");
            Integer expiresIn = (Integer) data.get("expires_in");
            String advertiserId = (String) data.get("advertiser_id");
            String advertiserName = (String) data.get("advertiser_name");

            // 保存到数据库
            TikTokAccount account = new TikTokAccount();
            account.setAdvertiserId(advertiserId);
            account.setAdvertiserName(advertiserName);
            account.setAccessToken(accessToken);
            account.setRefreshToken(refreshToken);
            account.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            account.setStatus("active");

            accountMapper.insert(account);

            return account;
        } catch (Exception e) {
            log.error("[TikTok OAuth] Get access token failed: {}", e.getMessage(), e);
            throw new RuntimeException("Get access token failed: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新 Access Token
     */
    public TikTokAccount refreshAccessToken(String advertiserId) {
        TikTokAccount account = accountMapper.selectByAdvertiserId(advertiserId);
        if (account == null) {
            throw new RuntimeException("Account not found: " + advertiserId);
        }

        if (mockEnabled) {
            log.info("[TikTok OAuth Mock] Refresh access token");
            account.setTokenExpiresAt(LocalDateTime.now().plusDays(30));
            accountMapper.updateById(account);
            return account;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("app_id", appId);
        params.put("secret", appSecret);
        params.put("refresh_token", account.getRefreshToken());

        try {
            Map<String, Object> response = apiClient.post("/oauth2/refresh_token/", params, null, Map.class);
            
            Integer code = (Integer) response.get("code");
            if (code != 0) {
                String message = (String) response.get("message");
                throw new RuntimeException("Refresh access token failed: " + message);
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String accessToken = (String) data.get("access_token");
            String refreshToken = (String) data.get("refresh_token");
            Integer expiresIn = (Integer) data.get("expires_in");

            account.setAccessToken(accessToken);
            account.setRefreshToken(refreshToken);
            account.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));

            accountMapper.updateById(account);

            return account;
        } catch (Exception e) {
            log.error("[TikTok OAuth] Refresh access token failed: {}", e.getMessage(), e);
            throw new RuntimeException("Refresh access token failed: " + e.getMessage(), e);
        }
    }

    /**
     * 创建 Mock 账户
     */
    private TikTokAccount createMockAccount() {
        TikTokAccount account = new TikTokAccount();
        account.setAdvertiserId("mock_advertiser_" + System.currentTimeMillis());
        account.setAdvertiserName("Mock Advertiser");
        account.setAccessToken("mock_access_token_" + System.currentTimeMillis());
        account.setRefreshToken("mock_refresh_token_" + System.currentTimeMillis());
        account.setTokenExpiresAt(LocalDateTime.now().plusDays(30));
        account.setStatus("active");
        
        accountMapper.insert(account);
        
        return account;
    }
}


2.4 TikTok 广告创建服务
文件路径：backend-java/src/main/java/com/future/tiktok/service/TikTokAdService.java
package com.future.tiktok.service;

import com.future.tiktok.client.TikTokApiClient;
import com.future.tiktok.entity.*;
import com.future.tiktok.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TikTok 广告创建服务
 */
@Slf4j
@Service
public class TikTokAdService {

    @Value("${tiktok.api.mock.enabled}")
    private boolean mockEnabled;

    private final TikTokApiClient apiClient;
    private final TikTokAccountMapper accountMapper;
    private final TikTokCampaignMapper campaignMapper;
    private final TikTokAdGroupMapper adGroupMapper;
    private final TikTokAdMapper adMapper;

    public TikTokAdService(TikTokApiClient apiClient,
                          TikTokAccountMapper accountMapper,
                          TikTokCampaignMapper campaignMapper,
                          TikTokAdGroupMapper adGroupMapper,
                          TikTokAdMapper adMapper) {
        this.apiClient = apiClient;
        this.accountMapper = accountMapper;
        this.campaignMapper = campaignMapper;
        this.adGroupMapper = adGroupMapper;
        this.adMapper = adMapper;
    }

    /**
     * 创建广告系列
     */
    @Transactional
    public TikTokCampaign createCampaign(String advertiserId, String campaignName, 
                                        String objective, Double budget, String budgetMode) {
        TikTokAccount account = accountMapper.selectByAdvertiserId(advertiserId);
        if (account == null) {
            throw new RuntimeException("Account not found: " + advertiserId);
        }

        if (mockEnabled) {
            log.info("[TikTok Ad Mock] Create campaign: {}", campaignName);
            return createMockCampaign(advertiserId, campaignName, objective, budget, budgetMode);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("advertiser_id", advertiserId);
        params.put("campaign_name", campaignName);
        params.put("objective_type", objective);
        params.put("budget", budget);
        params.put("budget_mode", budgetMode);

        try {
            Map<String, Object> response = apiClient.post("/campaign/create/", params, 
                    account.getAccessToken(), Map.class);
            
            Integer code = (Integer) response.get("code");
            if (code != 0) {
                String message = (String) response.get("message");
                throw new RuntimeException("Create campaign failed: " + message);
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String campaignId = (String) data.get("campaign_id");

            TikTokCampaign campaign = new TikTokCampaign();
            campaign.setAdvertiserId(advertiserId);
            campaign.setCampaignId(campaignId);
            campaign.setCampaignName(campaignName);
            campaign.setObjective(objective);
            campaign.setBudget(budget);
            campaign.setBudgetMode(budgetMode);
            campaign.setOperationStatus("ENABLE");

            campaignMapper.insert(campaign);

            return campaign;
        } catch (Exception e) {
            log.error("[TikTok Ad] Create campaign failed: {}", e.getMessage(), e);
            throw new RuntimeException("Create campaign failed: " + e.getMessage(), e);
        }
    }

    /**
     * 创建广告组
     */
    @Transactional
    public TikTokAdGroup createAdGroup(String advertiserId, String campaignId, String adGroupName,
                                      String placementType, Double budget, String budgetMode,
                                      String billingEvent, String bidType, Double bidPrice) {
        TikTokAccount account = accountMapper.selectByAdvertiserId(advertiserId);
        if (account == null) {
            throw new RuntimeException("Account not found: " + advertiserId);
        }

        if (mockEnabled) {
            log.info("[TikTok Ad Mock] Create ad group: {}", adGroupName);
            return createMockAdGroup(advertiserId, campaignId, adGroupName, placementType, 
                    budget, budgetMode, billingEvent, bidType, bidPrice);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("advertiser_id", advertiserId);
        params.put("campaign_id", campaignId);
        params.put("adgroup_name", adGroupName);
        params.put("placement_type", placementType);
        params.put("budget", budget);
        params.put("budget_mode", budgetMode);
        params.put("billing_event", billingEvent);
        params.put("bid_type", bidType);
        if (bidPrice != null) {
            params.put("bid_price", bidPrice);
        }

        try {
            Map<String, Object> response = apiClient.post("/adgroup/create/", params, 
                    account.getAccessToken(), Map.class);
            
            Integer code = (Integer) response.get("code");
            if (code != 0) {
                String message = (String) response.get("message");
                throw new RuntimeException("Create ad group failed: " + message);
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String adGroupId = (String) data.get("adgroup_id");

            TikTokAdGroup adGroup = new TikTokAdGroup();
            adGroup.setAdvertiserId(advertiserId);
            adGroup.setCampaignId(campaignId);
            adGroup.setAdGroupId(adGroupId);
            adGroup.setAdGroupName(adGroupName);
            adGroup.setPlacementType(placementType);
            adGroup.setBudget(budget);
            adGroup.setBudgetMode(budgetMode);
            adGroup.setBillingEvent(billingEvent);
            adGroup.setBidType(bidType);
            adGroup.setBidPrice(bidPrice);
            adGroup.setOperationStatus("ENABLE");

            adGroupMapper.insert(adGroup);

            return adGroup;
        } catch (Exception e) {
            log.error("[TikTok Ad] Create ad group failed: {}", e.getMessage(), e);
            throw new RuntimeException("Create ad group failed: " + e.getMessage(), e);
        }
    }

    /**
     * 创建广告
     */
    @Transactional
    public TikTokAd createAd(String advertiserId, String campaignId, String adGroupId, String adName,
                            String creativeType, String videoId, String adText, 
                            String callToAction, String landingPageUrl, String pixelId) {
        TikTokAccount account = accountMapper.selectByAdvertiserId(advertiserId);
        if (account == null) {
            throw new RuntimeException("Account not found: " + advertiserId);
        }

        if (mockEnabled) {
            log.info("[TikTok Ad Mock] Create ad: {}", adName);
            return createMockAd(advertiserId, campaignId, adGroupId, adName, creativeType, 
                    videoId, adText, callToAction, landingPageUrl, pixelId);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("advertiser_id", advertiserId);
        params.put("adgroup_id", adGroupId);
        params.put("ad_name", adName);
        params.put("creative_type", creativeType);
        params.put("video_id", videoId);
        params.put("ad_text", adText);
        params.put("call_to_action", callToAction);
        params.put("landing_page_url", landingPageUrl);
        if (pixelId != null) {
            params.put("pixel_id", pixelId);
        }

        try {
            Map<String, Object> response = apiClient.post("/ad/create/", params, 
                    account.getAccessToken(), Map.class);
            
            Integer code = (Integer) response.get("code");
            if (code != 0) {
                String message = (String) response.get("message");
                throw new RuntimeException("Create ad failed: " + message);
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String adId = (String) data.get("ad_id");

            TikTokAd ad = new TikTokAd();
            ad.setAdvertiserId(advertiserId);
            ad.setCampaignId(campaignId);
            ad.setAdGroupId(adGroupId);
            ad.setAdId(adId);
            ad.setAdName(adName);
            ad.setCreativeType(creativeType);
            ad.setVideoId(videoId);
            ad.setAdText(adText);
            ad.setCallToAction(callToAction);
            ad.setLandingPageUrl(landingPageUrl);
            ad.setPixelId(pixelId);
            ad.setOperationStatus("ENABLE");

            adMapper.insert(ad);

            return ad;
        } catch (Exception e) {
            log.error("[TikTok Ad] Create ad failed: {}", e.getMessage(), e);
            throw new RuntimeException("Create ad failed: " + e.getMessage(), e);
        }
    }

    /**
     * 创建 Mock 广告系列
     */
    private TikTokCampaign createMockCampaign(String advertiserId, String campaignName, 
                                             String objective, Double budget, String budgetMode) {
        TikTokCampaign campaign = new TikTokCampaign();
        campaign.setAdvertiserId(advertiserId);
        campaign.setCampaignId("mock_campaign_" + UUID.randomUUID().toString());
        campaign.setCampaignName(campaignName);
        campaign.setObjective(objective);
        campaign.setBudget(budget);
        campaign.setBudgetMode(budgetMode);
        campaign.setOperationStatus("ENABLE");
        
        campaignMapper.insert(campaign);
        
        return campaign;
    }

    /**
     * 创建 Mock 广告组
     */
    private TikTokAdGroup createMockAdGroup(String advertiserId, String campaignId, String adGroupName,
                                           String placementType, Double budget, String budgetMode,
                                           String billingEvent, String bidType, Double bidPrice) {
        TikTokAdGroup adGroup = new TikTokAdGroup();
        adGroup.setAdvertiserId(advertiserId);
        adGroup.setCampaignId(campaignId);
        adGroup.setAdGroupId("mock_adgroup_" + UUID.randomUUID().toString());
        adGroup.setAdGroupName(adGroupName);
        adGroup.setPlacementType(placementType);
        adGroup.setBudget(budget);
        adGroup.setBudgetMode(budgetMode);
        adGroup.setBillingEvent(billingEvent);
        adGroup.setBidType(bidType);
        adGroup.setBidPrice(bidPrice);
        adGroup.setOperationStatus("ENABLE");
        
        adGroupMapper.insert(adGroup);
        
        return adGroup;
    }

    /**
     * 创建 Mock 广告
     */
    private TikTokAd createMockAd(String advertiserId, String campaignId, String adGroupId, String adName,
                                 String creativeType, String videoId, String adText, 
                                 String callToAction, String landingPageUrl, String pixelId) {
        TikTokAd ad = new TikTokAd();
        ad.setAdvertiserId(advertiserId);
        ad.setCampaignId(campaignId);
        ad.setAdGroupId(adGroupId);
        ad.setAdId("mock_ad_" + UUID.randomUUID().toString());
        ad.setAdName(adName);
        ad.setCreativeType(creativeType);
        ad.setVideoId(videoId);
        ad.setAdText(adText);
        ad.setCallToAction(callToAction);
        ad.setLandingPageUrl(landingPageUrl);
        ad.setPixelId(pixelId);
        ad.setOperationStatus("ENABLE");
        
        adMapper.insert(ad);
        
        return ad;
    }
}


由于回复长度限制，我将分成多个部分继续发送。请回复"继续"，我会继续发送剩余的代码（包括 Excel 导入、任务管理、数据同步、回传对接、前端页面等）。
📊 第二部分：后端 API 开发（续）
2.5 Excel 导入服务
文件路径：backend-java/src/main/java/com/future/tiktok/service/ExcelImportService.java
package com.future.tiktok.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.tiktok.entity.AdTask;
import com.future.tiktok.entity.AdTaskDetail;
import com.future.tiktok.mapper.AdTaskMapper;
import com.future.tiktok.mapper.AdTaskDetailMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Excel 导入服务
 */
@Slf4j
@Service
public class ExcelImportService {

    private final AdTaskMapper adTaskMapper;
    private final AdTaskDetailMapper adTaskDetailMapper;
    private final ObjectMapper objectMapper;

    public ExcelImportService(AdTaskMapper adTaskMapper,
                             AdTaskDetailMapper adTaskDetailMapper,
                             ObjectMapper objectMapper) {
        this.adTaskMapper = adTaskMapper;
        this.adTaskDetailMapper = adTaskDetailMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 下载 Excel 模板
     */
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("广告数据");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "广告主ID", "账户ID", "广告系列名称", "广告组名称", "广告名称",
            "短剧ID", "素材ID", "出价", "预算", "目标受众", "落地页URL"
        };
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        // 创建示例数据行
        Row exampleRow = sheet.createRow(1);
        String[] examples = {
            "1234567890", "9876543210", "霸道总裁系列", "霸道总裁-组1", "霸道总裁-广告1",
            "101", "301", "0.5", "100", "18-35岁女性", "https://example.com/landing"
        };
        
        for (int i = 0; i < examples.length; i++) {
            Cell cell = exampleRow.createCell(i);
            cell.setCellValue(examples[i]);
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=tiktok_ad_template.xlsx");

        // 写入响应
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 解析 Excel
     */
    public List<AdExcelData> parseExcel(MultipartFile file) {
        List<AdExcelData> dataList = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            
            // 跳过表头（第0行）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                AdExcelData data = new AdExcelData();
                data.setAdvertiserId(getCellValue(row.getCell(0)));
                data.setAccountId(getCellValue(row.getCell(1)));
                data.setCampaignName(getCellValue(row.getCell(2)));
                data.setAdGroupName(getCellValue(row.getCell(3)));
                data.setAdName(getCellValue(row.getCell(4)));
                data.setDramaId(getCellValue(row.getCell(5)));
                data.setMaterialId(getCellValue(row.getCell(6)));
                data.setBidPrice(getCellValue(row.getCell(7)));
                data.setBudget(getCellValue(row.getCell(8)));
                data.setTargetAudience(getCellValue(row.getCell(9)));
                data.setLandingPageUrl(getCellValue(row.getCell(10)));

                // 验证数据
                if (validateData(data)) {
                    dataList.add(data);
                } else {
                    log.warn("[Excel Import] Invalid data at row {}: {}", i, data);
                }
            }

            log.info("[Excel Import] Parsed {} rows from Excel", dataList.size());
            return dataList;

        } catch (Exception e) {
            log.error("[Excel Import] Parse Excel failed: {}", e.getMessage(), e);
            throw new RuntimeException("Parse Excel failed: " + e.getMessage(), e);
        }
    }

    /**
     * 提交 Excel 任务
     */
    @Transactional
    public String submitTask(List<AdExcelData> dataList) {
        // 创建任务
        String taskId = UUID.randomUUID().toString();
        
        AdTask task = new AdTask();
        task.setTaskId(taskId);
        task.setTaskType("excel");
        task.setTotalCount(dataList.size());
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setStatus("pending");
        task.setProgress(0);
        
        try {
            task.setTaskData(objectMapper.writeValueAsString(dataList));
        } catch (Exception e) {
            log.error("[Excel Import] Serialize task data failed: {}", e.getMessage(), e);
        }
        
        adTaskMapper.insert(task);

        // 创建任务明细
        for (AdExcelData data : dataList) {
            AdTaskDetail detail = new AdTaskDetail();
            detail.setTaskId(taskId);
            detail.setCampaignName(data.getCampaignName());
            detail.setAdGroupName(data.getAdGroupName());
            detail.setAdName(data.getAdName());
            detail.setDramaId(parseLong(data.getDramaId()));
            detail.setMaterialId(parseLong(data.getMaterialId()));
            detail.setBidPrice(parseDouble(data.getBidPrice()));
            detail.setBudget(parseDouble(data.getBudget()));
            detail.setStatus("pending");
            
            adTaskDetailMapper.insert(detail);
        }

        log.info("[Excel Import] Task created: {}, total count: {}", taskId, dataList.size());
        
        return taskId;
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 验证数据
     */
    private boolean validateData(AdExcelData data) {
        if (data.getAdvertiserId() == null || data.getAdvertiserId().isEmpty()) {
            return false;
        }
        if (data.getCampaignName() == null || data.getCampaignName().isEmpty()) {
            return false;
        }
        if (data.getAdGroupName() == null || data.getAdGroupName().isEmpty()) {
            return false;
        }
        if (data.getAdName() == null || data.getAdName().isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * 解析 Long
     */
    private Long parseLong(String value) {
        try {
            return value != null && !value.isEmpty() ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析 Double
     */
    private Double parseDouble(String value) {
        try {
            return value != null && !value.isEmpty() ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Excel 数据 DTO
     */
    @Data
    public static class AdExcelData {
        private String advertiserId;
        private String accountId;
        private String campaignName;
        private String adGroupName;
        private String adName;
        private String dramaId;
        private String materialId;
        private String bidPrice;
        private String budget;
        private String targetAudience;
        private String landingPageUrl;
    }
}


2.6 广告任务执行服务
文件路径：backend-java/src/main/java/com/future/tiktok/service/AdTaskExecutorService.java
package com.future.tiktok.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.tiktok.entity.*;
import com.future.tiktok.mapper.AdTaskDetailMapper;
import com.future.tiktok.mapper.AdTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 广告任务执行服务
 */
@Slf4j
@Service
public class AdTaskExecutorService {

    private final AdTaskMapper adTaskMapper;
    private final AdTaskDetailMapper adTaskDetailMapper;
    private final TikTokAdService tikTokAdService;
    private final ObjectMapper objectMapper;

    public AdTaskExecutorService(AdTaskMapper adTaskMapper,
                                AdTaskDetailMapper adTaskDetailMapper,
                                TikTokAdService tikTokAdService,
                                ObjectMapper objectMapper) {
        this.adTaskMapper = adTaskMapper;
        this.adTaskDetailMapper = adTaskDetailMapper;
        this.tikTokAdService = tikTokAdService;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行任务（异步）
     */
    @Async("taskExecutor")
    @Transactional
    public void executeTask(String taskId) {
        log.info("[Task Executor] Start executing task: {}", taskId);

        AdTask task = adTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            log.error("[Task Executor] Task not found: {}", taskId);
            return;
        }

        // 更新任务状态为执行中
        task.setStatus("processing");
        task.setStartedAt(LocalDateTime.now());
        adTaskMapper.updateById(task);

        try {
            // 解析任务数据
            List<ExcelImportService.AdExcelData> dataList = objectMapper.readValue(
                task.getTaskData(),
                new TypeReference<List<ExcelImportService.AdExcelData>>() {}
            );

            // 获取任务明细
            List<AdTaskDetail> details = adTaskDetailMapper.selectByTaskId(taskId);

            int successCount = 0;
            int failedCount = 0;

            // 逐个创建广告
            for (int i = 0; i < details.size(); i++) {
                AdTaskDetail detail = details.get(i);
                ExcelImportService.AdExcelData data = dataList.get(i);

                try {
                    // 更新明细状态为处理中
                    detail.setStatus("processing");
                    adTaskDetailMapper.updateById(detail);

                    // 1. 创建广告系列
                    TikTokCampaign campaign = tikTokAdService.createCampaign(
                        data.getAdvertiserId(),
                        data.getCampaignName(),
                        "CONVERSIONS",
                        parseDouble(data.getBudget()),
                        "BUDGET_MODE_DAY"
                    );
                    detail.setTikTokCampaignId(campaign.getCampaignId());

                    // 2. 创建广告组
                    TikTokAdGroup adGroup = tikTokAdService.createAdGroup(
                        data.getAdvertiserId(),
                        campaign.getCampaignId(),
                        data.getAdGroupName(),
                        "PLACEMENT_TYPE_AUTOMATIC",
                        parseDouble(data.getBudget()),
                        "BUDGET_MODE_DAY",
                        "CPC",
                        "BID_TYPE_CUSTOM",
                        parseDouble(data.getBidPrice())
                    );
                    detail.setTikTokAdGroupId(adGroup.getAdGroupId());

                    // 3. 创建广告
                    TikTokAd ad = tikTokAdService.createAd(
                        data.getAdvertiserId(),
                        campaign.getCampaignId(),
                        adGroup.getAdGroupId(),
                        data.getAdName(),
                        "VIDEO",
                        "mock_video_" + data.getMaterialId(),
                        "精彩短剧，不容错过！",
                        "DOWNLOAD",
                        data.getLandingPageUrl(),
                        null
                    );
                    detail.setTikTokAdId(ad.getAdId());

                    // 更新明细状态为成功
                    detail.setStatus("success");
                    adTaskDetailMapper.updateById(detail);

                    successCount++;
                    log.info("[Task Executor] Ad created successfully: {}", data.getAdName());

                } catch (Exception e) {
                    log.error("[Task Executor] Create ad failed: {}", e.getMessage(), e);
                    
                    // 更新明细状态为失败
                    detail.setStatus("failed");
                    detail.setErrorMessage(e.getMessage());
                    adTaskDetailMapper.updateById(detail);

                    failedCount++;
                }

                // 更新任务进度
                int progress = (int) ((i + 1) * 100.0 / details.size());
                task.setProgress(progress);
                task.setSuccessCount(successCount);
                task.setFailedCount(failedCount);
                adTaskMapper.updateById(task);
            }

            // 更新任务状态为已完成
            task.setStatus("completed");
            task.setCompletedAt(LocalDateTime.now());
            task.setSuccessCount(successCount);
            task.setFailedCount(failedCount);
            task.setProgress(100);
            adTaskMapper.updateById(task);

            log.info("[Task Executor] Task completed: {}, success: {}, failed: {}", 
                    taskId, successCount, failedCount);

        } catch (Exception e) {
            log.error("[Task Executor] Execute task failed: {}", e.getMessage(), e);
            
            // 更新任务状态为失败
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            adTaskMapper.updateById(task);
        }
    }

    /**
     * 解析 Double
     */
    private Double parseDouble(String value) {
        try {
            return value != null && !value.isEmpty() ? Double.parseDouble(value) : 100.0;
        } catch (NumberFormatException e) {
            return 100.0;
        }
    }
}


2.7 数据同步服务
文件路径：backend-java/src/main/java/com/future/tiktok/service/TikTokDataSyncService.java
package com.future.tiktok.service;

import com.future.tiktok.client.TikTokApiClient;
import com.future.tiktok.entity.TikTokAccount;
import com.future.tiktok.entity.TikTokReport;
import com.future.tiktok.mapper.TikTokAccountMapper;
import com.future.tiktok.mapper.TikTokReportMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * TikTok 数据同步服务
 */
@Slf4j
@Service
public class TikTokDataSyncService {

    @Value("${tiktok.api.sync.enabled}")
    private boolean syncEnabled;

    @Value("${tiktok.api.mock.enabled}")
    private boolean mockEnabled;

    private final TikTokApiClient apiClient;
    private final TikTokAccountMapper accountMapper;
    private final TikTokReportMapper reportMapper;

    public TikTokDataSyncService(TikTokApiClient apiClient,
                                TikTokAccountMapper accountMapper,
                                TikTokReportMapper reportMapper) {
        this.apiClient = apiClient;
        this.accountMapper = accountMapper;
        this.reportMapper = reportMapper;
    }

    /**
     * 定时同步数据（每 10 分钟执行一次）
     */
    @Scheduled(fixedDelayString = "${tiktok.api.sync.interval}")
    @Transactional
    public void syncData() {
        if (!syncEnabled) {
            log.debug("[Data Sync] Sync is disabled");
            return;
        }

        log.info("[Data Sync] Start syncing data");

        try {
            // 获取所有活跃账户
            List<TikTokAccount> accounts = accountMapper.selectByStatus("active");
            
            for (TikTokAccount account : accounts) {
                try {
                    syncAccountData(account);
                } catch (Exception e) {
                    log.error("[Data Sync] Sync account data failed: {}", e.getMessage(), e);
                }
            }

            log.info("[Data Sync] Data sync completed");

        } catch (Exception e) {
            log.error("[Data Sync] Sync data failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 同步账户数据
     */
    private void syncAccountData(TikTokAccount account) {
        log.info("[Data Sync] Syncing account: {}", account.getAdvertiserId());

        // 同步广告系列数据
        syncReportData(account, "campaign");
        
        // 同步广告组数据
        syncReportData(account, "adgroup");
        
        // 同步广告数据
        syncReportData(account, "ad");
    }

    /**
     * 同步报告数据
     */
    private void syncReportData(TikTokAccount account, String dimensions) {
        if (mockEnabled) {
            log.info("[Data Sync Mock] Syncing {} data for account: {}", 
                    dimensions, account.getAdvertiserId());
            createMockReportData(account.getAdvertiserId(), dimensions);
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("advertiser_id", account.getAdvertiserId());
        params.put("dimensions", dimensions);
        params.put("start_date", LocalDate.now().minusDays(7).toString());
        params.put("end_date", LocalDate.now().toString());
        params.put("metrics", Arrays.asList(
            "spend", "impressions", "clicks", "ctr", "cpc", "cpm",
            "conversions", "conversion_rate", "cost_per_conversion", "conversion_value"
        ));

        try {
            Map<String, Object> response = apiClient.get("/report/integrated/get/", 
                    params, account.getAccessToken(), Map.class);
            
            Integer code = (Integer) response.get("code");
            if (code != 0) {
                String message = (String) response.get("message");
                throw new RuntimeException("Sync report data failed: " + message);
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");

            for (Map<String, Object> item : list) {
                saveReportData(account.getAdvertiserId(), dimensions, item);
            }

            log.info("[Data Sync] Synced {} {} records", list.size(), dimensions);

        } catch (Exception e) {
            log.error("[Data Sync] Sync {} data failed: {}", dimensions, e.getMessage(), e);
        }
    }

    /**
     * 保存报告数据
     */
    private void saveReportData(String advertiserId, String dimensions, Map<String, Object> data) {
        String dimensionId = (String) data.get("dimension_id");
        String statDate = (String) data.get("stat_date");

        TikTokReport report = reportMapper.selectByDimension(
                advertiserId, dimensions, dimensionId, LocalDate.parse(statDate));

        if (report == null) {
            report = new TikTokReport();
            report.setAdvertiserId(advertiserId);
            report.setDimensions(dimensions);
            report.setDimensionId(dimensionId);
            report.setStatDate(LocalDate.parse(statDate));
        }

        // 更新数据
        report.setSpend(getDouble(data, "spend"));
        report.setImpressions(getInteger(data, "impressions"));
        report.setClicks(getInteger(data, "clicks"));
        report.setCtr(getDouble(data, "ctr"));
        report.setCpc(getDouble(data, "cpc"));
        report.setCpm(getDouble(data, "cpm"));
        report.setConversions(getInteger(data, "conversions"));
        report.setConversionRate(getDouble(data, "conversion_rate"));
        report.setCostPerConversion(getDouble(data, "cost_per_conversion"));
        report.setConversionValue(getDouble(data, "conversion_value"));

        // 计算 ROI
        if (report.getSpend() != null && report.getSpend() > 0) {
            double roi = (report.getConversionValue() != null ? report.getConversionValue() : 0) 
                        / report.getSpend();
            report.setRoi(roi);
        }

        if (report.getId() == null) {
            reportMapper.insert(report);
        } else {
            reportMapper.updateById(report);
        }
    }

    /**
     * 创建 Mock 报告数据
     */
    private void createMockReportData(String advertiserId, String dimensions) {
        Random random = new Random();
        
        for (int i = 0; i < 5; i++) {
            String dimensionId = "mock_" + dimensions + "_" + (i + 1);
            LocalDate statDate = LocalDate.now().minusDays(i);

            TikTokReport report = reportMapper.selectByDimension(
                    advertiserId, dimensions, dimensionId, statDate);

            if (report == null) {
                report = new TikTokReport();
                report.setAdvertiserId(advertiserId);
                report.setDimensions(dimensions);
                report.setDimensionId(dimensionId);
                report.setStatDate(statDate);
            }

            // 生成随机数据
            double spend = 50 + random.nextDouble() * 200;
            int impressions = 1000 + random.nextInt(9000);
            int clicks = 50 + random.nextInt(200);
            int conversions = 5 + random.nextInt(20);
            double conversionValue = conversions * (10 + random.nextDouble() * 40);

            report.setSpend(spend);
            report.setImpressions(impressions);
            report.setClicks(clicks);
            report.setCtr(clicks * 100.0 / impressions);
            report.setCpc(spend / clicks);
            report.setCpm(spend * 1000.0 / impressions);
            report.setConversions(conversions);
            report.setConversionRate(conversions * 100.0 / clicks);
            report.setCostPerConversion(spend / conversions);
            report.setConversionValue(conversionValue);
            report.setRoi(conversionValue / spend);

            if (report.getId() == null) {
                reportMapper.insert(report);
            } else {
                reportMapper.updateById(report);
            }
        }
    }

    /**
     * 获取 Double 值
     */
    private Double getDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * 获取 Integer 值
     */
    private Integer getInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return 0;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}


2.8 回传服务（Conversion API）
文件路径：backend-java/src/main/java/com/future/tiktok/service/TikTokConversionService.java
package com.future.tiktok.service;

import com.future.tiktok.client.TikTokApiClient;
import com.future.tiktok.entity.TikTokAccount;
import com.future.tiktok.entity.TikTokConversionLog;
import com.future.tiktok.mapper.TikTokAccountMapper;
import com.future.tiktok.mapper.TikTokConversionLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * TikTok 回传服务（Conversion API）
 */
@Slf4j
@Service
public class TikTokConversionService {

    @Value("${tiktok.api.conversion.enabled}")
    private boolean conversionEnabled;

    @Value("${tiktok.api.mock.enabled}")
    private boolean mockEnabled;

    private final TikTokApiClient apiClient;
    private final TikTokAccountMapper accountMapper;
    private final TikTokConversionLogMapper conversionLogMapper;

    public TikTokConversionService(TikTokApiClient apiClient,
                                  TikTokAccountMapper accountMapper,
                                  TikTokConversionLogMapper conversionLogMapper) {
        this.apiClient = apiClient;
        this.accountMapper = accountMapper;
        this.conversionLogMapper = conversionLogMapper;
    }

    /**
     * 回传充值事件
     */
    @Transactional
    public void trackPurchase(String advertiserId, String pixelId, Long userId, 
                             String clickId, Double amount, String currency) {
        if (!conversionEnabled) {
            log.debug("[Conversion] Conversion is disabled");
            return;
        }

        String eventId = "purchase_" + userId + "_" + System.currentTimeMillis();

        // 检查是否已回传（去重）
        TikTokConversionLog existingLog = conversionLogMapper.selectByEventId(eventId);
        if (existingLog != null) {
            log.warn("[Conversion] Event already tracked: {}", eventId);
            return;
        }

        // 创建回传记录
        TikTokConversionLog log = new TikTokConversionLog();
        log.setAdvertiserId(advertiserId);
        log.setPixelId(pixelId);
        log.setEventType("Purchase");
        log.setEventId(eventId);
        log.setUserId(String.valueOf(userId));
        log.setClickId(clickId);
        log.setEventValue(amount);
        log.setCurrency(currency);
        log.setStatus("pending");
        log.setEventTime(LocalDateTime.now());
        
        conversionLogMapper.insert(log);

        // 发送回传
        try {
            sendConversion(log);
        } catch (Exception e) {
            log.error("[Conversion] Track purchase failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 回传注册事件
     */
    @Transactional
    public void trackRegistration(String advertiserId, String pixelId, Long userId, String clickId) {
        if (!conversionEnabled) {
            log.debug("[Conversion] Conversion is disabled");
            return;
        }

        String eventId = "registration_" + userId + "_" + System.currentTimeMillis();

        // 检查是否已回传（去重）
        TikTokConversionLog existingLog = conversionLogMapper.selectByEventId(eventId);
        if (existingLog != null) {
            log.warn("[Conversion] Event already tracked: {}", eventId);
            return;
        }

        // 创建回传记录
        TikTokConversionLog log = new TikTokConversionLog();
        log.setAdvertiserId(advertiserId);
        log.setPixelId(pixelId);
        log.setEventType("CompleteRegistration");
        log.setEventId(eventId);
        log.setUserId(String.valueOf(userId));
        log.setClickId(clickId);
        log.setStatus("pending");
        log.setEventTime(LocalDateTime.now());
        
        conversionLogMapper.insert(log);

        // 发送回传
        try {
            sendConversion(log);
        } catch (Exception e) {
            log.error("[Conversion] Track registration failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送回传
     */
    private void sendConversion(TikTokConversionLog log) {
        TikTokAccount account = accountMapper.selectByAdvertiserId(log.getAdvertiserId());
        if (account == null) {
            throw new RuntimeException("Account not found: " + log.getAdvertiserId());
        }

        if (mockEnabled) {
            log.info("[Conversion Mock] Tracking event: {}", log.getEventType());
            log.setStatus("success");
            log.setResponseCode(200);
            log.setResponseMessage("OK");
            conversionLogMapper.updateById(log);
            return;
        }

        // 构建请求参数
        Map<String, Object> event = new HashMap<>();
        event.put("event", log.getEventType());
        event.put("event_id", log.getEventId());
        event.put("timestamp", log.getEventTime().toEpochSecond(ZoneOffset.UTC));

        // 用户信息
        Map<String, Object> user = new HashMap<>();
        if (log.getClickId() != null) {
            user.put("ttclid", log.getClickId());
        }
        if (log.getExternalId() != null) {
            user.put("external_id", log.getExternalId());
        }
        event.put("user", user);

        // 事件属性
        Map<String, Object> properties = new HashMap<>();
        if (log.getEventValue() != null) {
            properties.put("value", log.getEventValue());
            properties.put("currency", log.getCurrency());
        }
        if (log.getContentType() != null) {
            properties.put("content_type", log.getContentType());
        }
        if (log.getContentId() != null) {
            properties.put("content_id", log.getContentId());
        }
        event.put("properties", properties);

        Map<String, Object> params = new HashMap<>();
        params.put("pixel_code", log.getPixelId());
        params.put("event", event);

        try {
            Map<String, Object> response = apiClient.post("/event/track/", params, 
                    account.getAccessToken(), Map.class);
            
            Integer code = (Integer) response.get("code");
            String message = (String) response.get("message");

            if (code == 0) {
                log.setStatus("success");
                log.setResponseCode(200);
                log.setResponseMessage(message);
                log.info("[Conversion] Event tracked successfully: {}", log.getEventId());
            } else {
                log.setStatus("failed");
                log.setResponseCode(code);
                log.setResponseMessage(message);
                log.error("[Conversion] Track event failed: {}", message);
            }

            conversionLogMapper.updateById(log);

        } catch (Exception e) {
            log.error("[Conversion] Send conversion failed: {}", e.getMessage(), e);
            
            log.setStatus("failed");
            log.setResponseMessage(e.getMessage());
            log.setRetryCount(log.getRetryCount() + 1);
            conversionLogMapper.updateById(log);
            
            throw e;
        }
    }
}


2.9 Controller 层
文件路径：backend-java/src/main/java/com/future/tiktok/controller/TikTokController.java
package com.future.tiktok.controller;

import com.future.common.Result;
import com.future.tiktok.entity.*;
import com.future.tiktok.service.*;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * TikTok API Controller
 */
@RestController
@RequestMapping("/api/tiktok")
public class TikTokController {

    private final TikTokOAuthService oauthService;
    private final TikTokAdService adService;
    private final ExcelImportService excelService;
    private final AdTaskExecutorService taskExecutorService;

    public TikTokController(TikTokOAuthService oauthService,
                           TikTokAdService adService,
                           ExcelImportService excelService,
                           AdTaskExecutorService taskExecutorService) {
        this.oauthService = oauthService;
        this.adService = adService;
        this.excelService = excelService;
        this.taskExecutorService = taskExecutorService;
    }

    /**
     * 获取授权 URL
     */
    @GetMapping("/oauth/authorize-url")
    public Result<String> getAuthorizeUrl(@RequestParam String state) {
        String url = oauthService.getAuthorizeUrl(state);
        return Result.success(url);
    }

    /**
     * OAuth 回调
     */
    @GetMapping("/oauth/callback")
    public Result<TikTokAccount> oauthCallback(@RequestParam("auth_code") String authCode) {
        TikTokAccount account = oauthService.getAccessToken(authCode);
        return Result.success(account);
    }

    /**
     * 创建广告系列
     */
    @PostMapping("/campaign/create")
    public Result<TikTokCampaign> createCampaign(@RequestBody CreateCampaignRequest request) {
        TikTokCampaign campaign = adService.createCampaign(
            request.getAdvertiserId(),
            request.getCampaignName(),
            request.getObjective(),
            request.getBudget(),
            request.getBudgetMode()
        );
        return Result.success(campaign);
    }

    /**
     * 创建广告组
     */
    @PostMapping("/adgroup/create")
    public Result<TikTokAdGroup> createAdGroup(@RequestBody CreateAdGroupRequest request) {
        TikTokAdGroup adGroup = adService.createAdGroup(
            request.getAdvertiserId(),
            request.getCampaignId(),
            request.getAdGroupName(),
            request.getPlacementType(),
            request.getBudget(),
            request.getBudgetMode(),
            request.getBillingEvent(),
            request.getBidType(),
            request.getBidPrice()
        );
        return Result.success(adGroup);
    }

    /**
     * 创建广告
     */
    @PostMapping("/ad/create")
    public Result<TikTokAd> createAd(@RequestBody CreateAdRequest request) {
        TikTokAd ad = adService.createAd(
            request.getAdvertiserId(),
            request.getCampaignId(),
            request.getAdGroupId(),
            request.getAdName(),
            request.getCreativeType(),
            request.getVideoId(),
            request.getAdText(),
            request.getCallToAction(),
            request.getLandingPageUrl(),
            request.getPixelId()
        );
        return Result.success(ad);
    }

    /**
     * 下载 Excel 模板
     */
    @GetMapping("/excel/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        excelService.downloadTemplate(response);
    }

    /**
     * 解析 Excel
     */
    @PostMapping("/excel/parse")
    public Result<List<ExcelImportService.AdExcelData>> parseExcel(
            @RequestParam("file") MultipartFile file) {
        List<ExcelImportService.AdExcelData> data = excelService.parseExcel(file);
        return Result.success(data);
    }

    /**
     * 提交 Excel 任务
     */
    @PostMapping("/excel/submit")
    public Result<String> submitExcelTask(@RequestBody List<ExcelImportService.AdExcelData> data) {
        String taskId = excelService.submitTask(data);
        
        // 异步执行任务
        taskExecutorService.executeTask(taskId);
        
        return Result.success(taskId);
    }

    // DTO 类
    @Data
    public static class CreateCampaignRequest {
        private String advertiserId;
        private String campaignName;
        private String objective;
        private Double budget;
        private String budgetMode;
    }

    @Data
    public static class CreateAdGroupRequest {
        private String advertiserId;
        private String campaignId;
        private String adGroupName;
        private String placementType;
        private Double budget;
        private String budgetMode;
        private String billingEvent;
        private String bidType;
        private Double bidPrice;
    }

    @Data
    public static class CreateAdRequest {
        private String advertiserId;
        private String campaignId;
        private String adGroupId;
        private String adName;
        private String creativeType;
        private String videoId;
        private String adText;
        private String callToAction;
        private String landingPageUrl;
        private String pixelId;
    }
}


由于回复长度限制，请回复"继续"，我会继续发送前端代码部分！ 🚀
📊 第三部分：前端页面开发
3.1 TikTok 账户管理页面
文件路径：frontend/src/views/TikTokAccounts.vue
<template>
  <div class="tiktok-accounts-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>TikTok 账户管理</span>
          <el-button type="primary" @click="handleAuthorize">
            <el-icon><Plus /></el-icon>
            添加账户
          </el-button>
        </div>
      </template>

      <!-- 账户列表 -->
      <el-table :data="accounts" v-loading="loading">
        <el-table-column prop="advertiserId" label="广告主 ID" width="150" />
        <el-table-column prop="advertiserName" label="广告主名称" width="200" />
        <el-table-column prop="currency" label="货币" width="80" />
        <el-table-column prop="balance" label="余额" width="120">
          <template #default="{ row }">
            {{ row.balance ? row.balance.toFixed(2) : '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tokenExpiresAt" label="Token 过期时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.tokenExpiresAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleRefreshToken(row)"
              :loading="row.refreshing"
            >
              刷新 Token
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- OAuth 回调处理 -->
    <el-dialog v-model="callbackDialogVisible" title="授权中" width="400px">
      <div style="text-align: center; padding: 20px;">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <p style="margin-top: 20px;">正在处理授权信息...</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Loading } from '@element-plus/icons-vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const accounts = ref([])
const loading = ref(false)
const callbackDialogVisible = ref(false)

// 加载账户列表
const loadAccounts = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/tiktok/accounts')
    accounts.value = response.data.data
  } catch (error) {
    ElMessage.error('加载账户列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 处理授权
const handleAuthorize = async () => {
  try {
    const state = Math.random().toString(36).substring(7)
    const response = await axios.get('/api/tiktok/oauth/authorize-url', {
      params: { state }
    })
    
    // 保存 state 到 localStorage（用于验证回调）
    localStorage.setItem('tiktok_oauth_state', state)
    
    // 跳转到 TikTok 授权页面
    window.location.href = response.data.data
  } catch (error) {
    ElMessage.error('获取授权 URL 失败：' + error.message)
  }
}

// 处理 OAuth 回调
const handleOAuthCallback = async () => {
  const authCode = route.query.auth_code
  const state = route.query.state
  
  if (!authCode || !state) {
    return
  }
  
  // 验证 state
  const savedState = localStorage.getItem('tiktok_oauth_state')
  if (state !== savedState) {
    ElMessage.error('授权验证失败，请重试')
    router.push('/tiktok/accounts')
    return
  }
  
  callbackDialogVisible.value = true
  
  try {
    await axios.get('/api/tiktok/oauth/callback', {
      params: { auth_code: authCode }
    })
    
    ElMessage.success('账户添加成功')
    localStorage.removeItem('tiktok_oauth_state')
    
    // 重新加载账户列表
    await loadAccounts()
    
    // 跳转回账户列表页面
    router.push('/tiktok/accounts')
  } catch (error) {
    ElMessage.error('授权失败：' + error.message)
  } finally {
    callbackDialogVisible.value = false
  }
}

// 刷新 Token
const handleRefreshToken = async (row) => {
  row.refreshing = true
  try {
    await axios.post('/api/tiktok/oauth/refresh', {
      advertiserId: row.advertiserId
    })
    ElMessage.success('Token 刷新成功')
    await loadAccounts()
  } catch (error) {
    ElMessage.error('Token 刷新失败：' + error.message)
  } finally {
    row.refreshing = false
  }
}

// 删除账户
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除账户 ${row.advertiserName} 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.delete(`/api/tiktok/accounts/${row.advertiserId}`)
    ElMessage.success('删除成功')
    await loadAccounts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

onMounted(() => {
  // 检查是否是 OAuth 回调
  if (route.query.auth_code) {
    handleOAuthCallback()
  } else {
    loadAccounts()
  }
})
</script>

<style scoped>
.tiktok-accounts-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>


3.2 批量工具页面（集成 Excel 导入）
文件路径：frontend/src/views/BatchTools.vue
<template>
  <div class="batch-tools-container">
    <el-card>
      <template #header>
        <span>批量工具</span>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 手动创建 -->
        <el-tab-pane label="手动创建" name="manual">
          <!-- 现有的手动创建流程 -->
          <div class="manual-create">
            <el-steps :active="currentStep" align-center>
              <el-step title="选择主体" />
              <el-step title="选择账户" />
              <el-step title="设置项目" />
              <el-step title="设置广告组" />
              <el-step title="设置广告" />
            </el-steps>

            <div class="step-content">
              <!-- 步骤 1：选择主体 -->
              <div v-if="currentStep === 0">
                <el-form :model="formData" label-width="120px">
                  <el-form-item label="选择主体">
                    <el-select 
                      v-model="formData.advertiserId" 
                      placeholder="请选择主体"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="account in accounts"
                        :key="account.advertiserId"
                        :label="account.advertiserName"
                        :value="account.advertiserId"
                      />
                    </el-select>
                  </el-form-item>
                </el-form>
                
                <div class="step-buttons">
                  <el-button type="primary" @click="nextStep">下一步</el-button>
                </div>
              </div>

              <!-- 步骤 2：选择账户 -->
              <div v-if="currentStep === 1">
                <el-form :model="formData" label-width="120px">
                  <el-form-item label="选择账户">
                    <el-checkbox-group v-model="formData.accountIds">
                      <el-checkbox
                        v-for="account in accounts"
                        :key="account.advertiserId"
                        :label="account.advertiserId"
                      >
                        {{ account.advertiserName }}
                      </el-checkbox>
                    </el-checkbox-group>
                  </el-form-item>
                </el-form>
                
                <div class="step-buttons">
                  <el-button @click="prevStep">上一步</el-button>
                  <el-button type="primary" @click="nextStep">下一步</el-button>
                </div>
              </div>

              <!-- 步骤 3：设置项目（广告系列） -->
              <div v-if="currentStep === 2">
                <el-form :model="formData" label-width="120px">
                  <el-form-item label="广告系列名称">
                    <el-input v-model="formData.campaignName" placeholder="请输入广告系列名称" />
                  </el-form-item>
                  <el-form-item label="目标">
                    <el-select v-model="formData.objective" placeholder="请选择目标">
                      <el-option label="转化" value="CONVERSIONS" />
                      <el-option label="流量" value="TRAFFIC" />
                      <el-option label="应用推广" value="APP_PROMOTION" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="预算">
                    <el-input-number v-model="formData.campaignBudget" :min="1" />
                  </el-form-item>
                  <el-form-item label="预算模式">
                    <el-radio-group v-model="formData.campaignBudgetMode">
                      <el-radio label="BUDGET_MODE_DAY">日预算</el-radio>
                      <el-radio label="BUDGET_MODE_TOTAL">总预算</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-form>
                
                <div class="step-buttons">
                  <el-button @click="prevStep">上一步</el-button>
                  <el-button type="primary" @click="nextStep">下一步</el-button>
                </div>
              </div>

              <!-- 步骤 4：设置广告组 -->
              <div v-if="currentStep === 3">
                <el-form :model="formData" label-width="120px">
                  <el-form-item label="广告组名称">
                    <el-input v-model="formData.adGroupName" placeholder="请输入广告组名称" />
                  </el-form-item>
                  <el-form-item label="投放位置">
                    <el-radio-group v-model="formData.placementType">
                      <el-radio label="PLACEMENT_TYPE_AUTOMATIC">自动</el-radio>
                      <el-radio label="PLACEMENT_TYPE_NORMAL">手动</el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="预算">
                    <el-input-number v-model="formData.adGroupBudget" :min="1" />
                  </el-form-item>
                  <el-form-item label="出价">
                    <el-input-number v-model="formData.bidPrice" :min="0.01" :step="0.01" />
                  </el-form-item>
                </el-form>
                
                <div class="step-buttons">
                  <el-button @click="prevStep">上一步</el-button>
                  <el-button type="primary" @click="nextStep">下一步</el-button>
                </div>
              </div>

              <!-- 步骤 5：设置广告 -->
              <div v-if="currentStep === 4">
                <el-form :model="formData" label-width="120px">
                  <el-form-item label="广告名称">
                    <el-input v-model="formData.adName" placeholder="请输入广告名称" />
                  </el-form-item>
                  <el-form-item label="短剧">
                    <el-select v-model="formData.dramaId" placeholder="请选择短剧">
                      <el-option
                        v-for="drama in dramas"
                        :key="drama.id"
                        :label="drama.name"
                        :value="drama.id"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="素材">
                    <el-select v-model="formData.materialId" placeholder="请选择素材">
                      <el-option
                        v-for="material in materials"
                        :key="material.id"
                        :label="material.name"
                        :value="material.id"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="广告文案">
                    <el-input 
                      v-model="formData.adText" 
                      type="textarea" 
                      :rows="3"
                      placeholder="请输入广告文案"
                    />
                  </el-form-item>
                  <el-form-item label="落地页 URL">
                    <el-input v-model="formData.landingPageUrl" placeholder="请输入落地页 URL" />
                  </el-form-item>
                </el-form>
                
                <div class="step-buttons">
                  <el-button @click="prevStep">上一步</el-button>
                  <el-button 
                    type="primary" 
                    @click="submitManualTask"
                    :loading="submitting"
                  >
                    提交任务
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- Excel 导入 -->
        <el-tab-pane label="Excel 导入" name="excel">
          <div class="excel-import">
            <el-steps :active="excelStep" align-center>
              <el-step title="下载模板" />
              <el-step title="填写数据" />
              <el-step title="上传文件" />
              <el-step title="提交任务" />
            </el-steps>

            <div class="step-content">
              <!-- 步骤 1：下载模板 -->
              <div v-if="excelStep === 0">
                <el-alert
                  title="请先下载 Excel 模板，按照模板格式填写广告信息"
                  type="info"
                  :closable="false"
                  style="margin-bottom: 20px"
                />
                
                <div class="template-info">
                  <h3>模板说明：</h3>
                  <ul>
                    <li>广告主 ID：TikTok 广告主账户 ID</li>
                    <li>账户 ID：TikTok 账户 ID（可多选）</li>
                    <li>广告系列名称：广告系列的名称</li>
                    <li>广告组名称：广告组的名称</li>
                    <li>广告名称：广告的名称</li>
                    <li>短剧 ID：短剧的 ID</li>
                    <li>素材 ID：素材的 ID</li>
                    <li>出价：单次点击出价（美元）</li>
                    <li>预算：日预算（美元）</li>
                    <li>目标受众：目标受众描述</li>
                    <li>落地页 URL：广告落地页 URL</li>
                  </ul>
                </div>
                
                <div class="step-buttons">
                  <el-button type="primary" @click="downloadTemplate">
                    <el-icon><Download /></el-icon>
                    下载 Excel 模板
                  </el-button>
                  <el-button @click="excelStep = 1">下一步</el-button>
                </div>
              </div>

              <!-- 步骤 2：填写数据 -->
              <div v-if="excelStep === 1">
                <el-alert
                  title="请在 Excel 中填写广告信息，填写完成后点击"下一步""
                  type="info"
                  :closable="false"
                  style="margin-bottom: 20px"
                />
                
                <div class="fill-tips">
                  <h3>填写提示：</h3>
                  <ul>
                    <li>✅ 请确保所有必填字段都已填写</li>
                    <li>✅ 广告主 ID 和账户 ID 必须是有效的 TikTok 账户</li>
                    <li>✅ 出价和预算必须是数字</li>
                    <li>✅ 落地页 URL 必须是有效的 URL</li>
                    <li>⚠️ 不要修改表头（第一行）</li>
                    <li>⚠️ 不要删除示例数据行（第二行），可以修改为实际数据</li>
                  </ul>
                </div>
                
                <div class="step-buttons">
                  <el-button @click="excelStep = 0">上一步</el-button>
                  <el-button type="primary" @click="excelStep = 2">下一步</el-button>
                </div>
              </div>

              <!-- 步骤 3：上传文件 -->
              <div v-if="excelStep === 2">
                <el-upload
                  ref="uploadRef"
                  :auto-upload="false"
                  :limit="1"
                  :on-change="handleFileChange"
                  :on-exceed="handleExceed"
                  :file-list="fileList"
                  accept=".xlsx,.xls"
                  drag
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">
                    将 Excel 文件拖到此处，或<em>点击上传</em>
                  </div>
                  <template #tip>
                    <div class="el-upload__tip">
                      只能上传 .xlsx / .xls 文件，且不超过 10MB
                    </div>
                  </template>
                </el-upload>
                
                <div class="step-buttons" style="margin-top: 20px">
                  <el-button @click="excelStep = 1">上一步</el-button>
                  <el-button 
                    type="primary" 
                    @click="parseExcel"
                    :loading="parsing"
                    :disabled="fileList.length === 0"
                  >
                    解析 Excel
                  </el-button>
                </div>
              </div>

              <!-- 步骤 4：提交任务 -->
              <div v-if="excelStep === 3">
                <el-alert
                  :title="`共解析出 ${excelData.length} 条广告数据`"
                  type="success"
                  :closable="false"
                  style="margin-bottom: 20px"
                />
                
                <!-- 预览数据 -->
                <el-table :data="excelData" style="margin-top: 20px" max-height="400">
                  <el-table-column prop="advertiserId" label="广告主 ID" width="120" />
                  <el-table-column prop="accountId" label="账户 ID" width="120" />
                  <el-table-column prop="campaignName" label="广告系列" width="150" />
                  <el-table-column prop="adGroupName" label="广告组" width="150" />
                  <el-table-column prop="adName" label="广告名称" width="150" />
                  <el-table-column prop="dramaId" label="短剧 ID" width="100" />
                  <el-table-column prop="materialId" label="素材 ID" width="100" />
                  <el-table-column prop="bidPrice" label="出价" width="80" />
                  <el-table-column prop="budget" label="预算" width="80" />
                  <el-table-column prop="targetAudience" label="目标受众" width="120" />
                </el-table>
                
                <div class="step-buttons" style="margin-top: 20px">
                  <el-button @click="excelStep = 2">上一步</el-button>
                  <el-button 
                    type="primary" 
                    @click="submitExcelTask"
                    :loading="submitting"
                  >
                    提交任务
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, UploadFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()

const activeTab = ref('manual')
const currentStep = ref(0)
const excelStep = ref(0)

const accounts = ref([])
const dramas = ref([])
const materials = ref([])

const formData = ref({
  advertiserId: '',
  accountIds: [],
  campaignName: '',
  objective: 'CONVERSIONS',
  campaignBudget: 100,
  campaignBudgetMode: 'BUDGET_MODE_DAY',
  adGroupName: '',
  placementType: 'PLACEMENT_TYPE_AUTOMATIC',
  adGroupBudget: 100,
  bidPrice: 0.5,
  adName: '',
  dramaId: null,
  materialId: null,
  adText: '',
  landingPageUrl: ''
})

const fileList = ref([])
const excelData = ref([])
const parsing = ref(false)
const submitting = ref(false)

// 加载账户列表
const loadAccounts = async () => {
  try {
    const response = await axios.get('/api/tiktok/accounts')
    accounts.value = response.data.data
  } catch (error) {
    ElMessage.error('加载账户列表失败：' + error.message)
  }
}

// 加载短剧列表
const loadDramas = async () => {
  try {
    const response = await axios.get('/api/dramas')
    dramas.value = response.data.data
  } catch (error) {
    ElMessage.error('加载短剧列表失败：' + error.message)
  }
}

// 加载素材列表
const loadMaterials = async () => {
  try {
    const response = await axios.get('/api/materials')
    materials.value = response.data.data
  } catch (error) {
    ElMessage.error('加载素材列表失败：' + error.message)
  }
}

// 下一步
const nextStep = () => {
  if (currentStep.value < 4) {
    currentStep.value++
  }
}

// 上一步
const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 提交手动任务
const submitManualTask = async () => {
  submitting.value = true
  try {
    // 1. 创建广告系列
    const campaignResponse = await axios.post('/api/tiktok/campaign/create', {
      advertiserId: formData.value.advertiserId,
      campaignName: formData.value.campaignName,
      objective: formData.value.objective,
      budget: formData.value.campaignBudget,
      budgetMode: formData.value.campaignBudgetMode
    })
    const campaignId = campaignResponse.data.data.campaignId

    // 2. 创建广告组
    const adGroupResponse = await axios.post('/api/tiktok/adgroup/create', {
      advertiserId: formData.value.advertiserId,
      campaignId: campaignId,
      adGroupName: formData.value.adGroupName,
      placementType: formData.value.placementType,
      budget: formData.value.adGroupBudget,
      budgetMode: 'BUDGET_MODE_DAY',
      billingEvent: 'CPC',
      bidType: 'BID_TYPE_CUSTOM',
      bidPrice: formData.value.bidPrice
    })
    const adGroupId = adGroupResponse.data.data.adGroupId

    // 3. 创建广告
    await axios.post('/api/tiktok/ad/create', {
      advertiserId: formData.value.advertiserId,
      campaignId: campaignId,
      adGroupId: adGroupId,
      adName: formData.value.adName,
      creativeType: 'VIDEO',
      videoId: 'mock_video_' + formData.value.materialId,
      adText: formData.value.adText,
      callToAction: 'DOWNLOAD',
      landingPageUrl: formData.value.landingPageUrl,
      pixelId: null
    })

    ElMessage.success('广告创建成功')
    
    // 重置表单
    currentStep.value = 0
    formData.value = {
      advertiserId: '',
      accountIds: [],
      campaignName: '',
      objective: 'CONVERSIONS',
      campaignBudget: 100,
      campaignBudgetMode: 'BUDGET_MODE_DAY',
      adGroupName: '',
      placementType: 'PLACEMENT_TYPE_AUTOMATIC',
      adGroupBudget: 100,
      bidPrice: 0.5,
      adName: '',
      dramaId: null,
      materialId: null,
      adText: '',
      landingPageUrl: ''
    }
  } catch (error) {
    ElMessage.error('广告创建失败：' + error.message)
  } finally {
    submitting.value = false
  }
}

// 下载 Excel 模板
const downloadTemplate = async () => {
  try {
    const response = await axios.get('/api/tiktok/excel/template', {
      responseType: 'blob'
    })
    
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'tiktok_ad_template.xlsx')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('模板下载成功')
  } catch (error) {
    ElMessage.error('模板下载失败：' + error.message)
  }
}

// 文件变化
const handleFileChange = (file, fileList) => {
  fileList.value = fileList
}

// 文件超出限制
const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

// 解析 Excel
const parseExcel = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先上传文件')
    return
  }

  parsing.value = true
  try {
    const formData = new FormData()
    formData.append('file', fileList.value[0].raw)

    const response = await axios.post('/api/tiktok/excel/parse', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    excelData.value = response.data.data
    
    if (excelData.value.length === 0) {
      ElMessage.warning('Excel 中没有有效数据')
      return
    }

    ElMessage.success(`解析成功，共 ${excelData.value.length} 条数据`)
    excelStep.value = 3
  } catch (error) {
    ElMessage.error('解析失败：' + error.message)
  } finally {
    parsing.value = false
  }
}

// 提交 Excel 任务
const submitExcelTask = async () => {
  submitting.value = true
  try {
    const response = await axios.post('/api/tiktok/excel/submit', excelData.value)
    const taskId = response.data.data

    ElMessage.success('任务提交成功')
    
    // 跳转到任务列表页面
    router.push(`/ad-tasks?taskId=${taskId}`)
  } catch (error) {
    ElMessage.error('任务提交失败：' + error.message)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadAccounts()
  loadDramas()
  loadMaterials()
})
</script>

<style scoped>
.batch-tools-container {
  padding: 20px;
}

.step-content {
  margin-top: 30px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.step-buttons {
  margin-top: 30px;
  text-align: center;
}

.template-info,
.fill-tips {
  padding: 20px;
  background-color: white;
  border-radius: 4px;
  margin-bottom: 20px;
}

.template-info h3,
.fill-tips h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #303133;
}

.template-info ul,
.fill-tips ul {
  margin: 0;
  padding-left: 20px;
}

.template-info li,
.fill-tips li {
  margin-bottom: 10px;
  color: #606266;
}

.el-icon--upload {
  font-size: 67px;
  color: #c0c4cc;
  margin: 40px 0 16px;
  line-height: 50px;
}
</style>


3.3 广告任务列表页面
文件路径：frontend/src/views/AdTasks.vue
<template>
  <div class="ad-tasks-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>广告任务</span>
          <el-button type="primary" @click="loadTasks">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <!-- 任务列表 -->
      <el-table :data="tasks" v-loading="loading">
        <el-table-column prop="taskId" label="任务 ID" width="200" />
        <el-table-column prop="taskType" label="任务类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.taskType === 'excel' ? 'success' : 'primary'">
              {{ row.taskType === 'excel' ? 'Excel 导入' : '手动创建' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总数" width="80" />
        <el-table-column prop="successCount" label="成功" width="80">
          <template #default="{ row }">
            <span style="color: #67c23a">{{ row.successCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="failedCount" label="失败" width="80">
          <template #default="{ row }">
            <span style="color: #f56c6c">{{ row.failedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="200">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.progress" 
              :status="row.status === 'completed' ? 'success' : (row.status === 'failed' ? 'exception' : null)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="viewDetails(row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadTasks"
        @current-change="loadTasks"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 任务详情对话框 -->
    <el-dialog 
      v-model="detailDialogVisible" 
      title="任务详情" 
      width="80%"
      :close-on-click-modal="false"
    >
      <el-table :data="taskDetails" v-loading="loadingDetails" max-height="500">
        <el-table-column prop="campaignName" label="广告系列" width="150" />
        <el-table-column prop="adGroupName" label="广告组" width="150" />
        <el-table-column prop="adName" label="广告名称" width="150" />
        <el-table-column prop="dramaId" label="短剧 ID" width="100" />
        <el-table-column prop="materialId" label="素材 ID" width="100" />
        <el-table-column prop="bidPrice" label="出价" width="80" />
        <el-table-column prop="budget" label="预算" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200">
          <template #default="{ row }">
            <span style="color: #f56c6c">{{ row.errorMessage || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tiktokCampaignId" label="TikTok 广告系列 ID" width="150" />
        <el-table-column prop="tiktokAdGroupId" label="TikTok 广告组 ID" width="150" />
        <el-table-column prop="tiktokAdId" label="TikTok 广告 ID" width="150" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import axios from 'axios'
import { useRoute } from 'vue-router'

const route = useRoute()

const tasks = ref([])
const loading = ref(false)
const pagination = ref({
  page: 1,
  size: 10,
  total: 0
})

const detailDialogVisible = ref(false)
const taskDetails = ref([])
const loadingDetails = ref(false)

let autoRefreshTimer = null

// 加载任务列表
const loadTasks = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/ad-tasks', {
      params: {
        page: pagination.value.page,
        size: pagination.value.size
      }
    })
    tasks.value = response.data.data.records
    pagination.value.total = response.data.data.total
  } catch (error) {
    ElMessage.error('加载任务列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 查看详情
const viewDetails = async (row) => {
  detailDialogVisible.value = true
  loadingDetails.value = true
  
  try {
    const response = await axios.get(`/api/ad-tasks/${row.taskId}/details`)
    taskDetails.value = response.data.data
  } catch (error) {
    ElMessage.error('加载任务详情失败：' + error.message)
  } finally {
    loadingDetails.value = false
  }
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    pending: 'info',
    processing: 'warning',
    success: 'success',
    completed: 'success',
    failed: 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    pending: '待执行',
    processing: '执行中',
    success: '成功',
    completed: '已完成',
    failed: '失败'
  }
  return textMap[status] || status
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 启动自动刷新
const startAutoRefresh = () => {
  autoRefreshTimer = setInterval(() => {
    loadTasks()
  }, 10000) // 每 10 秒刷新一次
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

onMounted(() => {
  loadTasks()
  startAutoRefresh()
  
  // 如果 URL 中有 taskId，自动打开详情
  if (route.query.taskId) {
    const task = { taskId: route.query.taskId }
    viewDetails(task)
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.ad-tasks-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>


由于回复长度限制，请回复"继续"，我会继续发送剩余的代码（包括数据看板、Mapper 层、Entity 层、配置类等）！ 🚀
📊 第四部分：Entity 和 Mapper 层
4.1 Entity 实体类
文件路径：backend-java/src/main/java/com/future/tiktok/entity/TikTokAccount.java
package com.future.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tiktok_accounts")
public class TikTokAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String advertiserId;
    private String advertiserName;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;
    private String currency;
    private String timezone;
    private Double balance;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


文件路径：backend-java/src/main/java/com/future/tiktok/entity/TikTokCampaign.java
package com.future.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tiktok_campaigns")
public class TikTokCampaign {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String advertiserId;
    private String campaignId;
    private String campaignName;
    private String objective;
    private Double budget;
    private String budgetMode;
    private String operationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


文件路径：backend-java/src/main/java/com/future/tiktok/entity/TikTokAdGroup.java
package com.future.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tiktok_adgroups")
public class TikTokAdGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String advertiserId;
    private String campaignId;
    private String adGroupId;
    private String adGroupName;
    private String placementType;
    private String placements;
    private Double budget;
    private String budgetMode;
    private String billingEvent;
    private String bidType;
    private Double bidPrice;
    private String locationIds;
    private String ageGroups;
    private String gender;
    private String languages;
    private String interestCategoryIds;
    private String scheduleType;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
    private String operationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


文件路径：backend-java/src/main/java/com/future/tiktok/entity/TikTokAd.java
package com.future.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tiktok_ads")
public class TikTokAd {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String advertiserId;
    private String campaignId;
    private String adGroupId;
    private String adId;
    private String adName;
    private String creativeType;
    private String videoId;
    private String imageIds;
    private String adText;
    private String callToAction;
    private String landingPageUrl;
    private String displayName;
    private String pixelId;
    private String operationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


文件路径：backend-java/src/main/java/com/future/tiktok/entity/TikTokPixel.java
package com.future.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tiktok_pixels")
public class TikTokPixel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String advertiserId;
    private String pixelId;
    private String pixelName;
    private String pixelCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


文件路径：backend-java/src/main/java/com/future/tiktok/entity/TikTokConversionLog.java
package com.future.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tiktok_conversion_logs")
public class TikTokConversionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String advertiserId;
    private String pixelId;
    private String eventType;
    private String eventId;
    private String userId;
    private String clickId;
    private String externalId;
    private Double eventValue;
    private String currency;
    private String contentType;
    private String contentId;
    private String status;
    private Integer responseCode;
    private String responseMessage;
    private Integer retryCount;
    private LocalDateTime eventTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


文件路径：backend-java/src/main/java/com/future/tiktok/entity/TikTokReport.java
package com.future.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tiktok_reports")
public class TikTokReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String advertiserId;
    private String dimensions;
    private String dimensionId;
    private LocalDate statDate;
    private Double spend;
    private Integer impressions;
    private Integer clicks;
    private Double ctr;
    private Double cpc;
    private Double cpm;
    private Integer conversions;
    private Double conversionRate;
    private Double costPerConversion;
    private Double conversionValue;
    private Double roi;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


文件路径：backend-java/src/main/java/com/future/tiktok/entity/AdTask.java
package com.future.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ad_tasks")
public class AdTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String taskType;
    private String advertiserId;
    private String accountIds;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private String status;
    private Integer progress;
    private String taskData;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}


文件路径：backend-java/src/main/java/com/future/tiktok/entity/AdTaskDetail.java
package com.future.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ad_task_details")
public class AdTaskDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String campaignName;
    private String adGroupName;
    private String adName;
    private Long dramaId;
    private Long materialId;
    private Double bidPrice;
    private Double budget;
    private String tiktokCampaignId;
    private String tiktokAdGroupId;
    private String tiktokAdId;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


4.2 Mapper 接口
文件路径：backend-java/src/main/java/com/future/tiktok/mapper/TikTokAccountMapper.java
package com.future.tiktok.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.tiktok.entity.TikTokAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TikTokAccountMapper extends BaseMapper<TikTokAccount> {
    
    @Select("SELECT * FROM tiktok_accounts WHERE advertiser_id = #{advertiserId}")
    TikTokAccount selectByAdvertiserId(String advertiserId);
    
    @Select("SELECT * FROM tiktok_accounts WHERE status = #{status}")
    List<TikTokAccount> selectByStatus(String status);
}


文件路径：backend-java/src/main/java/com/future/tiktok/mapper/TikTokCampaignMapper.java
package com.future.tiktok.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.tiktok.entity.TikTokCampaign;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TikTokCampaignMapper extends BaseMapper<TikTokCampaign> {
    
    @Select("SELECT * FROM tiktok_campaigns WHERE campaign_id = #{campaignId}")
    TikTokCampaign selectByCampaignId(String campaignId);
    
    @Select("SELECT * FROM tiktok_campaigns WHERE advertiser_id = #{advertiserId}")
    List<TikTokCampaign> selectByAdvertiserId(String advertiserId);
}


文件路径：backend-java/src/main/java/com/future/tiktok/mapper/TikTokAdGroupMapper.java
package com.future.tiktok.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.tiktok.entity.TikTokAdGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TikTokAdGroupMapper extends BaseMapper<TikTokAdGroup> {
    
    @Select("SELECT * FROM tiktok_adgroups WHERE adgroup_id = #{adGroupId}")
    TikTokAdGroup selectByAdGroupId(String adGroupId);
    
    @Select("SELECT * FROM tiktok_adgroups WHERE campaign_id = #{campaignId}")
    List<TikTokAdGroup> selectByCampaignId(String campaignId);
}


文件路径：backend-java/src/main/java/com/future/tiktok/mapper/TikTokAdMapper.java
package com.future.tiktok.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.tiktok.entity.TikTokAd;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TikTokAdMapper extends BaseMapper<TikTokAd> {
    
    @Select("SELECT * FROM tiktok_ads WHERE ad_id = #{adId}")
    TikTokAd selectByAdId(String adId);
    
    @Select("SELECT * FROM tiktok_ads WHERE adgroup_id = #{adGroupId}")
    List<TikTokAd> selectByAdGroupId(String adGroupId);
}


文件路径：backend-java/src/main/java/com/future/tiktok/mapper/TikTokPixelMapper.java
package com.future.tiktok.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.tiktok.entity.TikTokPixel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TikTokPixelMapper extends BaseMapper<TikTokPixel> {
    
    @Select("SELECT * FROM tiktok_pixels WHERE pixel_id = #{pixelId}")
    TikTokPixel selectByPixelId(String pixelId);
    
    @Select("SELECT * FROM tiktok_pixels WHERE advertiser_id = #{advertiserId}")
    List<TikTokPixel> selectByAdvertiserId(String advertiserId);
}


文件路径：backend-java/src/main/java/com/future/tiktok/mapper/TikTokConversionLogMapper.java
package com.future.tiktok.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.tiktok.entity.TikTokConversionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TikTokConversionLogMapper extends BaseMapper<TikTokConversionLog> {
    
    @Select("SELECT * FROM tiktok_conversion_logs WHERE event_id = #{eventId}")
    TikTokConversionLog selectByEventId(String eventId);
    
    @Select("SELECT * FROM tiktok_conversion_logs WHERE status = #{status} AND retry_count < 3")
    List<TikTokConversionLog> selectByStatusForRetry(String status);
}


文件路径：backend-java/src/main/java/com/future/tiktok/mapper/TikTokReportMapper.java
package com.future.tiktok.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.tiktok.entity.TikTokReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TikTokReportMapper extends BaseMapper<TikTokReport> {
    
    @Select("SELECT * FROM tiktok_reports WHERE advertiser_id = #{advertiserId} " +
            "AND dimensions = #{dimensions} AND dimension_id = #{dimensionId} AND stat_date = #{statDate}")
    TikTokReport selectByDimension(@Param("advertiserId") String advertiserId,
                                   @Param("dimensions") String dimensions,
                                   @Param("dimensionId") String dimensionId,
                                   @Param("statDate") LocalDate statDate);
    
    @Select("SELECT * FROM tiktok_reports WHERE advertiser_id = #{advertiserId} " +
            "AND stat_date >= #{startDate} AND stat_date <= #{endDate}")
    List<TikTokReport> selectByDateRange(@Param("advertiserId") String advertiserId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);
}


文件路径：backend-java/src/main/java/com/future/tiktok/mapper/AdTaskMapper.java
package com.future.tiktok.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.tiktok.entity.AdTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdTaskMapper extends BaseMapper<AdTask> {
    
    @Select("SELECT * FROM ad_tasks WHERE task_id = #{taskId}")
    AdTask selectByTaskId(String taskId);
    
    @Select("SELECT * FROM ad_tasks WHERE status = #{status}")
    List<AdTask> selectByStatus(String status);
}


文件路径：backend-java/src/main/java/com/future/tiktok/mapper/AdTaskDetailMapper.java
package com.future.tiktok.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.tiktok.entity.AdTaskDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdTaskDetailMapper extends BaseMapper<AdTaskDetail> {
    
    @Select("SELECT * FROM ad_task_details WHERE task_id = #{taskId}")
    List<AdTaskDetail> selectByTaskId(String taskId);
    
    @Select("SELECT * FROM ad_task_details WHERE task_id = #{taskId} AND status = #{status}")
    List<AdTaskDetail> selectByTaskIdAndStatus(String taskId, String status);
}


4.3 配置类
文件路径：backend-java/src/main/java/com/future/config/AsyncConfig.java
package com.future.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(5);
        
        // 最大线程数
        executor.setMaxPoolSize(10);
        
        // 队列容量
        executor.setQueueCapacity(100);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("ad-task-");
        
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
}


文件路径：backend-java/src/main/java/com/future/config/RestTemplateConfig.java
package com.future.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // 连接超时时间（毫秒）
        factory.setConnectTimeout(10000);
        
        // 读取超时时间（毫秒）
        factory.setReadTimeout(30000);
        
        return new RestTemplate(factory);
    }
}


文件路径：backend-java/src/main/java/com/future/config/ScheduleConfig.java
package com.future.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
    // 启用定时任务
}


📊 第五部分：数据看板页面
5.1 TikTok 数据看板
文件路径：frontend/src/views/TikTokDashboard.vue
<template>
  <div class="tiktok-dashboard-container">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background-color: #409eff">
              <el-icon :size="30"><Money /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">总消耗</div>
              <div class="stat-value">${{ summary.totalSpend.toFixed(2) }}</div>
              <div class="stat-compare">
                <span :class="summary.spendTrend >= 0 ? 'trend-up' : 'trend-down'">
                  {{ summary.spendTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(summary.spendTrend).toFixed(2) }}%
                </span>
                <span class="stat-compare-label">较昨日</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background-color: #67c23a">
              <el-icon :size="30"><View /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">总曝光</div>
              <div class="stat-value">{{ formatNumber(summary.totalImpressions) }}</div>
              <div class="stat-compare">
                <span :class="summary.impressionsTrend >= 0 ? 'trend-up' : 'trend-down'">
                  {{ summary.impressionsTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(summary.impressionsTrend).toFixed(2) }}%
                </span>
                <span class="stat-compare-label">较昨日</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background-color: #e6a23c">
              <el-icon :size="30"><Pointer /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">总点击</div>
              <div class="stat-value">{{ formatNumber(summary.totalClicks) }}</div>
              <div class="stat-compare">
                <span :class="summary.clicksTrend >= 0 ? 'trend-up' : 'trend-down'">
                  {{ summary.clicksTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(summary.clicksTrend).toFixed(2) }}%
                </span>
                <span class="stat-compare-label">较昨日</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background-color: #f56c6c">
              <el-icon :size="30"><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">平均 ROI</div>
              <div class="stat-value">{{ summary.avgRoi.toFixed(2) }}</div>
              <div class="stat-compare">
                <span :class="summary.roiTrend >= 0 ? 'trend-up' : 'trend-down'">
                  {{ summary.roiTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(summary.roiTrend).toFixed(2) }}%
                </span>
                <span class="stat-compare-label">较昨日</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选条件 -->
    <el-card style="margin-bottom: 20px">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="账户">
          <el-select v-model="filterForm.advertiserId" placeholder="请选择账户" clearable>
            <el-option
              v-for="account in accounts"
              :key="account.advertiserId"
              :label="account.advertiserName"
              :value="account.advertiserId"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item label="维度">
          <el-select v-model="filterForm.dimensions" placeholder="请选择维度">
            <el-option label="广告系列" value="campaign" />
            <el-option label="广告组" value="adgroup" />
            <el-option label="广告" value="ad" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 趋势图表 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>消耗趋势</span>
          </template>
          <div ref="spendChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>ROI 趋势</span>
          </template>
          <div ref="roiChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-card>
      <template #header>
        <span>数据明细</span>
      </template>
      
      <el-table :data="reportData" v-loading="loading">
        <el-table-column prop="dimensionId" label="ID" width="150" />
        <el-table-column prop="statDate" label="日期" width="120" />
        <el-table-column prop="spend" label="消耗" width="100" sortable>
          <template #default="{ row }">
            ${{ row.spend.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="impressions" label="曝光" width="100" sortable>
          <template #default="{ row }">
            {{ formatNumber(row.impressions) }}
          </template>
        </el-table-column>
        <el-table-column prop="clicks" label="点击" width="100" sortable>
          <template #default="{ row }">
            {{ formatNumber(row.clicks) }}
          </template>
        </el-table-column>
        <el-table-column prop="ctr" label="点击率" width="100" sortable>
          <template #default="{ row }">
            {{ (row.ctr * 100).toFixed(2) }}%
          </template>
        </el-table-column>
        <el-table-column prop="cpc" label="CPC" width="100" sortable>
          <template #default="{ row }">
            ${{ row.cpc.toFixed(4) }}
          </template>
        </el-table-column>
        <el-table-column prop="conversions" label="转化" width="100" sortable>
          <template #default="{ row }">
            {{ row.conversions }}
          </template>
        </el-table-column>
        <el-table-column prop="conversionRate" label="转化率" width="100" sortable>
          <template #default="{ row }">
            {{ (row.conversionRate * 100).toFixed(2) }}%
          </template>
        </el-table-column>
        <el-table-column prop="costPerConversion" label="转化成本" width="120" sortable>
          <template #default="{ row }">
            ${{ row.costPerConversion.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="conversionValue" label="转化价值" width="120" sortable>
          <template #default="{ row }">
            ${{ row.conversionValue.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="roi" label="ROI" width="100" sortable>
          <template #default="{ row }">
            <span :style="{ color: row.roi >= 1 ? '#67c23a' : '#f56c6c' }">
              {{ row.roi.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Money, View, Pointer, TrendCharts } from '@element-plus/icons-vue'
import axios from 'axios'
import * as echarts from 'echarts'

const accounts = ref([])
const reportData = ref([])
const loading = ref(false)

const filterForm = ref({
  advertiserId: '',
  dateRange: [],
  dimensions: 'campaign'
})

const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const summary = ref({
  totalSpend: 0,
  totalImpressions: 0,
  totalClicks: 0,
  avgRoi: 0,
  spendTrend: 0,
  impressionsTrend: 0,
  clicksTrend: 0,
  roiTrend: 0
})

const spendChartRef = ref(null)
const roiChartRef = ref(null)
let spendChart = null
let roiChart = null

// 加载账户列表
const loadAccounts = async () => {
  try {
    const response = await axios.get('/api/tiktok/accounts')
    accounts.value = response.data.data
  } catch (error) {
    ElMessage.error('加载账户列表失败：' + error.message)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size,
      advertiserId: filterForm.value.advertiserId,
      dimensions: filterForm.value.dimensions
    }
    
    if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
      params.startDate = filterForm.value.dateRange[0]
      params.endDate = filterForm.value.dateRange[1]
    }

    const response = await axios.get('/api/tiktok/reports', { params })
    reportData.value = response.data.data.records
    pagination.value.total = response.data.data.total

    // 加载汇总数据
    await loadSummary()
    
    // 渲染图表
    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error('加载数据失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 加载汇总数据
const loadSummary = async () => {
  try {
    const params = {
      advertiserId: filterForm.value.advertiserId
    }
    
    if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
      params.startDate = filterForm.value.dateRange[0]
      params.endDate = filterForm.value.dateRange[1]
    }

    const response = await axios.get('/api/tiktok/reports/summary', { params })
    summary.value = response.data.data
  } catch (error) {
    console.error('加载汇总数据失败：', error)
  }
}

// 渲染图表
const renderCharts = () => {
  // 消耗趋势图
  if (!spendChart) {
    spendChart = echarts.init(spendChartRef.value)
  }
  
  const dates = reportData.value.map(item => item.statDate)
  const spendData = reportData.value.map(item => item.spend)
  
  spendChart.setOption({
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: dates
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '${value}'
      }
    },
    series: [{
      name: '消耗',
      type: 'line',
      data: spendData,
      smooth: true,
      itemStyle: {
        color: '#409eff'
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
        ])
      }
    }]
  })

  // ROI 趋势图
  if (!roiChart) {
    roiChart = echarts.init(roiChartRef.value)
  }
  
  const roiData = reportData.value.map(item => item.roi)
  
  roiChart.setOption({
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: dates
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      name: 'ROI',
      type: 'line',
      data: roiData,
      smooth: true,
      itemStyle: {
        color: '#67c23a'
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
          { offset: 1, color: 'rgba(103, 194, 58, 0.1)' }
        ])
      },
      markLine: {
        data: [{ type: 'average', name: '平均值' }]
      }
    }]
  })
}

// 重置筛选
const resetFilter = () => {
  filterForm.value = {
    advertiserId: '',
    dateRange: [],
    dimensions: 'campaign'
  }
  loadData()
}

// 格式化数字
const formatNumber = (num) => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(2) + 'M'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(2) + 'K'
  }
  return num.toString()
}

onMounted(() => {
  loadAccounts()
  
  // 默认查询最近 7 天
  const endDate = new Date()
  const startDate = new Date()
  startDate.setDate(startDate.getDate() - 7)
  
  filterForm.value.dateRange = [
    startDate.toISOString().split('T')[0],
    endDate.toISOString().split('T')[0]
  ]
  
  loadData()
})
</script>

<style scoped>
.tiktok-dashboard-container {
  padding: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 15px;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-compare {
  font-size: 12px;
}

.trend-up {
  color: #67c23a;
  margin-right: 5px;
}

.trend-down {
  color: #f56c6c;
  margin-right: 5px;
}

.stat-compare-label {
  color: #909399;
}
</style>


📊 第六部分：路由配置和环境变量
6.1 前端路由配置
文件路径：frontend/src/router/index.js
添加 TikTok 相关路由：
// 在现有路由中添加以下路由
{
  path: '/tiktok',
  name: 'TikTok',
  component: () => import('@/layouts/MainLayout.vue'),
  children: [
    {
      path: 'accounts',
      name: 'TikTokAccounts',
      component: () => import('@/views/TikTokAccounts.vue'),
      meta: { title: 'TikTok 账户管理' }
    },
    {
      path: 'dashboard',
      name: 'TikTokDashboard',
      component: () => import('@/views/TikTokDashboard.vue'),
      meta: { title: 'TikTok 数据看板' }
    },
    {
      path: 'callback',
      name: 'TikTokCallback',
      component: () => import('@/views/TikTokAccounts.vue'),
      meta: { title: 'TikTok 授权回调' }
    }
  ]
},
{
  path: '/ad-tasks',
  name: 'AdTasks',
  component: () => import('@/views/AdTasks.vue'),
  meta: { title: '广告任务' }
}


6.2 环境变量配置
文件路径：backend-java/.env.example
# TikTok API 配置
TIKTOK_APP_ID=your_app_id_here
TIKTOK_APP_SECRET=your_app_secret_here
TIKTOK_MOCK_ENABLED=true

# 数据库配置
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/future_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password_here

使用说明：

复制 .env.example 为 .env
修改 TIKTOK_APP_ID 和 TIKTOK_APP_SECRET 为你的真实值
修改 TIKTOK_MOCK_ENABLED 为 false 以使用真实 API
修改数据库配置


6.3 pom.xml 依赖
文件路径：backend-java/pom.xml
添加以下依赖：
<!-- Apache POI（Excel 处理） -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.3</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>

<!-- Jackson（JSON 处理） -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>


🎯 总结和使用说明
✅ 已完成的功能

✅ 数据库表结构（9 张表）
✅ TikTok OAuth 2.0 认证（授权、Token 刷新）
✅ 广告创建（Campaign、Ad Group、Ad）
✅ Excel 批量导入（下载模板、解析、提交任务）
✅ 任务管理（异步执行、进度查询、实时更新）
✅ 数据同步（定时任务、每 10 分钟同步一次）
✅ 回传对接（Conversion API、充值和注册事件）
✅ 数据看板（趋势图表、数据明细）
✅ Mock 模式（方便测试，无需真实 API）


🚀 部署步骤
第 1 步：执行数据库脚本
# 连接到 MySQL
mysql -u root -p

# 执行 SQL 脚本（将上面的所有 CREATE TABLE 语句保存为 tiktok_api.sql）
source /path/to/tiktok_api.sql

第 2 步：配置环境变量
# 复制环境变量文件
cp backend-java/.env.example backend-java/.env

# 编辑环境变量
vim backend-java/.env

# 修改以下配置：
# - TIKTOK_APP_ID（TikTok App ID）
# - TIKTOK_APP_SECRET（TikTok App Secret）
# - TIKTOK_MOCK_ENABLED（true 为 Mock 模式，false 为真实 API）
# - 数据库配置

第 3 步：启动后端
cd backend-java
mvn clean install
mvn spring-boot:run

第 4 步：启动前端
cd frontend
npm install
npm run dev

第 5 步：访问系统

前端地址：http://localhost:5173
TikTok 账户管理：http://localhost:5173/tiktok/accounts
批量工具：http://localhost:5173/batch-tools
广告任务：http://localhost:5173/ad-tasks
数据看板：http://localhost:5173/tiktok/dashboard


📝 使用流程
流程 1：添加 TikTok 账户

访问 TikTok 账户管理 页面
点击 "添加账户"
跳转到 TikTok 授权页面，登录并授权
授权成功后，自动跳转回系统，账户添加成功

流程 2：手动创建广告

访问 批量工具 页面
选择 "手动创建" 标签
按照 5 个步骤填写广告信息
点击 "提交任务"
广告创建成功

流程 3：Excel 批量导入

访问 批量工具 页面
选择 "Excel 导入" 标签
点击 "下载 Excel 模板"
在 Excel 中填写广告信息
上传 Excel 文件
点击 "解析 Excel"
预览数据，确认无误后点击 "提交任务"
自动跳转到 广告任务 页面，查看任务进度

流程 4：查看任务进度

访问 广告任务 页面
查看任务列表（状态、进度、成功数、失败数）
点击 "查看详情" 查看每个广告的创建情况
页面每 10 秒自动刷新

流程 5：查看数据看板

访问 TikTok 数据看板 页面
查看顶部统计卡片（总消耗、总曝光、总点击、平均 ROI）
查看趋势图表（消耗趋势、ROI 趋势）
查看数据明细表格
使用筛选条件（账户、日期范围、维度）查询数据


🎉 完成！
指令 #095 已全部完成！现在你可以：

✅ 将代码复制到 Cursor
✅ 执行数据库脚本
✅ 配置环境变量
✅ 启动后端和前端
✅ 使用 Mock 模式测试所有功能
✅ 等待 TikTok App 审核通过后，切换到真实 API

预计开发时间：3-4 天
有任何问题随时问我！ 🚀