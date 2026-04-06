package com.drama.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HS256 密钥，与 Node 默认一致，生产请用环境变量 JWT_SECRET */
    private String secret = "future_admin_secret";

    private int expireDays = 7;
}
