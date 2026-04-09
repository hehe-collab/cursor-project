package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.TikTokPixel;
import com.drama.service.TikTokPixelService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/tiktok/pixels")
@RequiredArgsConstructor
public class TikTokPixelController {

    private final TikTokPixelService pixelService;

    @GetMapping
    public Result<List<TikTokPixel>> getPixels(@RequestParam(required = false) String advertiserId) {
        try {
            return Result.success(pixelService.getPixels(advertiserId));
        } catch (Exception e) {
            log.error("Failed to get pixels: {}", e.getMessage(), e);
            return Result.error("获取 Pixel 列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id:\\d+}")
    public Result<TikTokPixel> getPixelById(@PathVariable Long id) {
        try {
            return Result.success(pixelService.getPixelById(id));
        } catch (Exception e) {
            log.error("Failed to get pixel {}: {}", id, e.getMessage(), e);
            return Result.error("获取 Pixel 失败: " + e.getMessage());
        }
    }

    @GetMapping("/pixel/{pixelId}")
    public Result<TikTokPixel> getPixelByPixelId(@PathVariable String pixelId) {
        try {
            return Result.success(pixelService.getPixelByPixelId(pixelId));
        } catch (Exception e) {
            log.error("Failed to get pixel by pixel_id {}: {}", pixelId, e.getMessage(), e);
            return Result.error("获取 Pixel 失败: " + e.getMessage());
        }
    }

    @PostMapping("/sync")
    public Result<List<TikTokPixel>> syncPixels(@RequestBody Map<String, String> params) {
        try {
            String advertiserId = params.get("advertiser_id");
            if (advertiserId == null || advertiserId.isEmpty()) {
                return Result.error("advertiser_id 不能为空");
            }
            return Result.success(pixelService.syncPixelsFromTikTok(advertiserId));
        } catch (Exception e) {
            log.error("Failed to sync pixels: {}", e.getMessage(), e);
            return Result.error("同步 Pixel 失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<TikTokPixel> createPixel(@RequestBody TikTokPixel pixel) {
        try {
            return Result.success(pixelService.createPixel(pixel));
        } catch (Exception e) {
            log.error("Failed to create pixel: {}", e.getMessage(), e);
            return Result.error("创建 Pixel 失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id:\\d+}")
    public Result<TikTokPixel> updatePixel(@PathVariable Long id, @RequestBody TikTokPixel pixel) {
        try {
            return Result.success(pixelService.updatePixel(id, pixel));
        } catch (Exception e) {
            log.error("Failed to update pixel {}: {}", id, e.getMessage(), e);
            return Result.error("更新 Pixel 失败: " + e.getMessage());
        }
    }

    /**
     * 按 TikTok {@code pixel_id} 删除，与 {@link #getPixelByPixelId(String)} 路径风格一致，避免与 {@code
     * GET /{id}} 主键语义混淆。
     */
    @DeleteMapping("/pixel/{pixelId}")
    public Result<Void> deletePixelByPixelId(@PathVariable String pixelId) {
        try {
            pixelService.deletePixelByPixelId(pixelId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to delete pixel {}: {}", pixelId, e.getMessage(), e);
            return Result.error("删除 Pixel 失败: " + e.getMessage());
        }
    }
}
