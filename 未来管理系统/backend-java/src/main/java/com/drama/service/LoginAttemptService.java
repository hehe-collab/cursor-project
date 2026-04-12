package com.drama.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 30;

    private final LoadingCache<String, Integer> usernameAttempts;
    private final LoadingCache<String, Integer> ipAttempts;

    public LoginAttemptService() {
        this.usernameAttempts = CacheBuilder.newBuilder()
                .expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });

        this.ipAttempts = CacheBuilder.newBuilder()
                .expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    public void loginFailed(String username, String ip) {
        try {
            int userAttempts = usernameAttempts.get(username);
            usernameAttempts.put(username, userAttempts + 1);

            int ipAttemptsCount = ipAttempts.get(ip);
            ipAttempts.put(ip, ipAttemptsCount + 1);

            int remaining = MAX_ATTEMPTS - (userAttempts + 1);
            log.warn("[LOGIN_LIMIT] 登录失败: username={}, ip={}, 剩余尝试={}", username, ip, remaining);

            if (userAttempts + 1 >= MAX_ATTEMPTS) {
                log.error("[LOGIN_LIMIT] 账户已锁定: username={}, 持续{}分钟", username, BLOCK_DURATION_MINUTES);
            }
        } catch (ExecutionException e) {
            log.error("[LOGIN_LIMIT] 记录登录失败异常", e);
        }
    }

    public void loginSucceeded(String username, String ip) {
        usernameAttempts.invalidate(username);
        ipAttempts.invalidate(ip);
        log.info("[LOGIN_LIMIT] 登录成功，清除失败记录: username={}, ip={}", username, ip);
    }

    public boolean isUsernameBlocked(String username) {
        try {
            return usernameAttempts.get(username) >= MAX_ATTEMPTS;
        } catch (ExecutionException e) {
            return false;
        }
    }

    public boolean isIpBlocked(String ip) {
        try {
            return ipAttempts.get(ip) >= MAX_ATTEMPTS;
        } catch (ExecutionException e) {
            return false;
        }
    }

    public int getRemainingAttempts(String username) {
        try {
            int attempts = usernameAttempts.get(username);
            return Math.max(0, MAX_ATTEMPTS - attempts);
        } catch (ExecutionException e) {
            return MAX_ATTEMPTS;
        }
    }

    public void unlock(String username) {
        usernameAttempts.invalidate(username);
        log.info("[LOGIN_LIMIT] 管理员解锁账户: username={}", username);
    }
}
