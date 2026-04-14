package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.AdTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "广告任务", description = "广告任务的创建与导出")
@RestController
@RequestMapping("/api/ad-task")
@RequiredArgsConstructor
public class AdTaskController {

    private final AdTaskService adTaskService;

    @Operation(summary = "导出广告任务", description = "导出广告任务到 Excel 文件")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@Parameter(description = "查询参数") @RequestParam Map<String, String> query) throws IOException {
        byte[] bytes = adTaskService.exportExcel(query);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ad_tasks.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @Operation(summary = "获取广告任务账户选项", description = "获取广告任务历史中出现过的账户列表")
    @GetMapping("/account-options")
    public Result<?> accountOptions() {
        return Result.success(adTaskService.accountOptions());
    }

    @Operation(summary = "获取广告任务列表", description = "获取广告任务列表，支持筛选和分页")
    @GetMapping
    public Result<?> list(
            @Parameter(description = "任务ID") @RequestParam(required = false) String task_id,
            @Parameter(description = "账户ID") @RequestParam(required = false) String account_id,
            @Parameter(description = "账户名称") @RequestParam(required = false) String account_name,
            @Parameter(description = "任务状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        Map<String, String> q = new HashMap<>();
        if (task_id != null) {
            q.put("task_id", task_id);
        }
        if (account_id != null) {
            q.put("account_id", account_id);
        }
        if (account_name != null) {
            q.put("account_name", account_name);
        }
        if (status != null) {
            q.put("status", status);
        }
        return Result.success("success", adTaskService.listFiltered(q, page, pageSize));
    }

    @Operation(summary = "获取广告任务详情", description = "根据ID获取广告任务详细信息")
    @GetMapping("/{id}")
    public Result<?> one(@PathVariable("id") String id) {
        Map<String, Object> task = adTaskService.getOne(id);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(task);
    }

    @Operation(summary = "创建广告任务", description = "创建一个新的广告任务")
    @PostMapping
    public Result<?> create(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        Map<String, Object> task = adTaskService.create(body, adminId);
        return Result.success("任务创建成功", task);
    }
}
