package com.drama.controller;

import com.drama.common.Result;
import com.drama.exception.BusinessException;
import com.drama.service.H5Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "H5 用户端", description = "H5 播放器、匿名设备用户、充值接口")
@RestController
@RequestMapping("/api/h5")
@RequiredArgsConstructor
public class H5Controller {

    private final H5Service h5Service;

    @Operation(summary = "根据推广链接获取短剧与套餐信息")
    @GetMapping("/drama")
    public Result<Map<String, Object>> drama(
            @RequestParam("promo_id") String promoId,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return Result.success(h5Service.getDramaByPromo(promoId, deviceId));
    }

    @Operation(summary = "获取指定集播放地址")
    @GetMapping("/play")
    public Result<Map<String, Object>> play(
            @RequestParam("promo_id") String promoId,
            @RequestParam("episode") int episode,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        try {
            return Result.success(h5Service.getPlayInfo(promoId, episode, deviceId));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "获取匿名设备用户信息")
    @GetMapping("/user")
    public Result<Map<String, Object>> user(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return Result.success(h5Service.getUserInfo(deviceId));
    }

    @Operation(summary = "H5 发起充值")
    @PostMapping("/pay")
    public Result<Map<String, Object>> pay(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        Integer planId = null;
        Object rawPlanId = body.get("plan_id");
        if (rawPlanId instanceof Number n) {
            planId = n.intValue();
        } else if (rawPlanId != null && !String.valueOf(rawPlanId).isBlank()) {
            planId = Integer.parseInt(String.valueOf(rawPlanId));
        }
        String promoId = body.get("promo_id") != null ? String.valueOf(body.get("promo_id")) : "";
        return Result.success(h5Service.pay(deviceId, planId, promoId));
    }
}
