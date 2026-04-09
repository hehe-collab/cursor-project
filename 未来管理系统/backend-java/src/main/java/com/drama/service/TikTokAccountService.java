package com.drama.service;

import com.drama.entity.TikTokAccount;
import com.drama.mapper.TikTokAccountMapper;
import com.drama.util.TikTokTokenRefreshUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TikTokAccountService {

    private final TikTokAccountMapper accountMapper;
    private final TikTokTokenRefreshUtil tokenRefreshUtil;

    public List<TikTokAccount> getAccounts() {
        return accountMapper.selectAllOrderByIdAsc();
    }

    public TikTokAccount getAccountById(Long id) {
        TikTokAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw new IllegalStateException("Account not found: " + id);
        }
        return account;
    }

    public TikTokAccount getAccountByAdvertiserId(String advertiserId) {
        TikTokAccount account = accountMapper.selectByAdvertiserId(advertiserId);
        if (account == null) {
            throw new IllegalStateException("Account not found: " + advertiserId);
        }
        return account;
    }

    public List<TikTokAccount> getAccountsByStatus(String status) {
        return accountMapper.selectByStatus(status);
    }

    @Transactional
    public TikTokAccount upsertAccount(TikTokAccount account) {
        if (account.getTokenExpiresAt() == null && account.getAccessToken() != null) {
            account.setTokenExpiresAt(LocalDateTime.now().plusHours(2));
        }
        if (account.getStatus() == null) {
            account.setStatus("active");
        }
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        accountMapper.upsert(account);
        log.info("Upserted TikTok account: {}", account.getAdvertiserId());
        return accountMapper.selectByAdvertiserId(account.getAdvertiserId());
    }

    @Transactional
    public TikTokAccount updateAccount(Long id, TikTokAccount patch) {
        TikTokAccount existing = getAccountById(id);
        if (patch.getAdvertiserName() != null) {
            existing.setAdvertiserName(patch.getAdvertiserName());
        }
        if (patch.getAccessToken() != null) {
            existing.setAccessToken(patch.getAccessToken());
        }
        if (patch.getRefreshToken() != null) {
            existing.setRefreshToken(patch.getRefreshToken());
        }
        if (patch.getTokenExpiresAt() != null) {
            existing.setTokenExpiresAt(patch.getTokenExpiresAt());
        }
        if (patch.getBalance() != null) {
            existing.setBalance(patch.getBalance());
        }
        if (patch.getStatus() != null) {
            existing.setStatus(patch.getStatus());
        }
        if (patch.getCurrency() != null) {
            existing.setCurrency(patch.getCurrency());
        }
        if (patch.getTimezone() != null) {
            existing.setTimezone(patch.getTimezone());
        }
        accountMapper.updateById(existing);
        log.info("Updated TikTok account: {}", id);
        return getAccountById(id);
    }

    @Transactional
    public TikTokAccount updateTokens(
            String advertiserId, String accessToken, String refreshToken, LocalDateTime expiresAt) {
        TikTokAccount row = new TikTokAccount();
        row.setAdvertiserId(advertiserId);
        row.setAccessToken(accessToken);
        row.setRefreshToken(refreshToken);
        row.setTokenExpiresAt(expiresAt);
        accountMapper.updateTokens(row);
        log.info("Updated tokens for advertiser: {}", advertiserId);
        return getAccountByAdvertiserId(advertiserId);
    }

    @Transactional
    public TikTokAccount updateBalance(String advertiserId, BigDecimal balance) {
        accountMapper.updateBalance(advertiserId, balance);
        log.info("Updated balance for advertiser: {} -> {}", advertiserId, balance);
        return getAccountByAdvertiserId(advertiserId);
    }

    @Transactional
    public void deleteAccount(Long id) {
        TikTokAccount account = getAccountById(id);
        accountMapper.deleteById(id);
        log.info("Deleted TikTok account: {} ({})", id, account.getAdvertiserId());
    }

    @Transactional
    public void deleteAccountByAdvertiserId(String advertiserId) {
        accountMapper.deleteByAdvertiserId(advertiserId);
        log.info("Deleted TikTok account: {}", advertiserId);
    }

    public TikTokAccount checkAndRefreshToken(String advertiserId) {
        return tokenRefreshUtil.checkAndRefreshToken(advertiserId);
    }

    public void refreshAllExpiringTokens() {
        tokenRefreshUtil.refreshAllExpiringTokens();
    }

    public String getValidAccessToken(String advertiserId) {
        TikTokAccount account = checkAndRefreshToken(advertiserId);
        return account.getAccessToken();
    }
}
