package com.drama.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解，支持按 IP / 用户 / 全局限流。
 *
 * <p>使用 Guava RateLimiter 实现，限流器按注解参数动态创建并缓存。
 * 可与全局拦截器 {@link com.drama.interceptor.RateLimitInterceptor} 共存：
 * 拦截器负责全局兜底，本注解负责单个接口的精细控制。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流 Key 前缀，最终 Key 格式：{prefix}:{type}:{value}
     */
    String key() default "method";

    /**
     * 时间窗口内最大请求数。
     * 与 {@link #timeout()} 共同决定 RateLimiter 的 QPS = max / timeout。
     */
    int max() default 10;

    /**
     * 时间窗口（秒）。
     */
    int timeout() default 60;

    /**
     * 限流类型。
     */
    LimitType limitType() default LimitType.IP;

    /**
     * 获取令牌超时时间（毫秒），默认 500ms。
     * 时间内无法获得令牌则拒绝请求。
     */
    long waitMillis() default 500;

    enum LimitType {
        /** 按请求来源 IP 限流。 */
        IP,
        /** 按登录用户 ID 限流。 */
        USER,
        /** 全局限流，所有请求共享同一令牌桶。 */
        GLOBAL
    }
}
