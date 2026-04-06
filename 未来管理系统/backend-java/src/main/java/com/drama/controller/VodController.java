package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.VodService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vod")
@RequiredArgsConstructor
public class VodController {

    private final VodService vodService;

    @GetMapping("/upload-auth")
    public Result<Map<String, Object>> uploadAuth(
            @RequestParam String title, @RequestParam String fileName) {
        return Result.success(vodService.createUploadAuth(title, fileName));
    }

    @GetMapping("/refresh-upload-auth")
    public Result<Map<String, Object>> refreshUploadAuth(@RequestParam String videoId) {
        return Result.success(vodService.refreshUploadAuth(videoId));
    }

    @GetMapping("/play-auth/{videoId}")
    public Result<Map<String, Object>> playAuth(@PathVariable("videoId") String videoId) {
        return Result.success(vodService.getPlayAuth(videoId));
    }
}
