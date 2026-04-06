package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.AdMaterialService;
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

@RestController
@RequestMapping("/api/ad-material")
@RequiredArgsConstructor
public class AdMaterialController {

    private final AdMaterialService adMaterialService;

    @PostMapping("/upload-file")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file)
            throws java.io.IOException {
        Map<String, String> data = adMaterialService.saveUploadedFile(file);
        return Result.success("上传成功", data);
    }

    @PostMapping("/upload")
    public Result<Void> upload(@RequestBody Map<String, Object> body) {
        String accountId = body.get("accountId") != null ? String.valueOf(body.get("accountId")) : "";
        String folder = body.get("folder") != null ? String.valueOf(body.get("folder")) : "";
        String files = body.get("files") != null ? String.valueOf(body.get("files")) : "";
        adMaterialService.logUploadTask(accountId, folder, files);
        return Result.success("上传任务已提交", null);
    }

    @PostMapping("/sync")
    public Result<Void> sync(@RequestBody Map<String, Object> body) {
        Object raw = body.get("accountIds");
        if (raw == null) {
            raw = body.get("account_ids");
        }
        adMaterialService.logSyncTask(raw != null ? String.valueOf(raw) : "");
        return Result.success("同步任务已提交", null);
    }

    @GetMapping("/records")
    public Result<java.util.List<Map<String, Object>>> records() {
        return Result.success(adMaterialService.records());
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String materialId,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String name) {
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

    @PostMapping
    public Result<Map<String, Integer>> create(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        int id = adMaterialService.create(body, adminId);
        return Result.success("新增成功", new java.util.LinkedHashMap<>(Map.of("id", id)));
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        adMaterialService.update(id, body);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        adMaterialService.deleteOne(id);
        return Result.success("删除成功", null);
    }
}
