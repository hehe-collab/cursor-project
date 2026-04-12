package com.drama.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 配置
 * 访问地址：http://localhost:3001/swagger-ui.html
 * API 文档：http://localhost:3001/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("未来管理系统 API 文档")
                        .version("1.0.0")
                        .description("""
                                短剧管理系统后端接口文档

                                ## 功能模块
                                - 用户管理：用户列表、统计、CRUD
                                - 充值管理：充值记录、方案、方案组
                                - 短剧管理：短剧、剧集、分类、标签
                                - 投放管理：推广链接、广告账户、回传配置
                                - TikTok 管理：账户、广告系列、广告组、广告、Pixel、报表、回传、任务
                                - 系统管理：管理员、角色、权限、操作日志、站点设置

                                ## 认证说明
                                除登录接口外，所有接口均需要在请求头添加 JWT Token：
                                ```
                                Authorization: Bearer <your_token>
                                ```

                                ## 获取 Token
                                1. 调用 POST /api/auth/login 接口登录
                                2. 从响应中获取 token 字段
                                3. 点击右上角 "Authorize" 按钮，输入 token
                                """)
                        .contact(new Contact()
                                .name("技术支持")
                                .email("support@example.com")
                                .url("https://example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:3001").description("本地开发环境"),
                        new Server().url("https://api.example.com").description("生产环境")
                ))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请输入 JWT Token（不需要 'Bearer ' 前缀）")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"));
    }
}
