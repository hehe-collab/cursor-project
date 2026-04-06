package com.drama.controller;

import com.drama.common.Result;
import com.drama.dto.LoginRequest;
import com.drama.service.AuthService;
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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** 需 Bearer Token；由 {@link com.drama.config.JwtAuthenticationFilter} 写入 adminId */
    @GetMapping("/me")
    public Result<Map<String, Object>> me(
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        return authService.currentUser(adminId);
    }

    /** JWT 无状态，前端删 token 即可；此处仅占位成功 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success(null);
    }
}
