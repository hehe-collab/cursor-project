package com.drama.controller;

import com.drama.common.Result;
import com.drama.dto.RechargeQueryParam;
import com.drama.service.RechargeService;
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

@RestController
@RequestMapping("/api/recharge")
@RequiredArgsConstructor
public class RechargeController {

    private final RechargeService rechargeService;

    /** 必须在 /{id} 之前；筛选参数与列表接口一致（不含分页） */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(
            @RequestParam(required = false) String user_id,
            @RequestParam(required = false) String promotion_id,
            @RequestParam(required = false) String order_id,
            @RequestParam(required = false) String external_order_id,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String account_id,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date) {
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

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String user_id,
            @RequestParam(required = false) String promotion_id,
            @RequestParam(required = false) String order_id,
            @RequestParam(required = false) String external_order_id,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String account_id,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
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

    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable long id) {
        return Result.success(rechargeService.getById(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.success("创建成功", rechargeService.create(body));
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        rechargeService.update(id, body);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable long id) {
        rechargeService.delete(id);
        return Result.success("删除成功", null);
    }
}
