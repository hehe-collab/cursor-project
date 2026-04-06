package com.drama.service;

import com.alibaba.fastjson2.JSON;
import com.drama.entity.AdTask;
import com.drama.mapper.AdTaskMapper;
import com.drama.mapper.AdminMapper;
import com.drama.util.ExcelExportUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final AdminMapper adminMapper;

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
        String accountIds = str(body.get("account_ids"));
        String accountNames = str(body.get("account_names"));
        String promotionType = str(body.get("promotion_type"));
        String status = str(body.get("status"));
        if (status == null || status.isBlank()) {
            status = "running";
        }
        Object config = body.get("config");
        String configJson = config == null ? null : JSON.toJSONString(config);

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
        return normalizeTask(inserted);
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

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String statusText(String status) {
        if ("success".equalsIgnoreCase(status)) {
            return "成功";
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
}
