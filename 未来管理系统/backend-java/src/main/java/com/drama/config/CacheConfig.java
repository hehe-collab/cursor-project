package com.drama.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 本地缓存（Caffeine）配置。
 * 支持多级缓存策略：
 * - 默认缓存（5分钟过期）：dramas, users, categories, tags 等
 * - 统计缓存（1分钟过期）：stats, user-stats, drama-stats 等
 * - 长时缓存（1小时过期）：recharge-plans, promotion-links 等
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 缓存名称 */
    public static final String CACHE_DRAMAS = "dramas";
    public static final String CACHE_DRAMAS_LIST = "drama-list";
    public static final String CACHE_USERS = "users";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_TAGS = "tags";
    public static final String CACHE_STATS = "stats";
    public static final String CACHE_USER_STATS = "user-stats";
    public static final String CACHE_DRAMA_STATS = "drama-stats";
    public static final String CACHE_DASHBOARD_STATS = "dashboard-stats";
    public static final String CACHE_RECHARGE_PLANS = "rechargePlans";
    public static final String CACHE_PROMOTION_LINKS = "promotionLinks";
    public static final String CACHE_LONG_TERM = "long-term";

    /**
     * 默认缓存管理器（5分钟过期）
     * 适用于：短剧、用户、分类、标签等一般数据
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(
                CACHE_DRAMAS,
                CACHE_DRAMAS_LIST,
                CACHE_USERS,
                CACHE_CATEGORIES,
                CACHE_TAGS,
                CACHE_STATS,
                CACHE_USER_STATS,
                CACHE_DRAMA_STATS,
                CACHE_DASHBOARD_STATS
        );
        mgr.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .expireAfterAccess(3, TimeUnit.MINUTES)
                        .recordStats());
        return mgr;
    }

    /**
     * 长时缓存管理器（1小时过期）
     * 适用于：充值方案、推广链接等相对稳定的数据
     */
    @Bean("longTermCacheManager")
    public CacheManager longTermCacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(
                CACHE_RECHARGE_PLANS,
                CACHE_PROMOTION_LINKS,
                CACHE_LONG_TERM
        );
        mgr.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .recordStats());
        return mgr;
    }

    /**
     * 统计缓存管理器（1分钟过期）
     * 适用于：各类统计数据，需要较高实时性但又不想频繁查询
     */
    @Bean("statsCacheManager")
    public CacheManager statsCacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(
                "stats",
                "user-stats",
                "drama-stats",
                "dashboard-stats"
        );
        mgr.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(1, TimeUnit.MINUTES)
                        .recordStats());
        return mgr;
    }
}
