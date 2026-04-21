package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.OssService;
import com.drama.service.OssService.ParsedOssPath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "阿里云OSS", description = "OSS 文件夹扫描（用于「OSS 导入」入口）")
@RestController
@RequestMapping("/api/oss")
@RequiredArgsConstructor
public class OssController {

    private final OssService ossService;

    @Operation(summary = "OSS 配置概览", description = "查询当前 OSS 配置是否就绪、默认 bucket 名等")
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configured", ossService.isConfigured());
        data.put("bucket", ossService.getDefaultBucket());
        data.put("endpoint", ossService.getEndpoint());
        data.put("region", ossService.getRegion());
        data.put("bucketPrivate", ossService.isBucketPrivate());
        data.put("presignExpireSec", ossService.getPresignExpireSec());
        return Result.success(data);
    }

    @Operation(
            summary = "扫描 OSS 文件夹下的视频",
            description =
                    "支持智能路径：oss://bucket/path/ 跨 bucket；path/ 或 /path/ 用默认 bucket；"
                            + "只扫一层（不递归子文件夹）；只返回视频后缀文件，按文件名排序。")
    @PostMapping("/scan-videos")
    public Result<Map<String, Object>> scanVideos(@RequestBody Map<String, Object> body) {
        String ossPath = String.valueOf(body.getOrDefault("ossPath", ""));
        ParsedOssPath parsed = ossService.parsePath(ossPath);
        List<Map<String, Object>> files = ossService.listVideosOneLevel(parsed.bucket, parsed.prefix);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bucket", parsed.bucket);
        data.put("prefix", parsed.prefix);
        data.put("count", files.size());
        data.put("files", files);
        return Result.success(data);
    }
}
