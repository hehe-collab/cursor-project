package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.Admin;
import com.drama.mapper.AdminMapper;
import com.drama.service.TitlePackService;
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
@RequestMapping("/api/title-pack")
@RequiredArgsConstructor
public class TitlePackController {

    private final TitlePackService titlePackService;
    private final AdminMapper adminMapper;

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String titlePackId, @RequestParam(required = false) String title)
            throws java.io.IOException {
        byte[] bytes = titlePackService.exportExcel(titlePackId, title);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"title-packs.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String titlePackId,
            @RequestParam(required = false) String title) {
        int ps = Math.min(Math.max(pageSize, 1), 1000);
        int p = Math.max(page, 1);
        return Result.success(titlePackService.listPage(p, ps, titlePackId, title));
    }

    @PostMapping
    public Result<Map<String, Integer>> create(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        String[] nc = normalizeNameContent(body);
        int id =
                titlePackService.create(
                        nc[0], nc[1], adminId, creatorName(adminId));
        return Result.success("新增成功", new LinkedHashMap<>(Map.of("id", id)));
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        String[] nc = normalizeNameContent(body);
        titlePackService.update(id, nc[0], nc[1]);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("ids");
        if (raw == null) {
            return Result.error("请选择要删除的记录");
        }
        List<Integer> ids = raw.stream()
                .map(o -> o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(String.valueOf(o)))
                .toList();
        titlePackService.deleteBatch(ids);
        return Result.success("批量删除成功", null);
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        titlePackService.deleteOne(id);
        return Result.success("删除成功", null);
    }

    private static String[] normalizeNameContent(Map<String, Object> body) {
        Object nameObj = body.get("name");
        if (nameObj == null || String.valueOf(nameObj).trim().isEmpty()) {
            nameObj = body.get("title");
        }
        Object contentObj = body.get("content");
        if (contentObj == null) {
            contentObj = body.get("group");
        }
        String name = nameObj != null ? String.valueOf(nameObj).trim() : "";
        String content = contentObj != null ? String.valueOf(contentObj) : "";
        return new String[] {name, content};
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
