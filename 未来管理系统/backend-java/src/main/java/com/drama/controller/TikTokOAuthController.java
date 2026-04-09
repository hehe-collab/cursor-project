package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokAccount;
import com.drama.integration.tiktok.TikTokOAuthService;
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
@Slf4j
@RestController
@RequestMapping("/api/tiktok/oauth")
@RequiredArgsConstructor
public class TikTokOAuthController {

    private final TikTokOAuthService tikTokOAuthService;

    /** 需登录：获取浏览器授权 URL */
    @GetMapping("/authorize-url")
    public Result<String> authorizeUrl(@RequestParam(required = false) String state) {
        try {
            return Result.success(tikTokOAuthService.getAuthorizeUrl(state));
        } catch (Exception e) {
            log.warn("TikTok authorize-url: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /** 公开：TikTok 回跳（JWT 过滤器放行）；需在应用后台配置同款 redirect_uri */
    @GetMapping("/callback")
    public Result<List<TikTokAccount>> callback(@RequestParam("auth_code") String authCode) {
        try {
            List<TikTokAccount> rows = tikTokOAuthService.exchangeAuthCode(authCode);
            return Result.success(rows);
        } catch (Exception e) {
            log.error("TikTok OAuth callback failed", e);
            return Result.error("授权失败：" + e.getMessage());
        }
    }

    /** 需登录：按广告主刷新 Token */
    @PostMapping("/refresh")
    public Result<TikTokAccount> refresh(@RequestParam String advertiserId) {
        try {
            return Result.success(tikTokOAuthService.refreshAccessToken(advertiserId));
        } catch (Exception e) {
            log.error("TikTok OAuth refresh failed", e);
            return Result.error("刷新失败：" + e.getMessage());
        }
    }
}
