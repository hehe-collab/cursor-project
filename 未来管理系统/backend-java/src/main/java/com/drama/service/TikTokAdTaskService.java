package com.drama.service;

import com.drama.entity.TikTokAdTask;
import com.drama.mapper.TikTokAdTaskMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TikTokAdTaskService {

    private final TikTokAdTaskMapper taskMapper;
    private final TikTokCampaignService campaignService;
    private final TikTokAdGroupService adGroupService;
    private final TikTokAdService adService;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    public List<TikTokAdTask> getTasks(String advertiserId, String status, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * pageSize;
        if (StringUtils.hasText(status)) {
            return taskMapper.selectByStatus(status, pageSize, offset);
        }
        if (StringUtils.hasText(advertiserId)) {
            return taskMapper.selectByAdvertiserId(advertiserId, pageSize, offset);
        }
        return List.of();
    }

    public TikTokAdTask getTaskById(Long id) {
        return getById(id);
    }

    public TikTokAdTask getById(Long id) {
        TikTokAdTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new IllegalStateException("Task not found: " + id);
        }
        return task;
    }

    @Transactional
    public TikTokAdTask createTask(TikTokAdTask task) {
        if (task.getStatus() == null) {
            task.setStatus("pending");
        }
        if (task.getRetryCount() == null) {
            task.setRetryCount(0);
        }
        if (task.getMaxRetries() == null) {
            task.setMaxRetries(3);
        }
        if (task.getPriority() == null) {
            task.setPriority(0);
        }
        taskMapper.insert(task);
        log.info("Created task: {} type={}", task.getId(), task.getTaskType());
        return taskMapper.selectById(task.getId());
    }

    @Transactional
    public TikTokAdTask updateTask(TikTokAdTask task) {
        taskMapper.update(task);
        return getById(task.getId());
    }

    @Transactional
    public TikTokAdTask updateTaskStatus(Long id, String status) {
        getById(id);
        taskMapper.updateStatus(id, status);
        log.info("Updated task status: {} -> {}", id, status);
        return getById(id);
    }

    @Transactional
    public void processPendingTasks(int limit) {
        List<TikTokAdTask> tasks = taskMapper.selectPendingTasks(limit);
        for (TikTokAdTask task : tasks) {
            try {
                task.setStatus("processing");
                task.setStartedAt(LocalDateTime.now());
                taskMapper.update(task);

                executeTask(task);

                task.setStatus("success");
                task.setCompletedAt(LocalDateTime.now());
                task.setErrorMessage(null);
                taskMapper.update(task);
                log.info("Task ok id={} type={}", task.getId(), task.getTaskType());
            } catch (Exception e) {
                log.error(
                        "Task failed id={} type={}: {}", task.getId(), task.getTaskType(), e.getMessage(), e);
                int nextRetry = (task.getRetryCount() != null ? task.getRetryCount() : 0) + 1;
                task.setRetryCount(nextRetry);
                task.setErrorMessage(e.getMessage());
                task.setCompletedAt(LocalDateTime.now());
                int max = task.getMaxRetries() != null ? task.getMaxRetries() : 3;
                if (nextRetry < max) {
                    task.setStatus("pending");
                    task.setScheduledAt(LocalDateTime.now().plusMinutes(5));
                } else {
                    task.setStatus("failed");
                }
                taskMapper.update(task);
            }
        }
        log.info("Processed {} pending tasks", tasks.size());
    }

    private void executeTask(TikTokAdTask task) throws Exception {
        String taskType = task.getTaskType();
        String taskParams = task.getTaskParams();
        if (!StringUtils.hasText(taskParams)) {
            throw new IllegalArgumentException("taskParams 为空");
        }
        Map<String, Object> params = objectMapper.readValue(taskParams, MAP_TYPE);

        switch (taskType) {
            case "create_campaign":
            case "create_adgroup":
            case "create_ad":
            case "update_campaign":
            case "update_adgroup":
            case "update_ad":
                throw new IllegalStateException("任务类型未实现: " + taskType);
            case "pause_campaign": {
                String campaignId = str(params.get("campaign_id"));
                campaignService.updateCampaignStatus(campaignId, "DISABLE");
                task.setResultId(campaignId);
                break;
            }
            case "enable_campaign": {
                String campaignId = str(params.get("campaign_id"));
                campaignService.updateCampaignStatus(campaignId, "ENABLE");
                task.setResultId(campaignId);
                break;
            }
            case "pause_adgroup": {
                String adgroupId = str(params.get("adgroup_id"));
                adGroupService.updateAdGroupStatus(adgroupId, "DISABLE");
                task.setResultId(adgroupId);
                break;
            }
            case "enable_adgroup": {
                String adgroupId = str(params.get("adgroup_id"));
                adGroupService.updateAdGroupStatus(adgroupId, "ENABLE");
                task.setResultId(adgroupId);
                break;
            }
            case "pause_ad": {
                String adId = str(params.get("ad_id"));
                adService.updateAdStatus(adId, "DISABLE");
                task.setResultId(adId);
                break;
            }
            case "enable_ad": {
                String adId = str(params.get("ad_id"));
                adService.updateAdStatus(adId, "ENABLE");
                task.setResultId(adId);
                break;
            }
            case "delete_campaign": {
                String campaignId = str(params.get("campaign_id"));
                campaignService.deleteCampaign(campaignId);
                task.setResultId(campaignId);
                break;
            }
            case "delete_adgroup": {
                String adgroupId = str(params.get("adgroup_id"));
                adGroupService.deleteAdGroup(adgroupId);
                task.setResultId(adgroupId);
                break;
            }
            case "delete_ad": {
                String adId = str(params.get("ad_id"));
                adService.deleteAd(adId);
                task.setResultId(adId);
                break;
            }
            default:
                throw new IllegalStateException("Unknown task type: " + taskType);
        }
    }

    private static String str(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("缺少必填 ID 参数");
        }
        return o.toString();
    }

    public List<TikTokAdTask> listPending(int limit) {
        return taskMapper.selectPendingTasks(limit);
    }

    public List<TikTokAdTask> listByAdvertiser(String advertiserId, int limit, int offset) {
        return taskMapper.selectByAdvertiserId(advertiserId, limit, offset);
    }

    public List<TikTokAdTask> listByStatus(String status, int limit, int offset) {
        return taskMapper.selectByStatus(status, limit, offset);
    }

    @Transactional
    public void deleteById(Long id) {
        getById(id);
        taskMapper.deleteById(id);
    }

    @Transactional
    public void deleteTask(Long id) {
        deleteById(id);
        log.info("Deleted task: {}", id);
    }

    public int countTasks(String advertiserId, String status) {
        return taskMapper.countByAdvertiserIdAndStatus(advertiserId, status);
    }
}
