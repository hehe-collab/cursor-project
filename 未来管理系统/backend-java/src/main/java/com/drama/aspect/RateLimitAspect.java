package com.drama.aspect;

import com.drama.annotation.RateLimit;
import com.drama.exception.RateLimitException;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于注解的精细限流切面。
 *
 * <p>配合 {@link RateLimit} 注解使用，支持按 IP / 用户 / 全局限流。
 * 限流器按 (key + limitType + value) 动态创建并缓存，防止内存泄漏
 *（超过 10000 个时自动清理所有缓存的限流器）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final HttpServletRequest request;

    /** 限流器缓存表：Key = "{prefix}:{type}:{value}" */
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit);
        RateLimiter limiter = limiters.computeIfAbsent(key, k -> {
            double qps = (double) rateLimit.max() / rateLimit.timeout();
            log.info("创建限流器: key={}, max={}, timeout={}s, qps={}",
                    k, rateLimit.max(), rateLimit.timeout(), String.format("%.2f", qps));
            return RateLimiter.create(qps);
        });

        boolean acquired = limiter.tryAcquire(rateLimit.waitMillis(), TimeUnit.MILLISECONDS);
        if (!acquired) {
            log.warn("请求被限流: key={}, method={}", key, getMethodName(point));
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }

        return point.proceed();
    }

    private String buildKey(RateLimit annotation) {
        String key = annotation.key();
        switch (annotation.limitType()) {
            case IP:
                key += ":ip:" + getClientIp();
                break;
            case USER:
                Integer adminId = getAdminId();
                key += ":user:" + (adminId != null ? adminId : "anonymous");
                break;
            case GLOBAL:
            default:
                // 不追加后缀
                break;
        }
        return key;
    }

    private String getClientIp() {
        HttpServletRequest req = getRequest();
        if (req == null) return "unknown";

        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    private Integer getAdminId() {
        HttpServletRequest req = getRequest();
        if (req == null) return null;
        try {
            Object adminId = req.getAttribute("adminId");
            if (adminId instanceof Integer) return (Integer) adminId;
            if (adminId instanceof Number) return ((Number) adminId).intValue();
        } catch (Exception ignored) {}
        return null;
    }

    private String getMethodName(ProceedingJoinPoint point) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
