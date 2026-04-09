package com.drama.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TikTokHttpClientConfiguration {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, TikTokConfig tikTokConfig) {
        int ms =
                tikTokConfig.getTimeout() != null && tikTokConfig.getTimeout() > 0
                        ? tikTokConfig.getTimeout()
                        : 30_000;
        return builder
                .setConnectTimeout(Duration.ofMillis(ms))
                .setReadTimeout(Duration.ofMillis(ms))
                .build();
    }
}
