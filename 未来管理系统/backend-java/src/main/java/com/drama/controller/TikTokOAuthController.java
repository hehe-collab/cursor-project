package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokAccount;
import com.drama.integration.tiktok.TikTokOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.HtmlUtils;
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
    public ResponseEntity<?> callback(
            @Parameter(description = "授权码") @RequestParam("auth_code") String authCode,
            @Parameter(description = "状态参数") @RequestParam(required = false) String state,
            HttpServletRequest request) {
        try {
            List<TikTokAccount> rows = tikTokOAuthService.exchangeAuthCode(authCode);
            if (prefersHtml(request)) {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(buildCallbackHtml(rows, state, null));
            }
            return ResponseEntity.ok(Result.success(rows));
        } catch (Exception e) {
            log.error("TikTok OAuth callback failed", e);
            if (prefersHtml(request)) {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(buildCallbackHtml(List.of(), state, "授权失败：" + e.getMessage()));
            }
            return ResponseEntity.ok(Result.error("授权失败：" + e.getMessage()));
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

    private boolean prefersHtml(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_HTML_VALUE);
    }

    private String buildCallbackHtml(List<TikTokAccount> rows, String state, String errorMessage) {
        String title = errorMessage == null ? "TikTok 授权完成" : "TikTok 授权失败";
        String statusLine =
                errorMessage == null
                        ? "本次已写入 " + rows.size() + " 个 TikTok 授权账户。"
                        : HtmlUtils.htmlEscape(errorMessage);
        String target =
                state != null && !state.isBlank()
                        ? "<p class=\"meta\">状态参数：" + HtmlUtils.htmlEscape(state) + "</p>"
                        : "";
        String accounts = rows.isEmpty()
                ? "<li>未返回可用 advertiser_id，请检查 TikTok 授权范围。</li>"
                : rows.stream()
                        .map(
                                row ->
                                        "<li><strong>"
                                                + HtmlUtils.htmlEscape(row.getAdvertiserId())
                                                + "</strong>"
                                                + (row.getAdvertiserName() != null && !row.getAdvertiserName().isBlank()
                                                        ? " - " + HtmlUtils.htmlEscape(row.getAdvertiserName())
                                                        : "")
                                                + "</li>")
                        .reduce("", String::concat);
        String hint =
                errorMessage == null
                        ? "请返回管理系统账户管理页，点击“查询”刷新授权状态。"
                        : "请关闭当前页后返回管理系统，检查配置或重新发起授权。";
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>%s</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f5f7fa; color: #303133; margin: 0; padding: 32px 16px; }
                    .card { max-width: 720px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 8px 24px rgba(31, 35, 41, 0.08); }
                    h1 { margin: 0 0 12px; font-size: 24px; }
                    p { margin: 0 0 12px; line-height: 1.7; }
                    .meta { color: #909399; font-size: 13px; }
                    ul { margin: 16px 0; padding-left: 20px; }
                    li { margin-bottom: 8px; line-height: 1.6; }
                    .success { color: #67c23a; }
                    .error { color: #f56c6c; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>%s</h1>
                    <p class="%s">%s</p>
                    %s
                    <ul>%s</ul>
                    <p>%s</p>
                  </div>
                </body>
                </html>
                """
                .formatted(
                        title,
                        title,
                        errorMessage == null ? "success" : "error",
                        statusLine,
                        target,
                        accounts,
                        hint);
    }
}
