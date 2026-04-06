package com.drama.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 不依赖 IAcsClient Bean（VOD 客户端在 VodService 内按需创建），仅根据是否配置 Key 判断。
 */
@Component
public class VodHealthIndicator implements HealthIndicator {

    @Value("${aliyun.vod.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.vod.access-key-secret:}")
    private String accessKeySecret;

    @Override
    public Health health() {
        if (accessKeyId == null
                || accessKeyId.isBlank()
                || accessKeySecret == null
                || accessKeySecret.isBlank()) {
            return Health.status("UNKNOWN")
                    .withDetail("vod", "aliyun-vod")
                    .withDetail("status", "未配置 AccessKey")
                    .build();
        }
        return Health.up().withDetail("vod", "aliyun-vod").withDetail("status", "已配置").build();
    }
}
