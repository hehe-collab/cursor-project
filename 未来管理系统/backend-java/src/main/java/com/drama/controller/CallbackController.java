package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.CallbackConfig;
import com.drama.mapper.AdminMapper;
import com.drama.service.CallbackService;
import java.util.List;
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
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class CallbackController {

    private final CallbackService callbackService;
    private final AdminMapper adminMapper;

    @GetMapping("/stats")
    public Result<?> stats() {
        return Result.success(callbackService.getStats());
    }

    @GetMapping("/logs")
    public Result<?> logs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String event_type,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String order_id,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String order_no,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String dateEnd,
            @RequestParam(required = false) String end_date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        String et = event_type != null && !event_type.isBlank() ? event_type : eventType;
        String oid = firstNonBlank(order_id, orderNo, order_no);
        String ds = firstNonBlank(dateStart, start_date);
        String de = firstNonBlank(dateEnd, end_date);
        return Result.success(callbackService.listLogs(status, et, oid, ds, de, page, pageSize));
    }

    @PostMapping("/config/batch-delete")
    public Result<Void> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("ids");
        if (raw == null) {
            return Result.error(400, "请提供 ids");
        }
        List<Integer> ids =
                raw.stream().map(o -> o instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(o)))
                        .toList();
        callbackService.deleteConfigsBatch(ids);
        return Result.success("删除成功", null);
    }

    @GetMapping("/config")
    public Result<?> listConfigs(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String link_id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(callbackService.listConfigs(platform, link_id, page, pageSize));
    }

    @PostMapping("/config")
    public Result<?> createConfig(
            @RequestBody Map<String, Object> body, @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        body.remove("id");
        String creator = resolveCreator(adminId);
        CallbackConfig row = callbackService.createOrUpdateConfig(body, creator);
        if (row == null) {
            return Result.error(404, "配置不存在");
        }
        return Result.success("保存成功", row);
    }

    @PutMapping("/config/{id}")
    public Result<?> updateConfig(
            @PathVariable("id") int id,
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        body.put("id", id);
        String creator = resolveCreator(adminId);
        CallbackConfig row = callbackService.createOrUpdateConfig(body, creator);
        if (row == null) {
            return Result.error(404, "配置不存在");
        }
        return Result.success("保存成功", row);
    }

    @DeleteMapping("/config/{id}")
    public Result<Void> deleteConfig(@PathVariable("id") int id) {
        callbackService.deleteConfig(id);
        return Result.success("删除成功", null);
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

    private static String firstNonBlank(String... ss) {
        if (ss == null) {
            return null;
        }
        for (String s : ss) {
            if (s != null && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }
}
