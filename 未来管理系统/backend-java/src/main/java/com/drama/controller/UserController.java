package com.drama.controller;

import com.drama.annotation.RateLimit;
import com.drama.common.Result;
import com.drama.dto.UserQueryParam;
import com.drama.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "C端用户列表、统计、CRUD 操作")
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取用户统计", description = "获取用户相关的统计数据，支持按条件筛选")
    @GetMapping("/stats")
    @RateLimit(key = "user:stats", max = 30, timeout = 60, limitType = RateLimit.LimitType.USER)
    public Result<Map<String, Object>> stats(
            @Parameter(description = "8位用户ID") @RequestParam(required = false) String user_id,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "Token") @RequestParam(required = false) String token,
            @Parameter(description = "推广ID") @RequestParam(required = false) String promotion_id,
            @Parameter(description = "国家代码") @RequestParam(required = false) String country,
            @Parameter(description = "注册开始日期，格式 yyyy-MM-dd") @RequestParam(required = false) String start_date,
            @Parameter(description = "注册结束日期，格式 yyyy-MM-dd") @RequestParam(required = false) String end_date) {
        UserQueryParam q = new UserQueryParam();
        q.setUserId(user_id);
        q.setUsername(username);
        q.setToken(token);
        q.setPromotionId(promotion_id);
        q.setCountry(country);
        q.setStartDate(start_date);
        q.setEndDate(end_date);
        return Result.success(userService.stats(q));
    }

    @Operation(summary = "获取用户列表", description = "分页查询用户列表，支持多条件筛选")
    @GetMapping
    public Result<Map<String, Object>> list(
            @Parameter(description = "8位用户ID") @RequestParam(required = false) String user_id,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "Token") @RequestParam(required = false) String token,
            @Parameter(description = "推广ID") @RequestParam(required = false) String promotion_id,
            @Parameter(description = "国家代码") @RequestParam(required = false) String country,
            @Parameter(description = "注册开始日期，格式 yyyy-MM-dd") @RequestParam(required = false) String start_date,
            @Parameter(description = "注册结束日期，格式 yyyy-MM-dd") @RequestParam(required = false) String end_date,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        UserQueryParam q = new UserQueryParam();
        q.setUserId(user_id);
        q.setUsername(username);
        q.setToken(token);
        q.setPromotionId(promotion_id);
        q.setCountry(country);
        q.setStartDate(start_date);
        q.setEndDate(end_date);
        q.setPage(page);
        q.setPageSize(pageSize);
        return Result.success(userService.list(q));
    }

    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户的详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable int id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.success("创建成功", userService.create(body));
    }

    @Operation(summary = "更新用户信息", description = "更新指定用户的信息")
    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        userService.update(id, body);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除用户", description = "删除指定用户（软删除）")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        userService.delete(id);
        return Result.success("删除成功", null);
    }
}
