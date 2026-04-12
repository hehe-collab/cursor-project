package com.drama.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j
@Component
public class XssInterceptor implements HandlerInterceptor {

    private static final Pattern[] XSS_PATTERNS = {
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onerror\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onload\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onclick\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onmouseover\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("data:text/html", Pattern.CASE_INSENSITIVE)
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String ip = getClientIp(request);

        // 检查 JSON body
        if (request instanceof CachedBodyHttpServletRequest) {
            CachedBodyHttpServletRequest cached = (CachedBodyHttpServletRequest) request;
            String body = cached.getCachedBodyString();
            if (!body.isEmpty() && containsXss(body)) {
                log.warn("[XSS] 检测到攻击: ip={}", ip);
                sendError(response, "请求包含非法字符");
                return false;
            }
        }

        // 检查 URL 参数
        if (!request.getParameterMap().isEmpty()) {
            for (String key : request.getParameterMap().keySet()) {
                for (String value : request.getParameterMap().get(key)) {
                    if (containsXss(value)) {
                        log.warn("[XSS] 检测到攻击: ip={}, key={}", ip, key);
                        sendError(response, "请求包含非法字符");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean containsXss(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    private void sendError(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":400,\"message\":\"" + message + "\",\"data\":null}");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "";
    }
}
