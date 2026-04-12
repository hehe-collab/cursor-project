package com.drama.controller;

import com.drama.annotation.RequirePermission;
import com.drama.aspect.RateLimitAspect;
import com.drama.common.Result;
import com.google.common.util.concurrent.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;

@Tag(name = "限流管理")
@RestController
@RequestMapping("/api/rate-limit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitAspect rateLimitAspect;

    @Operation(summary = "获取限流统计", description = "查看所有活跃限流器的状态")
    @GetMapping("/stats")
    @RequirePermission(value = "admin:view", description = "查看限流统计")
    public Result<?> getStats() {
        List<Map<String, Object>> limiters = getLimiterStats();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", limiters.size());
        result.put("limiters", limiters);
        return Result.success(result);
    }

    @Operation(summary = "清除所有限流记录", description = "清除所有限流器缓存（谨慎使用）")
    @DeleteMapping("/clear")
    @RequirePermission(value = "admin:role", description = "清除限流记录")
    public Result<?> clearAll() {
        try {
            Field field = rateLimitAspect.getClass().getDeclaredField("limiters");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, RateLimiter> map = (Map<String, RateLimiter>) field.get(rateLimitAspect);
            int count = map.size();
            map.clear();
            return Result.success("已清除 " + count + " 个限流器");
        } catch (Exception e) {
            return Result.error("清除失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> getLimiterStats() {
        try {
            Field field = rateLimitAspect.getClass().getDeclaredField("limiters");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, RateLimiter> map = (Map<String, RateLimiter>) field.get(rateLimitAspect);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map.Entry<String, RateLimiter> entry : map.entrySet()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("key", entry.getKey());
                item.put("rate", entry.getValue().getRate());
                String type = entry.getKey().contains(":ip:") ? "IP"
                        : entry.getKey().contains(":user:") ? "USER" : "GLOBAL";
                item.put("type", type);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
