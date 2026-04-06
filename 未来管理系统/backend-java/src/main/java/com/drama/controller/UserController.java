package com.drama.controller;

import com.drama.common.Result;
import com.drama.dto.UserQueryParam;
import com.drama.service.UserService;
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
public class UserController {

    private final UserService userService;

    /** 须在 `/{id}` 之前 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(
            @RequestParam(required = false) String user_id,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String promotion_id,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date) {
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

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String user_id,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String promotion_id,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
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

    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable int id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.success("创建成功", userService.create(body));
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        userService.update(id, body);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        userService.delete(id);
        return Result.success("删除成功", null);
    }
}
