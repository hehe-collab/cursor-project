package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.PromotionLinkService;
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
@RequestMapping("/api/delivery-links")
@RequiredArgsConstructor
public class PromotionLinkController {

    private final PromotionLinkService promotionLinkService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String promo_id,
            @RequestParam(required = false) String promoteId,
            @RequestParam(required = false) String drama_id,
            @RequestParam(required = false) String dramaId,
            @RequestParam(required = false) String media,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String promo_name,
            @RequestParam(required = false) String promoteName,
            @RequestParam(required = false) String promo_domain,
            @RequestParam(required = false) String domain)
            throws java.io.IOException {
        String pid = promoteId != null && !promoteId.isBlank() ? promoteId : promo_id;
        String did = dramaId != null && !dramaId.isBlank() ? dramaId : drama_id;
        String pname = promoteName != null && !promoteName.isBlank() ? promoteName : promo_name;
        String dom = domain != null && !domain.isBlank() ? domain : promo_domain;
        byte[] bytes = promotionLinkService.exportExcel(pid, did, media, country, pname, dom);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"delivery_links.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("ids");
        if (raw == null) {
            return Result.error(400, "请提供要删除的ID列表");
        }
        List<Integer> ids = raw.stream()
                .map(o -> o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(String.valueOf(o)))
                .toList();
        promotionLinkService.batchDelete(ids);
        return Result.success("删除成功", null);
    }

    @PostMapping("/batch-status")
    public Result<Void> batchStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("ids");
        Object en = body.get("enabled");
        if (raw == null || en == null) {
            return Result.error(400, "参数不完整");
        }
        boolean enabled = en instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(en));
        List<Integer> ids = raw.stream()
                .map(o -> o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(String.valueOf(o)))
                .toList();
        promotionLinkService.batchStatus(ids, enabled);
        return Result.success("更新成功", null);
    }

    @PostMapping("/copy")
    public Result<Map<String, Object>> copy(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        Object sid = body.get("source_id");
        int sourceId =
                sid instanceof Number ? ((Number) sid).intValue() : Integer.parseInt(String.valueOf(sid));
        String newName = body.get("new_name") != null ? String.valueOf(body.get("new_name")) : "";
        Map<String, Object> data = promotionLinkService.copy(sourceId, newName, adminId);
        return Result.success("复制成功", data);
    }

    /** 关键词搜索推广链接，供回传配置等远程下拉（须排在 /{id} 之前由 Spring 匹配）。 */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "30") int limit) {
        return Result.success(promotionLinkService.searchOptions(keyword, limit));
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String promo_id,
            @RequestParam(required = false) String promoteId,
            @RequestParam(required = false) String drama_id,
            @RequestParam(required = false) String dramaId,
            @RequestParam(required = false) String media,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String promo_name,
            @RequestParam(required = false) String promoteName,
            @RequestParam(required = false) String promo_domain,
            @RequestParam(required = false) String domain) {
        String pid = promoteId != null && !promoteId.isBlank() ? promoteId : promo_id;
        String did = dramaId != null && !dramaId.isBlank() ? dramaId : drama_id;
        String pname = promoteName != null && !promoteName.isBlank() ? promoteName : promo_name;
        String dom = domain != null && !domain.isBlank() ? domain : promo_domain;
        int ps = Math.min(Math.max(pageSize, 1), 1000);
        int p = Math.max(page, 1);
        return Result.success(promotionLinkService.listPage(p, ps, pid, did, media, country, pname, dom));
    }

    @PostMapping
    public Result<Map<String, Object>> create(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        Map<String, Object> data = promotionLinkService.create(body, adminId);
        return Result.success("新增成功", data);
    }

    @PutMapping("/{id:\\d+}")
    public Result<Map<String, Object>> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        return Result.success("修改成功", promotionLinkService.update(id, body));
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        promotionLinkService.deleteOne(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> one(@PathVariable int id) {
        return Result.success(promotionLinkService.getById(id));
    }
}
