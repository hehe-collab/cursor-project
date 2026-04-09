package com.drama.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties({TikTokConfig.class, TikTokOAuthConfig.class})
@TestPropertySource(
        properties = {
            "tiktok.api.app-id=test-app",
            "tiktok.api.app-secret=test-secret",
            "tiktok.api.version=v1.3",
            "tiktok.oauth.redirect-uri=http://localhost:3001/callback",
        })
class TikTokConfigTest {

    @Autowired private TikTokConfig tiktokConfig;

    @Autowired private TikTokOAuthConfig oauthConfig;

    @Test
    void loadsTikTokApiProperties() {
        assertNotNull(tiktokConfig);
        assertEquals("test-app", tiktokConfig.getAppId());
        assertEquals("test-secret", tiktokConfig.getAppSecret());
        assertNotNull(tiktokConfig.getBaseUrl());
        assertEquals("v1.3", tiktokConfig.getVersion());
        assertTrue(tiktokConfig.getEnabled());
    }

    @Test
    void loadsOAuthProperties() {
        assertNotNull(oauthConfig);
        assertNotNull(oauthConfig.getAuthUrl());
        assertNotNull(oauthConfig.getTokenUrl());
        assertNotNull(oauthConfig.getRedirectUri());
    }

    @Test
    void fullApiUrlStripsLeadingSlash() {
        assertEquals(
                "https://business-api.tiktok.com/open_api/v1.3/report/integrated/get/",
                tiktokConfig.getFullApiUrl("report/integrated/get/"));
        assertEquals(
                "https://business-api.tiktok.com/open_api/v1.3/campaign/get/",
                tiktokConfig.getFullApiUrl("/campaign/get/"));
    }

    @Test
    void authorizationUrlContainsEncodedParams() {
        String url = oauthConfig.getAuthorizationUrl(tiktokConfig.getAppId(), "csrf-1");
        assertTrue(url.contains("app_id=test-app"), url);
        assertTrue(url.contains("state="), url);
        assertTrue(url.contains("redirect_uri="), url);
    }
}
