package com.drama.service;

import com.drama.exception.BusinessException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Pixel ID + Token 探测：Meta / Facebook 走 Graph API；其它平台仅做格式校验（避免误报为「已验证」时可看 probe 字段）。
 */
@Service
public class PixelVerificationService {

    @Value("${drama.pixel.meta-graph-version:v21.0}")
    private String metaGraphVersion;

    @Value("${drama.pixel.http-timeout-ms:10000}")
    private int httpTimeoutMs;

    public Map<String, Object> verify(String pixelId, String token, String mediaPlatform) {
        if (pixelId == null || pixelId.isBlank() || token == null || token.isBlank()) {
            throw new BusinessException(400, "Pixel ID 或 Token 无效");
        }
        String plat = mediaPlatform == null ? "" : mediaPlatform.trim().toLowerCase();
        if (plat.contains("tiktok")) {
            return verifyTikTokFormat(pixelId.trim(), token.trim());
        }
        if (plat.contains("google")) {
            return verifyGoogleFormat(pixelId.trim(), token.trim());
        }
        return verifyMetaPixel(pixelId.trim(), token.trim());
    }

    private Map<String, Object> verifyMetaPixel(String pixelId, String token) {
        String enc = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String url = String.format("https://graph.facebook.com/%s/%s?fields=id&access_token=%s", metaGraphVersion, pixelId, enc);
        HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(httpTimeoutMs)).build();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(httpTimeoutMs + 2000L))
                    .GET()
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = res.body() != null ? res.body() : "";
            if (res.statusCode() >= 200 && res.statusCode() < 300 && body.contains("\"id\"")) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("valid", true);
                d.put("probe", "meta-graph-api");
                return d;
            }
        } catch (Exception e) {
            throw new BusinessException(400, "连接 Meta 校验接口失败：" + e.getMessage());
        }
        throw new BusinessException(400, "Meta Pixel 校验未通过，请核对 Pixel ID 与访问令牌权限");
    }

    private Map<String, Object> verifyTikTokFormat(String pixelId, String token) {
        if (!pixelId.matches("\\d{6,24}") || token.length() < 8) {
            throw new BusinessException(400, "TikTok Pixel ID / Token 格式异常");
        }
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("valid", true);
        d.put("probe", "tiktok-format-check");
        return d;
    }

    private Map<String, Object> verifyGoogleFormat(String pixelId, String token) {
        if (pixelId.length() < 6 || token.length() < 8) {
            throw new BusinessException(400, "Google 转化 ID / Token 格式异常");
        }
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("valid", true);
        d.put("probe", "google-format-check");
        return d;
    }
}
