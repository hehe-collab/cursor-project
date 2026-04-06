package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.DramaService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dramas")
@RequiredArgsConstructor
public class DramaController {

    private final DramaService dramaService;

    /** 须在 `/{id}` 之前 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer category_id,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String public_id,
            @RequestParam(required = false) String status) {
        String t = title != null && !title.isBlank() ? title.trim() : null;
        String pub = public_id != null && !public_id.isBlank() ? public_id.trim() : null;
        return Result.success(dramaService.stats(t, category_id, status, id, pub));
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer category_id,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String public_id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        String t = title != null && !title.isBlank() ? title.trim() : null;
        String st = status != null && !status.isBlank() ? status.trim() : null;
        String pub = public_id != null && !public_id.isBlank() ? public_id.trim() : null;
        return Result.success(dramaService.list(t, category_id, st, id, pub, page, pageSize));
    }

    @GetMapping("/{id:\\d+}/episodes")
    public Result<Map<String, Object>> listEpisodes(@PathVariable int id) {
        return Result.success(dramaService.listEpisodes(id));
    }

    @PostMapping("/{id:\\d+}/episodes")
    public Result<Map<String, Object>> createEpisode(
            @PathVariable int id, @RequestBody Map<String, Object> body) {
        return Result.success("创建成功", dramaService.createEpisode(id, body));
    }

    @PutMapping("/{id:\\d+}/episodes/{episodeId:\\d+}")
    public Result<Void> updateEpisode(
            @PathVariable int id,
            @PathVariable int episodeId,
            @RequestBody Map<String, Object> body) {
        dramaService.updateEpisode(id, episodeId, body);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id:\\d+}/episodes/{episodeId:\\d+}")
    public Result<Void> deleteEpisode(@PathVariable int id, @PathVariable int episodeId) {
        dramaService.deleteEpisode(id, episodeId);
        return Result.success("删除成功", null);
    }

    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable int id) {
        return Result.success(dramaService.getById(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.success("创建成功", dramaService.create(body));
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        dramaService.update(id, body);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        dramaService.delete(id);
        return Result.success("删除成功", null);
    }
}
