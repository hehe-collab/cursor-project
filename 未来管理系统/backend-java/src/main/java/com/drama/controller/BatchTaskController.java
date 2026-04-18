package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.BatchTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "批量任务", description = "批量任务进度查询与管理")
@RestController
@RequestMapping("/api/batch-task")
@RequiredArgsConstructor
public class BatchTaskController {

    private final BatchTaskService batchTaskService;

    @Operation(summary = "查询任务进度")
    @GetMapping("/{taskId}")
    public Result<?> getProgress(@PathVariable String taskId) {
        Map<String, Object> progress = batchTaskService.getTaskProgress(taskId);
        if (progress == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(progress);
    }

    @Operation(summary = "获取当前用户的任务列表")
    @GetMapping("/my-tasks")
    public Result<?> myTasks(
            @RequestAttribute(value = "adminId", required = false) Integer adminId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<Map<String, Object>> tasks = batchTaskService.getUserTasks(adminId, page, pageSize);
        return Result.success(tasks);
    }

    @Operation(summary = "取消任务")
    @PostMapping("/{taskId}/cancel")
    public Result<?> cancelTask(@PathVariable String taskId) {
        boolean ok = batchTaskService.cancelTask(taskId);
        if (!ok) {
            return Result.error("任务无法取消（可能已完成或已取消）");
        }
        return Result.success("任务已取消", null);
    }
}
