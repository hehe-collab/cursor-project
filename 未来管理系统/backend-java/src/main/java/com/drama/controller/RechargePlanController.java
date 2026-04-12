package com.drama.controller;

import com.drama.common.Result;
import com.drama.dto.RechargePlanSaveRequest;
import com.drama.service.RechargePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
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

@Tag(name = "充值方案", description = "充值方案的 CRUD 操作")
@RestController
@RequestMapping("/api/recharge-plans")
@RequiredArgsConstructor
public class RechargePlanController {

    private final RechargePlanService rechargePlanService;

    @Operation(summary = "获取充值方案列表", description = "获取充值方案列表，支持筛选和分页")
    @GetMapping
    public Result<Map<String, Object>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "方案名称") @RequestParam(required = false) String name,
            @Parameter(description = "方案ID") @RequestParam(required = false) String id,
            @Parameter(description = "支付平台") @RequestParam(required = false) String payment_platform) {
        return Result.success(rechargePlanService.list(page, pageSize, name, id, payment_platform));
    }

    @Operation(summary = "创建充值方案", description = "创建一个新的充值方案")
    @PostMapping
    public Result<Map<String, Object>> create(
            @RequestBody RechargePlanSaveRequest body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        int id =
                rechargePlanService.create(
                        body, adminId, adminId != null ? String.valueOf(adminId) : "");
        return Result.success("新增成功", new LinkedHashMap<>(Map.of("id", id)));
    }

    @Operation(summary = "更新充值方案", description = "更新指定充值方案的信息")
    @PutMapping("/{id:\\d+}")
    public Result<Void> update(
            @PathVariable int id,
            @RequestBody RechargePlanSaveRequest body) {
        rechargePlanService.update(id, body);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除充值方案", description = "删除指定的充值方案")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        rechargePlanService.delete(id);
        return Result.success("删除成功", null);
    }
}
