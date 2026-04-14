package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.AdMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
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
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "广告素材", description = "广告素材上传与记录管理")
@RestController
@RequestMapping("/api/ad-material")
@RequiredArgsConstructor
public class AdMaterialController {

    private final AdMaterialService adMaterialService;

    @Operation(summary = "上传文件", description = "上传广告素材文件")
    @PostMapping("/upload-file")
    public Result<Map<String, String>> uploadFile(@Parameter(description = "文件") @RequestParam("file") MultipartFile file)
            throws java.io.IOException {
        Map<String, String> data = adMaterialService.saveUploadedFile(file);
        return Result.success("上传成功", data);
    }

    @Operation(summary = "获取素材账户选项", description = "获取广告素材中已有的账户列表")
    @GetMapping("/account-options")
    public Result<java.util.List<Map<String, Object>>> accountOptions() {
        return Result.success(adMaterialService.accountOptions());
    }

    @Operation(summary = "提交上传任务", description = "提交广告素材上传任务")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        String accountId = body.get("accountId") != null ? String.valueOf(body.get("accountId")) : "";
        String folder = body.get("folder") != null ? String.valueOf(body.get("folder")) : "";
        String files = body.get("files") != null ? String.valueOf(body.get("files")) : "";
        Map<String, Object> summary = adMaterialService.uploadMaterials(accountId, folder, files, adminId);
        return Result.success("素材上传完成", summary);
    }

    @Operation(summary = "提交同步任务", description = "提交广告素材同步任务")
    @PostMapping("/sync")
    public Result<Void> sync(@RequestBody Map<String, Object> body) {
        Object raw = body.get("accountIds");
        if (raw == null) {
            raw = body.get("account_ids");
        }
        adMaterialService.logSyncTask(raw != null ? String.valueOf(raw) : "");
        return Result.success("同步任务已提交", null);
    }

    @Operation(summary = "获取上传记录", description = "获取广告素材上传记录")
    @GetMapping("/records")
    public Result<java.util.List<Map<String, Object>>> records() {
        return Result.success(adMaterialService.records());
    }

    @Operation(summary = "获取素材列表", description = "获取广告素材列表，支持筛选和分页")
    @GetMapping
    public Result<Map<String, Object>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "账户ID") @RequestParam(required = false) String accountId,
            @Parameter(description = "素材ID") @RequestParam(required = false) String materialId,
            @Parameter(description = "素材名称") @RequestParam(required = false) String materialName,
            @Parameter(description = "实体名称") @RequestParam(required = false) String entityName,
            @Parameter(description = "名称") @RequestParam(required = false) String name) {
        int ps = Math.min(Math.max(pageSize, 1), 1000);
        int p = Math.max(page, 1);
        Map<String, String> f = new LinkedHashMap<>();
        if (accountId != null) {
            f.put("accountId", accountId);
        }
        if (materialId != null) {
            f.put("materialId", materialId);
        }
        if (materialName != null) {
            f.put("materialName", materialName);
        }
        if (entityName != null) {
            f.put("entityName", entityName);
        }
        if (name != null) {
            f.put("name", name);
        }
        return Result.success(adMaterialService.listPage(p, ps, f));
    }

    @Operation(summary = "创建素材", description = "创建一条新的广告素材记录")
    @PostMapping
    public Result<Map<String, Integer>> create(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        int id = adMaterialService.create(body, adminId);
        return Result.success("新增成功", new java.util.LinkedHashMap<>(Map.of("id", id)));
    }

    @Operation(summary = "更新素材", description = "更新指定广告素材的信息")
    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        adMaterialService.update(id, body);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除素材", description = "删除指定的广告素材")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        adMaterialService.deleteOne(id);
        return Result.success("删除成功", null);
    }
}
