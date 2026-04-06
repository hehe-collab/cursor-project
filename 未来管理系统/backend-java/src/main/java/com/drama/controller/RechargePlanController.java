package com.drama.controller;

import com.drama.common.Result;
import com.drama.dto.RechargePlanSaveRequest;
import com.drama.service.RechargePlanService;
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

@RestController
@RequestMapping("/api/recharge-plans")
@RequiredArgsConstructor
public class RechargePlanController {

    private final RechargePlanService rechargePlanService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String payment_platform) {
        return Result.success(rechargePlanService.list(page, pageSize, name, id, payment_platform));
    }

    @PostMapping
    public Result<Map<String, Object>> create(
            @RequestBody RechargePlanSaveRequest body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        int id =
                rechargePlanService.create(
                        body, adminId, adminId != null ? String.valueOf(adminId) : "");
        return Result.success("新增成功", new LinkedHashMap<>(Map.of("id", id)));
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> update(
            @PathVariable int id,
            @RequestBody RechargePlanSaveRequest body) {
        rechargePlanService.update(id, body);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        rechargePlanService.delete(id);
        return Result.success("删除成功", null);
    }
}
