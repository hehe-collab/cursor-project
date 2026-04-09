package com.drama.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/** HMAC-SHA256 / MD5 等签名辅助（若对接方要求签名字符串）。 */
@Slf4j
public final class TikTokSignatureUtil {

    private TikTokSignatureUtil() {}

    public static String generateSignature(String appSecret, Map<String, Object> params) {
        try {
            TreeMap<String, Object> sorted = new TreeMap<>(params);
            StringBuilder sb = new StringBuilder();
            sorted.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            String paramStr = sb.toString();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key =
                    new SecretKeySpec(
                            appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] raw = mac.doFinal(paramStr.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            log.error("HMAC-SHA256 签名失败: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate signature", e);
        }
    }

    public static String generateMd5Signature(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("MD5 失败: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate MD5 signature", e);
        }
    }

    public static boolean verifySignature(String appSecret, Map<String, Object> params, String signature) {
        return generateSignature(appSecret, params).equals(signature);
    }
}
