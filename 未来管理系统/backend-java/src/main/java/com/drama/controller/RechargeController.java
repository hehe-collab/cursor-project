package com.drama.controller;

import com.drama.annotation.RateLimit;
import com.drama.common.Result;
import com.drama.dto.RechargeQueryParam;
import com.drama.service.RechargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "充值管理", description = "充值记录、方案、方案组相关接口")
@RestController
@RequestMapping("/api/recharge")
@RequiredArgsConstructor
public class RechargeController {

    private final RechargeService rechargeService;

    /** 必须在 /{id} 之前；筛选参数与列表接口一致（不含分页） */
    @Operation(summary = "获取充值账户选项", description = "获取充值记录中出现过的账户列表")
    @GetMapping("/account-options")
    public Result<java.util.List<Map<String, Object>>> accountOptions() {
        return Result.success(rechargeService.accountOptions());
    }

    /** 必须在 /{id} 之前；筛选参数与列表接口一致（不含分页） */
    @Operation(summary = "获取充值统计", description = "获取充值记录的统计信息")
    @GetMapping("/stats")
    @RateLimit(key = "recharge:stats", max = 30, timeout = 60, limitType = RateLimit.LimitType.USER)
    public Result<Map<String, Object>> stats(
            @Parameter(description = "用户ID") @RequestParam(required = false) String user_id,
            @Parameter(description = "推广ID") @RequestParam(required = false) String promotion_id,
            @Parameter(description = "订单ID") @RequestParam(required = false) String order_id,
            @Parameter(description = "外部订单ID") @RequestParam(required = false) String external_order_id,
            @Parameter(description = "平台") @RequestParam(required = false) String platform,
            @Parameter(description = "账户ID") @RequestParam(required = false) String account_id,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "开始日期") @RequestParam(required = false) String start_date,
            @Parameter(description = "结束日期") @RequestParam(required = false) String end_date) {
        RechargeQueryParam q = new RechargeQueryParam();
        q.setUserId(user_id);
        q.setPromotionId(promotion_id);
        q.setOrderId(order_id);
        q.setExternalOrderId(external_order_id);
        q.setPlatform(platform);
        q.setAccountId(account_id);
        q.setCountry(country);
        q.setStartDate(start_date);
        q.setEndDate(end_date);
        return Result.success(rechargeService.stats(q));
    }

    @Operation(summary = "获取充值记录列表", description = "获取充值记录列表，支持筛选和分页")
    @GetMapping
    public Result<Map<String, Object>> list(
            @Parameter(description = "用户ID") @RequestParam(required = false) String user_id,
            @Parameter(description = "推广ID") @RequestParam(required = false) String promotion_id,
            @Parameter(description = "订单ID") @RequestParam(required = false) String order_id,
            @Parameter(description = "外部订单ID") @RequestParam(required = false) String external_order_id,
            @Parameter(description = "平台") @RequestParam(required = false) String platform,
            @Parameter(description = "账户ID") @RequestParam(required = false) String account_id,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "开始日期") @RequestParam(required = false) String start_date,
            @Parameter(description = "结束日期") @RequestParam(required = false) String end_date,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        RechargeQueryParam q = new RechargeQueryParam();
        q.setUserId(user_id);
        q.setPromotionId(promotion_id);
        q.setOrderId(order_id);
        q.setExternalOrderId(external_order_id);
        q.setPlatform(platform);
        q.setAccountId(account_id);
        q.setCountry(country);
        q.setStartDate(start_date);
        q.setEndDate(end_date);
        q.setStatus(status);
        q.setPage(page);
        q.setPageSize(pageSize);
        return Result.success(rechargeService.list(q));
    }

    @Operation(summary = "获取充值记录详情", description = "根据ID获取充值记录详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable long id) {
        return Result.success(rechargeService.getById(id));
    }

    @Operation(summary = "创建充值记录", description = "创建一条新的充值记录")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.success("创建成功", rechargeService.create(body));
    }

    @Operation(summary = "更新充值记录", description = "更新指定充值记录的信息")
    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        rechargeService.update(id, body);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除充值记录", description = "删除指定的充值记录")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable long id) {
        rechargeService.delete(id);
        return Result.success("删除成功", null);
    }
}
