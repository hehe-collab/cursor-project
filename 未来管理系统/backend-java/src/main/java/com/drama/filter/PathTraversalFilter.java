package com.drama.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
public class PathTraversalFilter implements Filter {

    private static final Pattern PATH_TRAVERSAL = Pattern.compile(
        "(\\.\\.[/\\\\])|(%2e%2e[/\\\\])|(%252e%252e)|(\\.%2e)", Pattern.CASE_INSENSITIVE
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String rawUri = req.getRequestURI();

        if (PATH_TRAVERSAL.matcher(rawUri).find()) {
            log.warn("路径遍历攻击被拦截: {}", rawUri);
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":1,\"message\":\"请求路径不合法\",\"data\":null}");
            return;
        }

        chain.doFilter(request, response);
    }
}