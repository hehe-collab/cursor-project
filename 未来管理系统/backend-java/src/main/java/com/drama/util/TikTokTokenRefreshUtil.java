package com.drama.util;

import com.drama.config.TikTokConfig;
import com.drama.entity.TikTokAccount;
import com.drama.integration.tiktok.TikTokOAuthService;
import com.drama.mapper.TikTokAccountMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 按 {@link TikTokConfig#getTokenRefreshAdvanceMinutes()} 检测即将过期的 Token 并刷新。
 *
 * <p>实际 HTTP 刷新委托 {@link TikTokOAuthService}（与 {@code drama.tiktok} 配置一致），避免重复实现 OAuth。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TikTokTokenRefreshUtil {

    private final TikTokConfig tiktokConfig;
    private final TikTokAccountMapper accountMapper;
    private final TikTokOAuthService tikTokOAuthService;

    public TikTokAccount checkAndRefreshToken(String advertiserId) {
        TikTokAccount account = accountMapper.selectByAdvertiserId(advertiserId);
        if (account == null) {
            throw new IllegalArgumentException("TikTok account not found: " + advertiserId);
        }
        int advance =
                tiktokConfig.getTokenRefreshAdvanceMinutes() != null
                        ? tiktokConfig.getTokenRefreshAdvanceMinutes()
                        : 5;
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(advance);
        if (account.getTokenExpiresAt() != null && account.getTokenExpiresAt().isBefore(threshold)) {
            log.info("Token 即将过期，刷新 advertiserId={}", advertiserId);
            return tikTokOAuthService.refreshAccessToken(advertiserId);
        }
        return account;
    }

    public TikTokAccount refreshToken(TikTokAccount account) {
        if (account == null || account.getAdvertiserId() == null || account.getAdvertiserId().isBlank()) {
            throw new IllegalArgumentException("TikTok account / advertiserId 无效");
        }
        return tikTokOAuthService.refreshAccessToken(account.getAdvertiserId().trim());
    }

    public void refreshAllExpiringTokens() {
        for (TikTokAccount account : accountMapper.selectByStatus("active")) {
            try {
                checkAndRefreshToken(account.getAdvertiserId());
            } catch (Exception e) {
                log.error(
                        "刷新 Token 失败 advertiserId={}: {}",
                        account.getAdvertiserId(),
                        e.getMessage());
            }
        }
    }
}
