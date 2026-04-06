package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.AdTaskService;
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

@RestController
@RequestMapping("/api/ad-task")
@RequiredArgsConstructor
public class AdTaskController {

    private final AdTaskService adTaskService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam Map<String, String> query) throws IOException {
        byte[] bytes = adTaskService.exportExcel(query);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ad_tasks.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping
    public Result<?> list(
            @RequestParam(required = false) String task_id,
            @RequestParam(required = false) String account_id,
            @RequestParam(required = false) String account_name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
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

    @GetMapping("/{id}")
    public Result<?> one(@PathVariable("id") String id) {
        Map<String, Object> task = adTaskService.getOne(id);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(task);
    }

    @PostMapping
    public Result<?> create(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        Map<String, Object> task = adTaskService.create(body, adminId);
        return Result.success("任务创建成功", task);
    }
}
