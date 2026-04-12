package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokAccount;
import com.drama.service.TikTokAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** TikTok 账户管理（需 JWT，路径 {@code /api/tiktok/accounts}） */
@Tag(name = "TikTok账户", description = "TikTok OAuth 授权账户管理")
@Slf4j
@RestController
@RequestMapping("/api/tiktok/accounts")
@RequiredArgsConstructor
public class TikTokAccountController {

    private final TikTokAccountService accountService;

    @Operation(summary = "获取TikTok账户列表", description = "获取所有TikTok授权账户，可按状态筛选")
    @GetMapping
    public Result<List<TikTokAccount>> getAccounts(@Parameter(description = "账户状态") @RequestParam(required = false) String status) {
        try {
            List<TikTokAccount> accounts =
                    (status != null && !status.isEmpty())
                            ? accountService.getAccountsByStatus(status)
                            : accountService.getAccounts();
            return Result.success(accounts);
        } catch (Exception e) {
            log.error("Failed to get accounts: {}", e.getMessage(), e);
            return Result.error("获取账户列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取TikTok账户详情", description = "根据ID获取TikTok账户详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<TikTokAccount> getAccountById(@PathVariable Long id) {
        try {
            return Result.success(accountService.getAccountById(id));
        } catch (Exception e) {
            log.error("Failed to get account {}: {}", id, e.getMessage(), e);
            return Result.error("获取账户失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据广告主ID获取账户", description = "根据广告主ID获取TikTok账户")
    @GetMapping("/advertiser/{advertiserId}")
    public Result<TikTokAccount> getAccountByAdvertiserId(@PathVariable String advertiserId) {
        try {
            return Result.success(accountService.getAccountByAdvertiserId(advertiserId));
        } catch (Exception e) {
            log.error("Failed to get account by advertiser_id {}: {}", advertiserId, e.getMessage(), e);
            return Result.error("获取账户失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建或更新TikTok账户", description = "创建或更新TikTok授权账户")
    @PostMapping
    public Result<TikTokAccount> upsertAccount(@RequestBody TikTokAccount account) {
        try {
            return Result.success(accountService.upsertAccount(account));
        } catch (Exception e) {
            log.error("Failed to upsert account: {}", e.getMessage(), e);
            return Result.error("创建/更新账户失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新TikTok账户", description = "更新指定TikTok账户的信息")
    @PutMapping("/{id:\\d+}")
    public Result<TikTokAccount> updateAccount(
            @PathVariable Long id, @RequestBody TikTokAccount account) {
        try {
            return Result.success(accountService.updateAccount(id, account));
        } catch (Exception e) {
            log.error("Failed to update account {}: {}", id, e.getMessage(), e);
            return Result.error("更新账户失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除TikTok账户", description = "删除指定的TikTok账户")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to delete account {}: {}", id, e.getMessage(), e);
            return Result.error("删除账户失败: " + e.getMessage());
        }
    }

    @Operation(summary = "刷新Token", description = "刷新指定TikTok账户的访问Token")
    @PostMapping("/{id:\\d+}/refresh-token")
    public Result<TikTokAccount> refreshToken(@PathVariable Long id) {
        try {
            TikTokAccount account = accountService.getAccountById(id);
            return Result.success(accountService.checkAndRefreshToken(account.getAdvertiserId()));
        } catch (Exception e) {
            log.error("Failed to refresh token for account {}: {}", id, e.getMessage(), e);
            return Result.error("刷新 Token 失败: " + e.getMessage());
        }
    }

    @Operation(summary = "刷新所有Token", description = "刷新所有即将过期的TikTok账户Token")
    @PostMapping("/refresh-all-tokens")
    public Result<Void> refreshAllTokens() {
        try {
            accountService.refreshAllExpiringTokens();
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to refresh all tokens: {}", e.getMessage(), e);
            return Result.error("刷新所有 Token 失败: " + e.getMessage());
        }
    }
}
