package com.drama.controller;

import com.drama.common.Result;
import com.drama.dto.BatchIdsRequest;
import com.drama.dto.RechargePlanGroupSaveRequest;
import com.drama.service.RechargePlanGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "充值方案组", description = "充值方案组的 CRUD 与 Pixel 测试")
@RestController
@RequestMapping("/api/recharge-groups")
@RequiredArgsConstructor
public class RechargePlanGroupController {

    private final RechargePlanGroupService rechargePlanGroupService;

    /** 用户端/H5：无需登录（Jwt 过滤器放行） */
    @Operation(summary = "获取前端充值方案组", description = "获取用户端可用的充值方案组列表")
    @GetMapping("/frontend")
    public Result<List<Map<String, Object>>> frontend() {
        return Result.success(rechargePlanGroupService.frontendGroups());
    }

    @Operation(summary = "测试 Pixel", description = "测试 Pixel 配置是否有效")
    @PostMapping("/test-pixel")
    public Result<Map<String, Object>> testPixel(@RequestBody Map<String, Object> body) {
        return Result.success("Pixel 验证成功", rechargePlanGroupService.testPixel(body));
    }

    @Operation(summary = "批量删除充值方案组", description = "批量删除多条充值方案组")
    @PostMapping("/batch-delete")
    public Result<Map<String, Object>> batchDelete(@RequestBody BatchIdsRequest body) {
        List<Integer> ids = body != null ? body.getIds() : null;
        if (ids == null || ids.isEmpty()) {
            return Result.error(400, "请选择要删���的充值方案组");
        }
        int n = rechargePlanGroupService.deleteBatch(ids);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deleted", n);
        return Result.success("已删除 " + n + " 个充值方案组", data);
    }

    @Operation(summary = "获取充值方案组列表", description = "获取充值方案组列表，支持筛选和分页")
    @GetMapping
    public Result<Map<String, Object>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "方案组ID") @RequestParam(required = false) String group_id,
            @Parameter(description = "方案组名称") @RequestParam(required = false) String group_name) {
        return Result.success(
                "获取成功",
                rechargePlanGroupService.list(page, pageSize, group_id, group_name));
    }

    @Operation(summary = "获取充值方案组详情", description = "根据ID获取充值方案组详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable int id) {
        return Result.success("获取成功", rechargePlanGroupService.detail(id));
    }

    @Operation(summary = "创建充值方案组", description = "创建一个新的充值方案组")
    @PostMapping
    public Result<Map<String, Object>> create(
            @RequestBody RechargePlanGroupSaveRequest body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        int id = rechargePlanGroupService.create(body, adminId);
        return Result.success("新增成功", new LinkedHashMap<>(Map.of("id", id)));
    }

    @Operation(summary = "更新充值方案组", description = "更新指定充值方案组的信息")
    @PutMapping("/{id:\\d+}")
    public Result<Map<String, Object>> update(
            @PathVariable int id, @RequestBody RechargePlanGroupSaveRequest body) {
        rechargePlanGroupService.update(id, body);
        return Result.success("更新成功", new LinkedHashMap<>(Map.of("id", id)));
    }

    @Operation(summary = "删除充值方案组", description = "删除指定的充值方案组")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        rechargePlanGroupService.delete(id);
        return Result.success("删除成功", null);
    }
}
