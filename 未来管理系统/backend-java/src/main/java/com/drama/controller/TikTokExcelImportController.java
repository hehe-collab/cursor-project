package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.Admin;
import com.drama.entity.TikTokExcelImport;
import com.drama.mapper.AdminMapper;
import com.drama.service.TikTokExcelImportService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** TikTok Excel 导入（{@code /api/tiktok/excel-imports}，需 Bearer） */
@Slf4j
@RestController
@RequestMapping("/api/tiktok/excel-imports")
@RequiredArgsConstructor
public class TikTokExcelImportController {

    private final TikTokExcelImportService excelImportService;
    private final AdminMapper adminMapper;

    @GetMapping
    public Result<Map<String, Object>> getImports(
            @RequestParam(required = false) String advertiserId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            List<TikTokExcelImport> imports = excelImportService.getImports(advertiserId, status, page, pageSize);
            Map<String, Object> data = new HashMap<>();
            data.put("list", imports);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("total", imports.size());
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get imports: {}", e.getMessage(), e);
            return Result.error("获取导入记录列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id:\\d+}")
    public Result<TikTokExcelImport> getImportById(@PathVariable Long id) {
        try {
            TikTokExcelImport row = excelImportService.getImportById(id);
            return Result.success(row);
        } catch (Exception e) {
            log.error("Failed to get import {}: {}", id, e.getMessage(), e);
            return Result.error("获取导入记录失败: " + e.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<TikTokExcelImport> uploadAndProcess(
            @RequestParam String advertiserId,
            @RequestParam String importType,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                return Result.error("只支持 .xlsx 格式的文件");
            }
            if (!importType.matches("campaigns|adgroups|ads")) {
                return Result.error("不支持的导入类型: " + importType);
            }
            String createdBy = resolveCreator(adminId);
            TikTokExcelImport result =
                    excelImportService.uploadAndProcess(advertiserId, importType, file, createdBy);
            return Result.success(result);
        } catch (IOException e) {
            log.error("Failed to upload and process excel: {}", e.getMessage(), e);
            return Result.error("上传并处理 Excel 失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to upload and process excel: {}", e.getMessage(), e);
            return Result.error("上传并处理 Excel 失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> deleteImport(@PathVariable Long id) {
        try {
            excelImportService.deleteImport(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to delete import {}: {}", id, e.getMessage(), e);
            return Result.error("删除导入记录失败: " + e.getMessage());
        }
    }

    /** 模板需置于 {@code classpath:templates/}；未放置时返回 404。 */
    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate(@RequestParam String importType) {
        try {
            String templatePath =
                    switch (importType) {
                        case "campaigns" -> "templates/tiktok_campaign_template.xlsx";
                        case "adgroups" -> "templates/tiktok_adgroup_template.xlsx";
                        case "ads" -> "templates/tiktok_ad_template.xlsx";
                        default -> throw new IllegalArgumentException("不支持的导入类型: " + importType);
                    };
            Resource resource = new ClassPathResource(templatePath);
            if (!resource.exists()) {
                log.warn("Template not found: {}", templatePath);
                return ResponseEntity.notFound().build();
            }
            String outName = resource.getFilename() != null ? resource.getFilename() : "template.xlsx";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outName + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to download template: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String resolveCreator(Integer adminId) {
        if (adminId == null) {
            return "system";
        }
        Admin a = adminMapper.selectById(adminId);
        if (a == null) {
            return "system";
        }
        if (a.getUsername() != null && !a.getUsername().isBlank()) {
            return a.getUsername();
        }
        if (a.getNickname() != null && !a.getNickname().isBlank()) {
            return a.getNickname();
        }
        return String.valueOf(adminId);
    }
}
