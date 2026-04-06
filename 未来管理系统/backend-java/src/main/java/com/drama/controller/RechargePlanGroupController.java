package com.drama.controller;

import com.drama.common.Result;
import com.drama.dto.BatchIdsRequest;
import com.drama.dto.RechargePlanGroupSaveRequest;
import com.drama.service.RechargePlanGroupService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recharge-groups")
@RequiredArgsConstructor
public class RechargePlanGroupController {

    private final RechargePlanGroupService rechargePlanGroupService;

    /** 用户端/H5：无需登录（Jwt 过滤器放行） */
    @GetMapping("/frontend")
    public Result<List<Map<String, Object>>> frontend() {
        return Result.success(rechargePlanGroupService.frontendGroups());
    }

    @PostMapping("/test-pixel")
    public Result<Map<String, Object>> testPixel(@RequestBody Map<String, Object> body) {
        return Result.success("Pixel 验证成功", rechargePlanGroupService.testPixel(body));
    }

    @PostMapping("/batch-delete")
    public Result<Map<String, Object>> batchDelete(@RequestBody BatchIdsRequest body) {
        List<Integer> ids = body != null ? body.getIds() : null;
        if (ids == null || ids.isEmpty()) {
            return Result.error(400, "请选择要删除的充值方案组");
        }
        int n = rechargePlanGroupService.deleteBatch(ids);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deleted", n);
        return Result.success("已删除 " + n + " 个充值方案组", data);
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String group_id,
            @RequestParam(required = false) String group_name) {
        return Result.success(
                "获取成功",
                rechargePlanGroupService.list(page, pageSize, group_id, group_name));
    }

    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable int id) {
        return Result.success("获取成功", rechargePlanGroupService.detail(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(
            @RequestBody RechargePlanGroupSaveRequest body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        int id = rechargePlanGroupService.create(body, adminId);
        return Result.success("新增成功", new LinkedHashMap<>(Map.of("id", id)));
    }

    @PutMapping("/{id:\\d+}")
    public Result<Map<String, Object>> update(
            @PathVariable int id, @RequestBody RechargePlanGroupSaveRequest body) {
        rechargePlanGroupService.update(id, body);
        return Result.success("更新成功", new LinkedHashMap<>(Map.of("id", id)));
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        rechargePlanGroupService.delete(id);
        return Result.success("删除成功", null);
    }
}
