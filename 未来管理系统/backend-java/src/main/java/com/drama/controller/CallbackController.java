package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.CallbackConfig;
import com.drama.mapper.AdminMapper;
import com.drama.service.CallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "回传配置", description = "回传配置与日志查询")
@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class CallbackController {

    private final CallbackService callbackService;
    private final AdminMapper adminMapper;

    @Operation(summary = "获取回传统计", description = "获取回传配置与日志的统计信息")
    @GetMapping("/stats")
    public Result<?> stats() {
        return Result.success(callbackService.getStats());
    }

    @Operation(summary = "获取回传日志", description = "获取回传日志列表，支持多条件筛选和分页")
    @GetMapping("/logs")
    public Result<?> logs(
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "事件类型") @RequestParam(required = false) String event_type,
            @Parameter(description = "事件类型") @RequestParam(required = false) String eventType,
            @Parameter(description = "订单ID") @RequestParam(required = false) String order_id,
            @Parameter(description = "订单ID") @RequestParam(required = false) String orderNo,
            @Parameter(description = "订单ID") @RequestParam(required = false) String order_no,
            @Parameter(description = "用户ID") @RequestParam(required = false) String user_id,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "推广ID") @RequestParam(required = false) String promotion_id,
            @Parameter(description = "推广ID") @RequestParam(required = false) String promotionId,
            @Parameter(description = "开始日期") @RequestParam(required = false) String dateStart,
            @Parameter(description = "开始日期") @RequestParam(required = false) String start_date,
            @Parameter(description = "结束日期") @RequestParam(required = false) String dateEnd,
            @Parameter(description = "结束日期") @RequestParam(required = false) String end_date,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        String et = event_type != null && !event_type.isBlank() ? event_type : eventType;
        String oid = firstNonBlank(order_id, orderNo, order_no);
        String ds = firstNonBlank(dateStart, start_date);
        String de = firstNonBlank(dateEnd, end_date);
        String uid = firstNonBlank(user_id, userId);
        String pid = firstNonBlank(promotion_id, promotionId);
        return Result.success(callbackService.listLogs(status, et, oid, uid, pid, ds, de, page, pageSize));
    }

    @Operation(summary = "批量删除回传配置", description = "批量删除多条回传配置")
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

    @Operation(summary = "获取回传配置列表", description = "获取回传配置列表，支持筛选和分页")
    @GetMapping("/config")
    public Result<?> listConfigs(
            @Parameter(description = "平台") @RequestParam(required = false) String platform,
            @Parameter(description = "链接ID") @RequestParam(required = false) String link_id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(callbackService.listConfigs(platform, link_id, page, pageSize));
    }

    @Operation(summary = "创建回传配置", description = "创建一个新的回传配置")
    @PostMapping("/config")
    public Result<?> createConfig(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        body.remove("id");
        String creator = resolveCreator(adminId);
        CallbackConfig row = callbackService.createOrUpdateConfig(body, creator);
        if (row == null) {
            return Result.error(404, "配置不存在");
        }
        return Result.success("保存成功", row);
    }

    @Operation(summary = "更新回传配置", description = "更新指定回传配置的信息")
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

    @Operation(summary = "删除回传配置", description = "删除指定的回传配置")
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
