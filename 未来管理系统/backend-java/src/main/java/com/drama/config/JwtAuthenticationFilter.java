package com.drama.config;

import com.alibaba.fastjson2.JSON;
import com.drama.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.equals("/api/health")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/logout")
                || path.equals("/api/recharge-groups/frontend")
                || path.startsWith("/api/h5/")
                || path.startsWith("/error")) {
            filterChain.doFilter(request, response);
            return;
        }
        // 素材封面通过 <img> 加载，无法带 Authorization
        if ("GET".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/uploads/")) {
            filterChain.doFilter(request, response);
            return;
        }
        // TikTok OAuth 浏览器回跳无法携带 Bearer
        if ("GET".equalsIgnoreCase(request.getMethod()) && "/api/tiktok/oauth/callback".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return;
        }
        String token = auth.substring(7).trim();
        try {
            var claims = jwtUtil.parse(token);
            Object uid = claims.get("userId");
            if (uid != null) {
                request.setAttribute("adminId", ((Number) uid).intValue());
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            writeUnauthorized(response);
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.toJSONString(Result.error(401, "未授权")));
    }
}
