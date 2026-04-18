package com.drama.service;

import com.alibaba.fastjson2.JSON;
import com.drama.entity.AdAccount;
import com.drama.entity.AdTask;
import com.drama.entity.BatchTask;
import com.drama.entity.BatchTaskItem;
import com.drama.mapper.AdAccountMapper;
import com.drama.mapper.AdTaskMapper;
import com.drama.mapper.AdminMapper;
import com.drama.service.BatchTaskService;
import com.drama.task.BatchTaskProcessor;
import com.drama.util.ExcelExportUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdTaskService {

    private static final DateTimeFormatter CREATED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AdTaskMapper adTaskMapper;
    private final AdAccountMapper adAccountMapper;
    private final AdminMapper adminMapper;
    private final BatchTaskService batchTaskService;
    private final BatchTaskProcessor batchTaskProcessor;

    public Map<String, Object> listFiltered(Map<String, String> query, int page, int pageSize) {
        List<Map<String, Object>> tasks = filterTasks(query);
        int total = tasks.size();
        int p = Math.max(1, page);
        int ps = Math.max(1, pageSize);
        int from = (p - 1) * ps;
        List<Map<String, Object>> slice =
                tasks.subList(Math.min(from, total), Math.min(from + ps, total));
        Map<String, Object> data = new HashMap<>();
        data.put("list", slice);
        data.put("total", total);
        data.put("page", p);
        data.put("pageSize", ps);
        return data;
    }

    public Map<String, Object> getOne(String idOrTaskId) {
        AdTask raw = findRaw(idOrTaskId);
        if (raw == null) {
            return null;
        }
        return normalizeTask(raw);
    }

    public List<Map<String, Object>> accountOptions() {
        Map<String, AdAccount> accountSnapshot = loadAccountSnapshot();
        Map<String, Map<String, Object>> dedup = new LinkedHashMap<>();
        for (AdTask task : adTaskMapper.selectAllOrderByCreatedDesc()) {
            List<String> ids = splitCsv(task.getAccountIds());
            List<String> names = splitCsv(task.getAccountNames());
            LinkedHashSet<String> seenInTask = new LinkedHashSet<>();
            for (int i = 0; i < ids.size(); i++) {
                String accountId = ids.get(i);
                if (accountId.isBlank() || !seenInTask.add(accountId)) {
                    continue;
                }
                AdAccount account = accountSnapshot.get(accountId);
                String historyName = i < names.size() ? names.get(i) : "";
                Map<String, Object> item =
                        dedup.computeIfAbsent(
                                accountId,
                                key -> {
                                    Map<String, Object> created = new LinkedHashMap<>();
                                    created.put("accountId", key);
                                    created.put(
                                            "accountName",
                                            firstNonBlank(
                                                    historyName,
                                                    account != null ? account.getAccountName() : ""));
                                    created.put("subjectName", account != null ? nz(account.getSubjectName()) : "");
                                    created.put("taskCount", 0);
                                    return created;
                                });
                if (nz(item.get("accountName")).isBlank() && !historyName.isBlank()) {
                    item.put("accountName", historyName);
                }
                item.put("taskCount", intVal(item.get("taskCount")) + 1);
            }
        }
        return dedup.values().stream()
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("accountId"))))
                .toList();
    }

    private Map<String, AdAccount> loadAccountSnapshot() {
        Map<String, AdAccount> snapshot = new LinkedHashMap<>();
        for (AdAccount account : adAccountMapper.selectAllOrderByIdDesc()) {
            String accountId = nz(account.getAccountId()).trim();
            if (!accountId.isEmpty() && !snapshot.containsKey(accountId)) {
                snapshot.put(accountId, account);
            }
        }
        return snapshot;
    }

    public byte[] exportExcel(Map<String, String> query) throws IOException {
        List<Map<String, Object>> tasks = filterTasks(query);
        String[] headers = {"任务ID", "账户ID", "账户名称", "推广类型", "状态", "创建人", "创建时间"};
        List<Object[]> rows = new ArrayList<>();
        for (Map<String, Object> t : tasks) {
            rows.add(new Object[] {
                t.get("task_id"),
                t.get("account_ids"),
                t.get("account_names"),
                t.get("promotion_type"),
                statusText(String.valueOf(t.get("status"))),
                t.get("created_by"),
                t.get("created_at"),
            });
        }
        if (rows.isEmpty()) {
            rows.add(new Object[] {""});
        }
        return ExcelExportUtil.buildXlsx("广告任务", headers, rows);
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, Integer adminId) {
        String taskId = body.get("task_id") != null ? String.valueOf(body.get("task_id")) : null;
        if (taskId == null || taskId.isBlank()) {
            taskId = "T" + System.currentTimeMillis();
        }
        String createdBy = body.get("created_by") != null ? String.valueOf(body.get("created_by")) : null;
        if (createdBy == null || createdBy.isBlank()) {
            createdBy = resolveCreator(adminId);
        }
        String accountIds = normalizeCsv(str(body.get("account_ids")));
        String accountNames = resolveAccountNames(accountIds, str(body.get("account_names")));
        String promotionType = str(body.get("promotion_type"));
        String status = str(body.get("status"));
        if (status == null || status.isBlank()) {
            status = "running";
        }
        Object config = body.get("config");
        Map<String, Object> configMap = asMap(config);
        String configJson = config == null ? null : JSON.toJSONString(configMap != null ? configMap : config);

        AdTask row = new AdTask();
        row.setTaskId(taskId);
        row.setAccountIds(accountIds != null ? accountIds : "");
        row.setAccountNames(accountNames != null ? accountNames : "");
        row.setPromotionType(promotionType != null ? promotionType : "");
        row.setStatus(status);
        row.setCreatedBy(createdBy);
        row.setCreatedAt(LocalDateTime.now());
        row.setConfigJson(configJson);
        adTaskMapper.insert(row);
        AdTask inserted = adTaskMapper.selectByTaskId(taskId);

        if (configMap != null && shouldExecuteBatch(configMap)) {
            // 异步执行：创建 batch_task 记录，提交到线程池异步处理
            List<Map<String, Object>> projects = asList(configMap.get("projects"));
            int itemCount = Math.max(projects.size(), 1);

            List<BatchTaskItem> items = new ArrayList<>();
            for (int i = 0; i < projects.size(); i++) {
                BatchTaskItem item = new BatchTaskItem();
                item.setItemIndex(i + 1);
                item.setStage("project");
                Map<String, Object> project = projects.get(i);
                item.setProjectId(project != null ? str(project.get("id")) : "");
                item.setAdvertiserId(project != null ? str(project.get("accountId")) : "");
                item.setItemData(project != null ? JSON.toJSONString(project) : "{}");
                items.add(item);
            }

            BatchTask batchTask = batchTaskService.createTask(
                    "ad_batch_launch", adminId, createdBy,
                    taskId, configMap, items);

            // 提交异步执行（在独立线程中处理，立即返回）
            batchTaskProcessor.executeAsync(batchTask.getTaskId());

            // 在返回结果中附带 batchTaskId，前端据此轮询进度
            Map<String, Object> result = normalizeTask(inserted);
            result.put("batchTaskId", batchTask.getTaskId());
            result.put("async", true);
            return result;
        }
        return normalizeTask(inserted);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    row.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                out.add(row);
            }
        }
        return out;
    }

    private boolean shouldExecuteBatch(Map<String, Object> config) {
        return config != null && config.get("projects") instanceof List<?>;
    }

    private Map<String, Object> asMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            out.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return out;
    }

    private String resolveAccountNames(String accountIdsRaw, String accountNamesRaw) {
        List<String> ids = splitCsv(accountIdsRaw);
        List<String> names = splitCsv(accountNamesRaw);
        if (ids.isEmpty()) {
            return normalizeCsv(accountNamesRaw);
        }
        Map<String, AdAccount> snapshot = loadAccountSnapshot();
        List<String> resolved = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            String name = i < names.size() ? names.get(i) : "";
            if (name.isBlank()) {
                AdAccount account = snapshot.get(id);
                name = account != null ? nz(account.getAccountName()) : "";
            }
            resolved.add(name);
        }
        return String.join(",", resolved);
    }

    private AdTask findRaw(String id) {
        AdTask byTask = adTaskMapper.selectByTaskId(id);
        if (byTask != null) {
            return byTask;
        }
        try {
            int nid = Integer.parseInt(id, 10);
            return adTaskMapper.selectByNumericId(nid);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<Map<String, Object>> filterTasks(Map<String, String> query) {
        String taskId = query.get("task_id");
        String accountId = query.get("account_id");
        String accountName = query.get("account_name");
        String status = query.get("status");
        List<AdTask> all = adTaskMapper.selectAllOrderByCreatedDesc();
        List<Map<String, Object>> tasks =
                all.stream().map(this::normalizeTask).collect(Collectors.toList());
        if (taskId != null && !taskId.isBlank()) {
            tasks = tasks.stream()
                    .filter(t -> String.valueOf(t.get("task_id")).contains(taskId))
                    .collect(Collectors.toList());
        }
        if (accountId != null && !accountId.isBlank()) {
            final String aid = accountId;
            tasks = tasks.stream()
                    .filter(t -> {
                        String a = String.valueOf(t.get("account_ids"));
                        return a.contains(aid);
                    })
                    .collect(Collectors.toList());
        }
        if (accountName != null && !accountName.isBlank()) {
            final String an = accountName;
            tasks = tasks.stream()
                    .filter(t -> {
                        String n = String.valueOf(t.get("account_names"));
                        return n.contains(an);
                    })
                    .collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            tasks = tasks.stream()
                    .filter(t -> status.equals(String.valueOf(t.get("status"))))
                    .collect(Collectors.toList());
        }
        tasks.sort((a, b) -> Long.compare(createdMs(b.get("created_at")), createdMs(a.get("created_at"))));
        return tasks;
    }

    private long createdMs(Object createdAt) {
        if (createdAt == null) {
            return 0;
        }
        if (createdAt instanceof LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        String s = String.valueOf(createdAt);
        try {
            return LocalDateTime.parse(s, CREATED_FMT).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> normalizeTask(AdTask t) {
        Map<String, Object> m = new HashMap<>();
        String taskId = t.getTaskId();
        if (taskId != null && !taskId.isBlank()) {
            m.put("task_id", taskId);
            m.put("account_ids", nz(t.getAccountIds()));
            m.put("account_names", nz(t.getAccountNames()));
            m.put("promotion_type", nz(t.getPromotionType()));
            m.put("status", nz(t.getStatus()));
            m.put("created_by", nz(t.getCreatedBy()));
            m.put("created_at", formatCreated(t.getCreatedAt()));
            m.put("config", parseConfig(t.getConfigJson()));
            return m;
        }
        m.put("task_id", t.getId() != null ? String.valueOf(t.getId()) : "");
        m.put("account_ids", nz(t.getAccountIds()));
        m.put("account_names", nz(t.getAccountNames()));
        m.put("promotion_type", nz(t.getPromotionType()));
        if ("stopped".equalsIgnoreCase(String.valueOf(t.getStatus()))) {
            m.put("status", "failed");
        } else if ("running".equalsIgnoreCase(String.valueOf(t.getStatus()))) {
            m.put("status", "running");
        } else {
            m.put("status", "success");
        }
        m.put("created_by", nz(t.getCreatedBy()));
        m.put("created_at", formatCreated(t.getCreatedAt()));
        m.put("config", parseConfig(t.getConfigJson()));
        return m;
    }

    private Object parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON.parse(json);
        } catch (Exception e) {
            return json;
        }
    }

    private String formatCreated(LocalDateTime at) {
        if (at == null) {
            return "";
        }
        return CREATED_FMT.format(at);
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }

    private static String nz(Object value) {
        return value != null ? String.valueOf(value) : "";
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

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String statusText(String status) {
        if ("success".equalsIgnoreCase(status)) {
            return "成功";
        }
        if ("partial".equalsIgnoreCase(status)) {
            return "部分成功";
        }
        if ("failed".equalsIgnoreCase(status)) {
            return "失败";
        }
        if ("running".equalsIgnoreCase(status)) {
            return "进行中";
        }
        return "未知";
    }

    private String resolveCreator(Integer adminId) {
        if (adminId == null) {
            return "admin";
        }
        var a = adminMapper.selectById(adminId);
        if (a == null) {
            return "admin";
        }
        if (a.getUsername() != null && !a.getUsername().isBlank()) {
            return a.getUsername();
        }
        return a.getNickname() != null ? a.getNickname() : "admin";
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String normalizeCsv(String raw) {
        return String.join(",", splitCsv(raw));
    }
}
