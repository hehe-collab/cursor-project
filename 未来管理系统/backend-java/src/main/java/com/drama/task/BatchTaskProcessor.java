package com.drama.task;

import com.alibaba.fastjson2.JSON;
import com.drama.entity.BatchTask;
import com.drama.mapper.AdTaskMapper;
import com.drama.mapper.BatchTaskMapper;
import com.drama.entity.AdTask;
import com.drama.service.BatchAdLaunchService;
import com.drama.service.BatchTaskService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 异步执行批量任务，脱离 HTTP 请求线程。
 * <p>
 * 采用 @Async 提交到 taskExecutor 线程池执行；
 * @Scheduled 用于兜底检查卡住的任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchTaskProcessor {

    private final BatchTaskService batchTaskService;
    private final BatchAdLaunchService batchAdLaunchService;
    private final AdTaskMapper adTaskMapper;
    private final BatchTaskMapper batchTaskMapper;

    /**
     * 异步执行一个批量任务。由 Controller/Service 创建任务后调用。
     * 在独立线程中运行，不阻塞 HTTP 请求。
     */
    @Async("taskExecutor")
    public void executeAsync(String taskId) {
        log.info("异步开始处理批量任务: taskId={}", taskId);
        try {
            BatchTask task = batchTaskService.getByTaskId(taskId);
            if (task == null) {
                log.error("批量任务不存在: taskId={}", taskId);
                return;
            }
            if (!"pending".equals(task.getStatus())) {
                log.warn("批量任务状态非 pending，跳过: taskId={}, status={}", taskId, task.getStatus());
                return;
            }

            batchTaskService.markStarted(taskId);

            Map<String, Object> configMap;
            try {
                configMap = JSON.parseObject(task.getConfigJson(), new com.alibaba.fastjson2.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.error("解析任务配置失败: taskId={}", taskId, e);
                batchTaskService.markCompleted(taskId, "failed", null);
                batchTaskService.getByTaskId(taskId); // refresh
                return;
            }

            if (batchTaskService.isCancelled(taskId)) {
                log.info("任务已被取消，跳过执行: taskId={}", taskId);
                return;
            }

            // 复用现有的 BatchAdLaunchService.execute()，它已经包含完整的
            // campaign → adgroup → ad 三阶段处理逻辑、错误处理和结果收集。
            // 在异步线程中调用，不阻塞 HTTP 请求线程。
            Map<String, Object> execution = batchAdLaunchService.execute(configMap);

            if (batchTaskService.isCancelled(taskId)) {
                log.info("任务执行完成但已被标记取消: taskId={}", taskId);
                return;
            }

            // 从 execution 结果中提取计数，同步到 batch_tasks 表
            int successCount = intVal(execution.get("successCount"));
            int failedCount = intVal(execution.get("failedCount"));
            int totalCount = intVal(execution.get("totalCount"));
            String execStatus = text(execution.get("status"));

            // 更新 batch_tasks 表的最终状态
            String finalStatus;
            if ("success".equals(execStatus)) {
                finalStatus = "completed";
            } else if ("partial".equals(execStatus)) {
                finalStatus = "completed";
            } else {
                finalStatus = "failed";
            }

            String resultJson = JSON.toJSONString(execution);
            batchTaskService.markCompleted(taskId, finalStatus, resultJson);

            // 同步更新关联的 ad_tasks 记录（保持与原同步逻辑一致）
            syncAdTask(task.getAdTaskId(), configMap, execution);

            log.info("批量任务执行完成: taskId={}, status={}, success={}, failed={}",
                    taskId, finalStatus, successCount, failedCount);

        } catch (Exception e) {
            log.error("批量任务执行异常: taskId={}", taskId, e);
            try {
                batchTaskService.markCompleted(taskId, "failed", null);
                BatchTask task = batchTaskService.getByTaskId(taskId);
                if (task != null) {
                    batchTaskService.getByTaskId(taskId);
                }
            } catch (Exception ex) {
                log.error("更新任务失败状态异常: taskId={}", taskId, ex);
            }
        }
    }

    /**
     * 把执行结果同步回 ad_tasks 表（保持与原同步逻辑一致）
     */
    private void syncAdTask(String adTaskId, Map<String, Object> configMap, Map<String, Object> execution) {
        if (adTaskId == null || adTaskId.isBlank()) {
            return;
        }
        try {
            AdTask adTask = adTaskMapper.selectByTaskId(adTaskId);
            if (adTask == null) {
                return;
            }
            configMap.put("execution", execution);
            String execStatus = text(execution.get("status"));
            adTask.setStatus(firstNonBlank(execStatus, adTask.getStatus(), "failed"));
            adTask.setConfigJson(JSON.toJSONString(configMap));
            adTaskMapper.update(adTask);
            log.info("已同步 ad_tasks 记录: adTaskId={}, status={}", adTaskId, adTask.getStatus());
        } catch (Exception e) {
            log.error("同步 ad_tasks 失败: adTaskId={}", adTaskId, e);
        }
    }

    /**
     * 兜底定时检查：每 5 分钟扫描处于 processing 状态超过 30 分钟的任务，标记为失败。
     */
    @Scheduled(fixedDelay = 300_000)
    public void checkStuckTasks() {
        try {
            List<BatchTask> stuck = batchTaskMapper.selectStuckProcessing(30);
            for (BatchTask task : stuck) {
                log.warn("发现卡住的批量任务，标记为失败: taskId={}, startedAt={}", task.getTaskId(), task.getStartedAt());
                batchTaskService.markCompleted(task.getTaskId(), "failed", null);
            }
        } catch (Exception e) {
            log.error("检查卡住任务异常", e);
        }
    }

    private static int intVal(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value != null ? Integer.parseInt(String.valueOf(value)) : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "";
    }
}
