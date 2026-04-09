package com.drama.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.drama.config.TikTokConfiguration;
import com.drama.config.TikTokHttpClientConfiguration;
import com.drama.mapper.TikTokSyncLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = {
            RestTemplateAutoConfiguration.class,
            TikTokHttpClientConfiguration.class,
            TikTokConfiguration.class,
            TikTokApiClient.class,
            TikTokApiClientTest.MinimalBeans.class,
        })
@TestPropertySource(
        properties = {
            "tiktok.api.app-id=test-app",
            "tiktok.api.app-secret=test-secret",
        })
class TikTokApiClientTest {

    @Autowired private TikTokApiClient apiClient;

    @Test
    void apiClientBeanPresent() {
        assertNotNull(apiClient);
    }

    @TestConfiguration
    static class MinimalBeans {

        @Bean
        TikTokSyncLogMapper tikTokSyncLogMapper() {
            return mock(TikTokSyncLogMapper.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
