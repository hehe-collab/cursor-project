package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.VodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "阿里云VOD", description = "阿里云视频点播上传/播放凭证")
@RestController
@RequestMapping("/api/vod")
@RequiredArgsConstructor
public class VodController {

    private final VodService vodService;

    @Operation(summary = "获取上传凭证", description = "获取阿里云VOD视频上传凭证")
    @GetMapping("/upload-auth")
    public Result<Map<String, Object>> uploadAuth(
            @Parameter(description = "视频标题") @RequestParam String title,
            @Parameter(description = "文件名") @RequestParam String fileName) {
        return Result.success(vodService.createUploadAuth(title, fileName));
    }

    @Operation(summary = "获取上传凭证", description = "POST 方式获取阿里云 VOD 视频上传凭证")
    @PostMapping("/upload-auth")
    public Result<Map<String, Object>> uploadAuthPost(@RequestBody Map<String, Object> body) {
        return Result.success(
                vodService.createUploadAuth(
                        String.valueOf(body.getOrDefault("title", "")),
                        String.valueOf(body.getOrDefault("fileName", ""))));
    }

    @Operation(summary = "刷新上传凭证", description = "刷新阿里云VOD视频上传凭证")
    @GetMapping("/refresh-upload-auth")
    public Result<Map<String, Object>> refreshUploadAuth(@Parameter(description = "视频ID") @RequestParam String videoId) {
        return Result.success(vodService.refreshUploadAuth(videoId));
    }

    @Operation(summary = "获取 VOD 公共配置", description = "返回前端可安全使用的 VOD 公共配置")
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", vodService.isConfigured());
        data.put("regionId", vodService.getRegionId());
        return Result.success(data);
    }

    @Operation(summary = "获取播放凭证", description = "获取阿里云VOD视频播放凭证")
    @GetMapping("/play-auth/{videoId}")
    public Result<Map<String, Object>> playAuth(@PathVariable("videoId") String videoId) {
        return Result.success(vodService.getPlayAuth(videoId));
    }

    @Operation(summary = "获取播放地址", description = "获取阿里云 VOD 可播放地址，优先返回 HLS")
    @GetMapping("/play-url/{videoId}")
    public Result<Map<String, Object>> playUrl(@PathVariable("videoId") String videoId) {
        return Result.success(vodService.getPlayUrl(videoId));
    }

    @Operation(summary = "获取视频信息", description = "获取阿里云 VOD 视频状态、时长、封面等信息")
    @GetMapping("/info/{videoId}")
    public Result<Map<String, Object>> info(@PathVariable("videoId") String videoId) {
        return Result.success(vodService.getVideoInfo(videoId));
    }
}
