package com.drama.controller;

import com.alibaba.druid.pool.DruidDataSource;
import com.drama.common.Result;
import com.github.benmanes.caffeine.cache.Cache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.util.*;

/**
 * 性能监控 Controller
 * 提供系统性能指标、缓存命中率、数据库连接池状态等监控接口
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Tag(name = "性能监控", description = "系统性能指标监控")
public class MonitorController {

    private final CacheManager cacheManager;
    private final DataSource dataSource;

    @Operation(summary = "获取完整性能指标", description = "返回 JVM 内存、线程、缓存命中率、数据库连接池状态")
    @GetMapping("/metrics")
    public Result<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // JVM 内存
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("heapUsed", formatBytes(heapUsage.getUsed()));
        memory.put("heapMax", formatBytes(heapUsage.getMax()));
        memory.put("heapUsedPercent", String.format("%.1f%%", (double) heapUsage.getUsed() / heapUsage.getMax() * 100));
        memory.put("nonHeapUsed", formatBytes(nonHeapUsage.getUsed()));
        memory.put("nonHeapMax", formatBytes(nonHeapUsage.getMax()));
        memory.put("nonHeapUsedPercent", String.format("%.1f%%", (double) nonHeapUsage.getUsed() / nonHeapUsage.getMax() * 100));
        metrics.put("memory", memory);

        // 线程
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threads = new LinkedHashMap<>();
        threads.put("count", threadMXBean.getThreadCount());
        threads.put("peakCount", threadMXBean.getPeakThreadCount());
        threads.put("daemonCount", threadMXBean.getDaemonThreadCount());
        threads.put("maxCount", threadMXBean.getPeakThreadCount());
        metrics.put("threads", threads);

        // 缓存统计
        metrics.put("cacheStats", collectCacheStats());

        // 数据库连接池
        metrics.put("dataSource", collectDataSourceStats());

        // 系统信息
        Map<String, Object> system = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        system.put("availableProcessors", runtime.availableProcessors());
        system.put("freeMemory", formatBytes(runtime.freeMemory()));
        system.put("totalMemory", formatBytes(runtime.totalMemory()));
        system.put("maxMemory", formatBytes(runtime.maxMemory()));
        system.put("uptime", formatUptime());
        metrics.put("system", system);

        return Result.success(metrics);
    }

    @Operation(summary = "获取缓存统计", description = "查看所有缓存的命中率、大小等信息")
    @GetMapping("/cache/stats")
    public Result<List<Map<String, Object>>> getCacheStats() {
        return Result.success(collectCacheStats());
    }

    @Operation(summary = "清除指定缓存", description = "清除指定名称的缓存")
    @DeleteMapping("/cache/{cacheName}")
    public Result<String> clearCache(@PathVariable String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return Result.success("缓存已清除: " + cacheName);
        }
        return Result.error("缓存不存在: " + cacheName);
    }

    @Operation(summary = "清除所有缓存", description = "清除系统中的所有缓存")
    @DeleteMapping("/cache/all")
    public Result<String> clearAllCaches() {
        int count = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                count++;
            }
        }
        return Result.success("已清除 " + count + " 个缓存");
    }

    @Operation(summary = "获取数据库连接池状态", description = "查看 Druid 连接池的详细状态")
    @GetMapping("/datasource")
    public Result<Map<String, Object>> getDataSourceStats() {
        return Result.success(collectDataSourceStats());
    }

    @Operation(summary = "健康检查", description = "检查数据库连接和应用状态")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        // 数据库连接检查
        try (Connection conn = dataSource.getConnection()) {
            health.put("database", "CONNECTED");
            health.put("databaseUrl", conn.getMetaData().getURL());
        } catch (Exception e) {
            health.put("database", "DISCONNECTED");
            health.put("databaseError", e.getMessage());
        }

        return Result.success(health);
    }

    // ========== 私有方法 ==========

    private List<Map<String, Object>> collectCacheStats() {
        List<Map<String, Object>> stats = new ArrayList<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                com.github.benmanes.caffeine.cache.stats.CacheStats cacheStats = nativeCache.stats();

                Map<String, Object> stat = new LinkedHashMap<>();
                stat.put("name", cacheName);
                stat.put("size", nativeCache.estimatedSize());
                stat.put("hitCount", cacheStats.hitCount());
                stat.put("missCount", cacheStats.missCount());
                stat.put("hitRate", String.format("%.2f%%", cacheStats.hitRate() * 100));
                stat.put("evictionCount", cacheStats.evictionCount());
                stat.put("loadSuccessCount", cacheStats.loadSuccessCount());
                stat.put("loadFailureCount", cacheStats.loadFailureCount());
                stat.put("averageLoadPenalty", String.format("%.2fms", cacheStats.averageLoadPenalty() / 1_000_000));

                stats.add(stat);
            }
        }

        return stats;
    }

    private Map<String, Object> collectDataSourceStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        if (dataSource instanceof DruidDataSource druidDS) {
            stats.put("poolName", druidDS.getName());
            stats.put("activeCount", druidDS.getActiveCount());
            stats.put("poolingCount", druidDS.getPoolingCount());
            stats.put("poolingMax", druidDS.getMaxActive());
            stats.put("waitThreadCount", druidDS.getWaitThreadCount());
            stats.put("initialSize", druidDS.getInitialSize());
            stats.put("minIdle", druidDS.getMinIdle());
            stats.put("maxActive", druidDS.getMaxActive());
            stats.put("loginTimeout", druidDS.getLoginTimeout() + "s");
            stats.put("validationQuery", druidDS.getValidationQuery());
            stats.put("testOnBorrow", druidDS.isTestOnBorrow());
            stats.put("testWhileIdle", druidDS.isTestWhileIdle());
            stats.put("removeAbandoned", druidDS.isRemoveAbandoned());
            stats.put("removeAbandonedTimeout", druidDS.getRemoveAbandonedTimeout() + "s");

            // 连接获取统计
            long connectCount = druidDS.getConnectCount();
            long errorCount = druidDS.getErrorCount();
            long executeCount = druidDS.getExecuteCount();
            long executeQueryCount = druidDS.getExecuteQueryCount();

            Map<String, Object> counters = new LinkedHashMap<>();
            counters.put("connectCount", connectCount);
            counters.put("errorCount", errorCount);
            counters.put("executeCount", executeCount);
            counters.put("executeQueryCount", executeQueryCount);
            if (connectCount > 0) {
                counters.put("errorRate", String.format("%.2f%%", (double) errorCount / connectCount * 100));
            }
            stats.put("counters", counters);
        } else {
            stats.put("type", dataSource.getClass().getSimpleName());
            stats.put("message", "非 Druid 连接池，部分指标不可用");
        }

        return stats;
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "N/A";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        int unit = 0;
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format("%.1f %s", value, units[unit]);
    }

    private String formatUptime() {
        long ms = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%d天 %d小时 %d分钟", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟 %d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }
}
