package com.drama.service;

import com.alibaba.fastjson2.JSON;
import com.drama.entity.BatchTask;
import com.drama.entity.BatchTaskItem;
import com.drama.mapper.BatchTaskMapper;
import com.drama.mapper.BatchTaskItemMapper;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTaskService {

    private final BatchTaskMapper batchTaskMapper;
    private final BatchTaskItemMapper batchTaskItemMapper;

    /**
     * 已取消任务的 taskId 集合，供 Processor 在执行过程中检查。
     * 用内存 Set 替代 Redis，单机部署够用。
     */
    private final ConcurrentHashMap.KeySetView<String, Boolean> cancelledTasks =
            ConcurrentHashMap.newKeySet();

    @Transactional(rollbackFor = Exception.class)
    public BatchTask createTask(String taskType, Integer userId, String userName,
                                String adTaskId, Map<String, Object> configMap,
                                List<BatchTaskItem> items) {
        String taskId = UUID.randomUUID().toString().replace("-", "");

        BatchTask task = new BatchTask();
        task.setTaskId(taskId);
        task.setTaskType(taskType);
        task.setUserId(userId);
        task.setUserName(userName);
        task.setAdTaskId(adTaskId);
        task.setTotalCount(items.size());
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setStatus("pending");
        task.setProgress(0);
        task.setConfigJson(JSON.toJSONString(configMap));
        task.setCreatedAt(LocalDateTime.now());
        batchTaskMapper.insert(task);

        if (!items.isEmpty()) {
            for (BatchTaskItem item : items) {
                item.setTaskId(taskId);
                item.setStatus("pending");
                item.setRetryCount(0);
            }
            batchTaskItemMapper.insertBatch(items);
        }

        log.info("批量任务已创建: taskId={}, type={}, items={}", taskId, taskType, items.size());
        return batchTaskMapper.selectByTaskId(taskId);
    }

    public BatchTask getByTaskId(String taskId) {
        return batchTaskMapper.selectByTaskId(taskId);
    }

    public Map<String, Object> getTaskProgress(String taskId) {
        BatchTask task = batchTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            return null;
        }
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("taskId", task.getTaskId());
        progress.put("taskType", task.getTaskType());
        progress.put("adTaskId", task.getAdTaskId());
        progress.put("status", task.getStatus());
        progress.put("totalCount", task.getTotalCount());
        progress.put("successCount", task.getSuccessCount());
        progress.put("failedCount", task.getFailedCount());
        progress.put("progress", task.getProgress());
        progress.put("errorMessage", task.getErrorMessage());

        int pending = task.getTotalCount() - task.getSuccessCount() - task.getFailedCount();
        progress.put("pendingCount", Math.max(pending, 0));

        progress.put("startedAt", task.getStartedAt());
        progress.put("completedAt", task.getCompletedAt());
        progress.put("createdAt", task.getCreatedAt());

        boolean isCompleted = "completed".equals(task.getStatus())
                || "failed".equals(task.getStatus())
                || "cancelled".equals(task.getStatus());
        progress.put("isCompleted", isCompleted);

        long elapsedSeconds = 0;
        if (task.getStartedAt() != null) {
            LocalDateTime end = task.getCompletedAt() != null ? task.getCompletedAt() : LocalDateTime.now();
            elapsedSeconds = Duration.between(task.getStartedAt(), end).getSeconds();
        }
        progress.put("elapsedSeconds", elapsedSeconds);

        if (isCompleted && task.getResultJson() != null) {
            try {
                progress.put("result", JSON.parse(task.getResultJson()));
            } catch (Exception ignored) {
                progress.put("result", null);
            }
        }

        return progress;
    }

    public List<BatchTaskItem> getItems(String taskId) {
        return batchTaskItemMapper.selectByTaskId(taskId);
    }

    public void markStarted(String taskId) {
        batchTaskMapper.updateStarted(taskId);
    }

    public void markItemSuccess(String taskId, Long itemId, String resultId) {
        batchTaskItemMapper.updateStatus(itemId, "success", resultId, null);
        batchTaskMapper.incrementSuccess(taskId);
        batchTaskMapper.updateProgress(taskId);
    }

    public void markItemFailed(String taskId, Long itemId, String errorMessage) {
        batchTaskItemMapper.updateStatus(itemId, "failed", null, errorMessage);
        batchTaskMapper.incrementFailed(taskId);
        batchTaskMapper.updateProgress(taskId);
    }

    public void markItemSkipped(String taskId, Long itemId, String reason) {
        batchTaskItemMapper.updateStatus(itemId, "skipped", null, reason);
        batchTaskMapper.incrementSuccess(taskId);
        batchTaskMapper.updateProgress(taskId);
    }

    public void markCompleted(String taskId, String status, String resultJson) {
        batchTaskMapper.updateCompleted(taskId, status, resultJson);
        cancelledTasks.remove(taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(String taskId) {
        BatchTask task = batchTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            return false;
        }
        if ("completed".equals(task.getStatus()) || "cancelled".equals(task.getStatus())) {
            return false;
        }
        cancelledTasks.add(taskId);
        batchTaskMapper.updateStatus(taskId, "cancelled");
        batchTaskItemMapper.updateStatusByTaskIdAndStatus(taskId, "pending", "cancelled");
        log.info("批量任务已取消: taskId={}", taskId);
        return true;
    }

    public boolean isCancelled(String taskId) {
        return cancelledTasks.contains(taskId);
    }

    public List<Map<String, Object>> getUserTasks(Integer userId, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * pageSize;
        List<BatchTask> tasks = batchTaskMapper.selectByUserId(userId, pageSize, offset);
        List<Map<String, Object>> result = new ArrayList<>();
        for (BatchTask task : tasks) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskId", task.getTaskId());
            item.put("taskType", task.getTaskType());
            item.put("adTaskId", task.getAdTaskId());
            item.put("totalCount", task.getTotalCount());
            item.put("successCount", task.getSuccessCount());
            item.put("failedCount", task.getFailedCount());
            item.put("status", task.getStatus());
            item.put("progress", task.getProgress());
            item.put("createdAt", task.getCreatedAt());
            item.put("completedAt", task.getCompletedAt());
            result.add(item);
        }
        return result;
    }
}
