package com.drama.interceptor;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter globalRateLimiter = RateLimiter.create(100.0);
    private final ConcurrentHashMap<String, RateLimiter> ipRateLimiters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!globalRateLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            log.warn("全局限流触发");
            sendRateLimitResponse(response, "系统繁忙，请稍后再试");
            return false;
        }
        String clientIp = getClientIp(request);
        RateLimiter ipLimiter =
                ipRateLimiters.computeIfAbsent(clientIp, k -> RateLimiter.create(30.0));
        if (!ipLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            log.warn("IP 限流触发: {}", clientIp);
            sendRateLimitResponse(response, "请求过于频繁，请稍后再试");
            return false;
        }
        if (ipRateLimiters.size() > 10000) {
            ipRateLimiters.clear();
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "";
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter()
                .write(String.format("{\"code\":429,\"message\":\"%s\",\"data\":null}", message));
    }
}
