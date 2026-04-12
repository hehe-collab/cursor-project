package com.drama.controller;

import com.drama.annotation.LogOperation;
import com.drama.annotation.RateLimit;
import com.drama.common.Result;
import com.drama.service.DramaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "短剧管理", description = "短剧、剧集、分类、标签相关接口")
public class DramaController {

    private final DramaService dramaService;

    @Operation(summary = "获取短剧统计", description = "按条件统计短剧数量")
    @GetMapping("/stats")
    @RateLimit(key = "drama:stats", max = 30, timeout = 60, limitType = RateLimit.LimitType.USER)
    public Result<Map<String, Object>> stats(
            @Parameter(description = "短剧标题（模糊搜索）") @RequestParam(required = false) String title,
            @Parameter(description = "分类ID") @RequestParam(required = false) Integer category_id,
            @Parameter(description = "短剧ID") @RequestParam(required = false) Integer id,
            @Parameter(description = "业务侧15位剧ID") @RequestParam(required = false) String public_id,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        String t = title != null && !title.isBlank() ? title.trim() : null;
        String pub = public_id != null && !public_id.isBlank() ? public_id.trim() : null;
        return Result.success(dramaService.stats(t, category_id, status, id, pub));
    }

    @Operation(summary = "获取短剧列表", description = "分页查询短剧列表，支持按标题、分类、状态筛选")
    @GetMapping
    public Result<Map<String, Object>> list(
            @Parameter(description = "短剧标题（模糊搜索）") @RequestParam(required = false) String title,
            @Parameter(description = "分类ID") @RequestParam(required = false) Integer category_id,
            @Parameter(description = "短剧ID") @RequestParam(required = false) Integer id,
            @Parameter(description = "业务侧15位剧ID") @RequestParam(required = false) String public_id,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
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
    @LogOperation(type = "CREATE", desc = "创建短剧分集", targetType = "drama")
    public Result<Map<String, Object>> createEpisode(
            @PathVariable int id, @RequestBody Map<String, Object> body) {
        return Result.success("创建成功", dramaService.createEpisode(id, body));
    }

    @PutMapping("/{id:\\d+}/episodes/{episodeId:\\d+}")
    @LogOperation(type = "UPDATE", desc = "更新短剧分集", targetType = "drama")
    public Result<Void> updateEpisode(
            @PathVariable int id,
            @PathVariable int episodeId,
            @RequestBody Map<String, Object> body) {
        dramaService.updateEpisode(id, episodeId, body);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id:\\d+}/episodes/{episodeId:\\d+}")
    @LogOperation(type = "DELETE", desc = "删除短剧分集", targetType = "drama")
    public Result<Void> deleteEpisode(@PathVariable int id, @PathVariable int episodeId) {
        dramaService.deleteEpisode(id, episodeId);
        return Result.success("删除成功", null);
    }

    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable int id) {
        return Result.success(dramaService.getById(id));
    }

    @PostMapping
    @LogOperation(type = "CREATE", desc = "创建短剧", targetType = "drama")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.success("创建成功", dramaService.create(body));
    }

    @PutMapping("/{id:\\d+}")
    @LogOperation(type = "UPDATE", desc = "更新短剧", targetType = "drama")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        dramaService.update(id, body);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id:\\d+}")
    @LogOperation(type = "DELETE", desc = "删除短剧", targetType = "drama")
    public Result<Void> delete(@PathVariable int id) {
        dramaService.delete(id);
        return Result.success("删除成功", null);
    }
}
