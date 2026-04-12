package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.Admin;
import com.drama.mapper.AdminMapper;
import com.drama.service.AdAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@Tag(name = "广告账户", description = "广告账户的 CRUD 与导出")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AdAccountController {

    private final AdAccountService adAccountService;
    private final AdminMapper adminMapper;

    @Operation(summary = "获取广告实体列表", description = "获取所有广告实体选项")
    @GetMapping("/entities")
    public Result<List<Map<String, String>>> entities() {
        return Result.success(adAccountService.entitiesOptions());
    }

    @Operation(summary = "获取国家列表", description = "获取所有可选国家列表")
    @GetMapping("/countries")
    public Result<List<String>> countries() {
        return Result.success("获取成功", adAccountService.countries());
    }

    @Operation(summary = "导出广告账户", description = "导出广告账户到 Excel 文件")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "媒体平台") @RequestParam(required = false) String media,
            @Parameter(description = "平台") @RequestParam(required = false) String platform,
            @Parameter(description = "平台别名") @RequestParam(required = false) String platformAlias,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "主体") @RequestParam(required = false) String subject,
            @Parameter(description = "实体名称") @RequestParam(required = false) String entityName,
            @Parameter(description = "账户ID") @RequestParam(required = false) String accountId,
            @Parameter(description = "SPID") @RequestParam(required = false) String spid,
            @Parameter(description = "账户名称") @RequestParam(required = false) String accountName,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword)
            throws java.io.IOException {
        Map<String, String> f = buildFilter(media, platform, platformAlias, country, subject, entityName, accountId, spid, accountName, keyword);
        byte[] bytes = adAccountService.exportExcel(f);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"accounts.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @Operation(summary = "批量删除广告账户", description = "批量删除多条广告账户")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("ids");
        if (raw == null) {
            return Result.error("请选择要删除的记录");
        }
        List<Integer> ids = raw.stream()
                .map(o -> o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(String.valueOf(o)))
                .toList();
        adAccountService.deleteBatch(ids);
        return Result.success("批量删除成功", null);
    }

    @Operation(summary = "获取广告账户列表", description = "获取广告账户列表，支持筛选和分页")
    @GetMapping
    public Result<Map<String, Object>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "媒体平台") @RequestParam(required = false) String media,
            @Parameter(description = "平台") @RequestParam(required = false) String platform,
            @Parameter(description = "平台别名") @RequestParam(required = false) String platformAlias,
            @Parameter(description = "国家") @RequestParam(required = false) String country,
            @Parameter(description = "主体") @RequestParam(required = false) String subject,
            @Parameter(description = "实体名称") @RequestParam(required = false) String entityName,
            @Parameter(description = "账户ID") @RequestParam(required = false) String accountId,
            @Parameter(description = "SPID") @RequestParam(required = false) String spid,
            @Parameter(description = "账户名称") @RequestParam(required = false) String accountName,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        int ps = Math.min(Math.max(pageSize, 1), 1000);
        int p = Math.max(page, 1);
        Map<String, String> f = buildFilter(media, platform, platformAlias, country, subject, entityName, accountId, spid, accountName, keyword);
        return Result.success(adAccountService.listPage(p, ps, f));
    }

    @Operation(summary = "创建广告账户", description = "创建一条新的广告账户")
    @PostMapping
    public Result<Map<String, Integer>> create(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        int id = adAccountService.create(body, adminId, creatorName(adminId));
        return Result.success("新增成功", new LinkedHashMap<>(Map.of("id", id)));
    }

    @Operation(summary = "更新广告账户", description = "更新指定广告账户的信息")
    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        adAccountService.update(id, body);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除广告账户", description = "删除指定的广告账户")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        adAccountService.deleteOne(id);
        return Result.success("删除成功", null);
    }

    private static Map<String, String> buildFilter(
            String media,
            String platform,
            String platformAlias,
            String country,
            String subject,
            String entityName,
            String accountId,
            String spid,
            String accountName,
            String keyword) {
        Map<String, String> f = new LinkedHashMap<>();
        if (media != null) f.put("media", media);
        if (platform != null) f.put("platform", platform);
        if (platformAlias != null) f.put("platformAlias", platformAlias);
        if (country != null) f.put("country", country);
        if (subject != null) f.put("subject", subject);
        if (entityName != null) f.put("entityName", entityName);
        if (accountId != null) f.put("accountId", accountId);
        if (spid != null) f.put("spid", spid);
        if (accountName != null) f.put("accountName", accountName);
        if (keyword != null) f.put("keyword", keyword);
        return f;
    }

    private String creatorName(Integer adminId) {
        if (adminId == null) {
            return "";
        }
        Admin a = adminMapper.selectById(adminId);
        if (a == null) {
            return "";
        }
        if (a.getNickname() != null && !a.getNickname().isBlank()) {
            return a.getNickname().trim();
        }
        if (a.getUsername() != null) {
            return a.getUsername();
        }
        return "";
    }
}
