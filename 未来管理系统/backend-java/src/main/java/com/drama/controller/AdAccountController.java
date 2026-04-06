package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.Admin;
import com.drama.mapper.AdminMapper;
import com.drama.service.AdAccountService;
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

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AdAccountController {

    private final AdAccountService adAccountService;
    private final AdminMapper adminMapper;

    @GetMapping("/entities")
    public Result<List<Map<String, String>>> entities() {
        return Result.success(adAccountService.entitiesOptions());
    }

    @GetMapping("/countries")
    public Result<List<String>> countries() {
        return Result.success("获取成功", adAccountService.countries());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String media,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String platformAlias,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String spid,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String keyword)
            throws java.io.IOException {
        Map<String, String> f = buildFilter(media, platform, platformAlias, country, subject, entityName, accountId, spid, accountName, keyword);
        byte[] bytes = adAccountService.exportExcel(f);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"accounts.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

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

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String media,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String platformAlias,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String spid,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String keyword) {
        int ps = Math.min(Math.max(pageSize, 1), 1000);
        int p = Math.max(page, 1);
        Map<String, String> f = buildFilter(media, platform, platformAlias, country, subject, entityName, accountId, spid, accountName, keyword);
        return Result.success(adAccountService.listPage(p, ps, f));
    }

    @PostMapping
    public Result<Map<String, Integer>> create(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        int id = adAccountService.create(body, adminId, creatorName(adminId));
        return Result.success("新增成功", new LinkedHashMap<>(Map.of("id", id)));
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        adAccountService.update(id, body);
        return Result.success("修改成功", null);
    }

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
