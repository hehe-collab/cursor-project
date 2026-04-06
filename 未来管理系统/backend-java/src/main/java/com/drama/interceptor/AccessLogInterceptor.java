package com.drama.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");
    private static final String START = "accessStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object st = request.getAttribute(START);
        if (!(st instanceof Long)) {
            return;
        }
        long startTime = (Long) st;
        long duration = System.currentTimeMillis() - startTime;
        String msg = String.format(
                "%s %s %d %dms %s",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration,
                request.getRemoteAddr());
        ACCESS_LOG.info(msg);
        if (duration > 1000) {
            log.warn("慢请求: {} 耗时 {}ms", request.getRequestURI(), duration);
        }
    }
}
