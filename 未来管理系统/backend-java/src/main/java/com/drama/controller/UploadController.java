package com.drama.controller;

import com.drama.annotation.RateLimit;
import com.drama.common.Result;
import com.drama.util.FileUploadUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "图片、视频、文档上传接口")
public class UploadController {

    private final FileUploadUtil fileUploadUtil;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Operation(summary = "上传图片", description = "上传 JPG/PNG/GIF/WebP 格式图片，最大 5MB")
    @PostMapping("/image")
    @RateLimit(key = "upload:image", max = 10, timeout = 60, limitType = RateLimit.LimitType.USER)
    public Result<?> uploadImage(
            @Parameter(description = "图片文件") @RequestParam("file") MultipartFile file) {
        try {
            fileUploadUtil.validateImage(file);
            String filename = fileUploadUtil.saveFile(file, uploadDir + "/images");
            String url = "/api/upload/files/images/" + filename;
            Map<String, Object> data = new HashMap<>();
            data.put("url", url);
            data.put("filename", filename);
            data.put("originalName", file.getOriginalFilename());
            data.put("size", file.getSize());
            log.info("✅ 图片上传成功: filename={}, size={}", filename, file.getSize());
            return Result.success("上传成功", data);
        } catch (SecurityException e) {
            log.warn("❌ 图片上传失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("❌ 图片保存失败", e);
            return Result.error("文件保存失败");
        }
    }

    @Operation(summary = "上传视频", description = "上传 MP4/MOV/AVI 格式视频，最大 100MB")
    @PostMapping("/video")
    @RateLimit(key = "upload:video", max = 5, timeout = 60, limitType = RateLimit.LimitType.USER)
    public Result<?> uploadVideo(
            @Parameter(description = "视频文件") @RequestParam("file") MultipartFile file) {
        try {
            fileUploadUtil.validateVideo(file);
            String filename = fileUploadUtil.saveFile(file, uploadDir + "/videos");
            String url = "/api/upload/files/videos/" + filename;
            Map<String, Object> data = new HashMap<>();
            data.put("url", url);
            data.put("filename", filename);
            data.put("originalName", file.getOriginalFilename());
            data.put("size", file.getSize());
            log.info("✅ 视频上传成功: filename={}, size={}", filename, file.getSize());
            return Result.success("上传成功", data);
        } catch (SecurityException e) {
            log.warn("❌ 视频上传失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("❌ 视频保存失败", e);
            return Result.error("文件保存失败");
        }
    }

    @Operation(summary = "上传文档", description = "上传 PDF/Excel/Word 文档，最大 10MB")
    @PostMapping("/document")
    @RateLimit(key = "upload:document", max = 5, timeout = 60, limitType = RateLimit.LimitType.USER)
    public Result<?> uploadDocument(
            @Parameter(description = "文档文件") @RequestParam("file") MultipartFile file) {
        try {
            fileUploadUtil.validateDocument(file);
            String filename = fileUploadUtil.saveFile(file, uploadDir + "/documents");
            Map<String, Object> data = new HashMap<>();
            data.put("filename", filename);
            data.put("originalName", file.getOriginalFilename());
            data.put("size", file.getSize());
            log.info("✅ 文档上传成功: filename={}, size={}", filename, file.getSize());
            return Result.success("上传成功", data);
        } catch (SecurityException e) {
            log.warn("❌ 文档上传失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("❌ 文档保存失败", e);
            return Result.error("文件保存失败");
        }
    }

    @Operation(summary = "批量上传", description = "批量上传多个图片，单次最多 10 个")
    @PostMapping("/batch")
    @RateLimit(key = "upload:batch", max = 5, timeout = 3600, limitType = RateLimit.LimitType.USER)
    public Result<?> batchUpload(
            @Parameter(description = "文件列表") @RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.error("请选择要上传的文件");
        }
        if (files.length > 10) {
            return Result.error("单次最多上传 10 个文件");
        }
        int successCount = 0;
        int failCount = 0;
        for (MultipartFile file : files) {
            try {
                fileUploadUtil.validateImage(file);
                fileUploadUtil.saveFile(file, uploadDir + "/images");
                successCount++;
            } catch (Exception e) {
                log.warn("文件上传失败: {}", file.getOriginalFilename(), e);
                failCount++;
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("total", files.length);
        data.put("success", successCount);
        data.put("fail", failCount);
        log.info("✅ 批量上传完成: total={}, success={}, fail={}", files.length, successCount, failCount);
        return Result.success("批量上传完成", data);
    }

    // ========== 静态文件访问 ==========

    @Operation(summary = "获取上传文件", description = "获取已上传的静态文件")
    @GetMapping("/files/{filename:.+}")
    @RateLimit(key = "uploads:file", max = 60, timeout = 60, limitType = RateLimit.LimitType.IP)
    public ResponseEntity<Resource> getFile(
            @Parameter(description = "文件名") @PathVariable String filename) {
        // 文件名安全校验：仅允许字母、数字、点、下划线、连字符
        if (!filename.matches("^[a-zA-Z0-9._-]+$")) {
            return ResponseEntity.notFound().build();
        }
        // 路径安全：限制在 uploadDir 目录内，防止路径遍历
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = dir.resolve(filename).normalize();
        if (!filePath.startsWith(dir) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }
        try {
            String probe = Files.probeContentType(filePath);
            MediaType mediaType = probe != null
                    ? MediaType.parseMediaType(probe)
                    : MediaType.APPLICATION_OCTET_STREAM;
            FileSystemResource resource = new FileSystemResource(filePath.toFile());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(mediaType)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
