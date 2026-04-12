package com.drama.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        // 业务鉴权由 JwtAuthenticationFilter + Result 401 处理；此处保持 permitAll，避免未注入 SecurityContext 的 JWT 方案与 authorizeRequest 冲突。
        // /actuator/** 仅允许本机（127.0.0.1 / ::1）访问，防止 Prometheus 指标对外暴露。
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**")
                            .access((authentication, context) -> {
                                String remoteAddr = context.getRequest().getRemoteAddr();
                                boolean isLocal = "127.0.0.1".equals(remoteAddr)
                                        || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                                        || "::1".equals(remoteAddr)
                                        || new IpAddressMatcher("127.0.0.0/8").matches(remoteAddr);
                                return new org.springframework.security.authorization.AuthorizationDecision(isLocal);
                            })
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
