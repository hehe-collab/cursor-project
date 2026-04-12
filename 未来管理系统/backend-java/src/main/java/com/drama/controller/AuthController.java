package com.drama.controller;

import com.drama.annotation.RateLimit;
import com.drama.common.Result;
import com.drama.dto.LoginRequest;
import com.drama.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、登出、获取当前用户信息")
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "管理员登录",
        description = "使用用户名和密码登录系统，成功后返回 JWT Token"
    )
    @ApiResponse(
        responseCode = "200",
        description = "登录成功",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = "{\"code\":0,\"message\":\"登录成功\",\"data\":{\"token\":\"eyJ...\",\"admin\":{\"id\":1,\"username\":\"admin\"}}}"
            )
        )
    )
    @SecurityRequirements
    @PostMapping("/login")
    @RateLimit(key = "auth:login", max = 5, timeout = 60, limitType = RateLimit.LimitType.IP)
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        return authService.login(req, ip);
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
        return ip;
    }

    @Operation(summary = "获取当前登录用户信息", description = "根据 Token 获取当前登录的管理员信息")
    @ApiResponse(responseCode = "200", description = "请求成功")
    @GetMapping("/me")
    public Result<Map<String, Object>> me(
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        return authService.currentUser(adminId);
    }

    @Operation(summary = "��理员登出", description = "退出登录，前端删除本地存储的 Token 即可")
    @ApiResponse(responseCode = "200", description = "登出成功")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success(null);
    }
}
