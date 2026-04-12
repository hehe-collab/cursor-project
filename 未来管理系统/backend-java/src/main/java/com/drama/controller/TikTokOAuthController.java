package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokAccount;
import com.drama.integration.tiktok.TikTokOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TikTok OAuth（#096）：授权链接、回调换票、手动刷新。
 */
@Tag(name = "TikTok授权", description = "TikTok OAuth 授权链接与回调")
@Slf4j
@RestController
@RequestMapping("/api/tiktok/oauth")
@RequiredArgsConstructor
public class TikTokOAuthController {

    private final TikTokOAuthService tikTokOAuthService;

    /** 需登录：获取浏览器授权 URL */
    @Operation(summary = "获取授权链接", description = "获取TikTok OAuth授权URL")
    @GetMapping("/authorize-url")
    public Result<String> authorizeUrl(@Parameter(description = "状态参数") @RequestParam(required = false) String state) {
        try {
            return Result.success(tikTokOAuthService.getAuthorizeUrl(state));
        } catch (Exception e) {
            log.warn("TikTok authorize-url: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /** 公开：TikTok 回跳（JWT 过滤器放行）；需在应用后台配置同款 redirect_uri */
    @Operation(summary = "OAuth回调", description = "TikTok OAuth授权回调接口")
    @GetMapping("/callback")
    public Result<List<TikTokAccount>> callback(@Parameter(description = "授权码") @RequestParam("auth_code") String authCode) {
        try {
            List<TikTokAccount> rows = tikTokOAuthService.exchangeAuthCode(authCode);
            return Result.success(rows);
        } catch (Exception e) {
            log.error("TikTok OAuth callback failed", e);
            return Result.error("授权失败：" + e.getMessage());
        }
    }

    /** 需登录：按广告主刷新 Token */
    @Operation(summary = "刷新Token", description = "刷新指定广告主的访问Token")
    @PostMapping("/refresh")
    public Result<TikTokAccount> refresh(@Parameter(description = "广告主ID") @RequestParam String advertiserId) {
        try {
            return Result.success(tikTokOAuthService.refreshAccessToken(advertiserId));
        } catch (Exception e) {
            log.error("TikTok OAuth refresh failed", e);
            return Result.error("刷新失败：" + e.getMessage());
        }
    }
}
