package com.drama.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    TikTokIntegrationProperties.class,
    TikTokConfig.class,
    TikTokOAuthConfig.class
})
public class TikTokConfiguration {}
