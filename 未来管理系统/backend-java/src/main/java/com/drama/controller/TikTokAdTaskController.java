package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokAdTask;
import com.drama.service.TikTokAdTaskService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** TikTok 广告任务（{@code /api/tiktok/tasks}，需 Bearer） */
@Slf4j
@RestController
@RequestMapping("/api/tiktok/tasks")
@RequiredArgsConstructor
public class TikTokAdTaskController {

    private final TikTokAdTaskService taskService;

    @GetMapping
    public Result<Map<String, Object>> getTasks(
            @RequestParam(required = false) String advertiserId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            List<TikTokAdTask> tasks = taskService.getTasks(advertiserId, status, page, pageSize);
            Map<String, Object> data = new HashMap<>();
            data.put("list", tasks);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("total", tasks.size());
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get tasks: {}", e.getMessage(), e);
            return Result.error("获取任务列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id:\\d+}")
    public Result<TikTokAdTask> getTaskById(@PathVariable Long id) {
        try {
            TikTokAdTask task = taskService.getTaskById(id);
            return Result.success(task);
        } catch (Exception e) {
            log.error("Failed to get task {}: {}", id, e.getMessage(), e);
            return Result.error("获取任务失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<TikTokAdTask> createTask(@RequestBody TikTokAdTask task) {
        try {
            TikTokAdTask result = taskService.createTask(task);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to create task: {}", e.getMessage(), e);
            return Result.error("创建任务失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id:\\d+}/status")
    public Result<TikTokAdTask> updateTaskStatus(
            @PathVariable Long id, @RequestBody Map<String, String> params) {
        try {
            String status = params.get("status");
            if (status == null || status.isEmpty()) {
                return Result.error("status 不能为空");
            }
            TikTokAdTask result = taskService.updateTaskStatus(id, status);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to update task status {}: {}", id, e.getMessage(), e);
            return Result.error("更新任务状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/process")
    public Result<Void> processPendingTasks(@RequestParam(defaultValue = "10") int limit) {
        try {
            taskService.processPendingTasks(limit);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to process pending tasks: {}", e.getMessage(), e);
            return Result.error("处理待执行任务失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to delete task {}: {}", id, e.getMessage(), e);
            return Result.error("删除任务失败: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getTaskStats(
            @RequestParam String advertiserId, @RequestParam(required = false) String status) {
        try {
            int count = taskService.countTasks(advertiserId, status);
            Map<String, Object> stats = new HashMap<>();
            stats.put("advertiser_id", advertiserId);
            stats.put("status", status);
            stats.put("count", count);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to get task stats: {}", e.getMessage(), e);
            return Result.error("获取任务统计失败: " + e.getMessage());
        }
    }
}
