package com.drama.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<PathTraversalFilter> pathTraversalFilterRegistration() {
        FilterRegistrationBean<PathTraversalFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new PathTraversalFilter());
        // 对所有请求进行检查（过滤器内部通过正则精确判断 .. 序列）
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE - 100);
        return reg;
    }
}
