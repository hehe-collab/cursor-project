package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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

@Tag(name = "标签管理", description = "短剧标签的 CRUD 操作")
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "获取标签统计", description = "获取标签的数量统计信息")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(tagService.stats());
    }

    /**
     * 无 page/pageSize 时 data 为数组（兼容短剧页下拉）；否则为 { list, total, page, pageSize }。
     */
    @Operation(summary = "获取标签列表", description = "获取标签列表，支持筛选和分页")
    @GetMapping
    public Result<?> list(
            @Parameter(description = "标签名称") @RequestParam(required = false) String name,
            @Parameter(description = "是否热门") @RequestParam(required = false) Boolean isHot,
            @Parameter(description = "排序字段") @RequestParam(required = false, defaultValue = "sort") String orderBy,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize) {
        if (page == null && pageSize == null) {
            List<Map<String, Object>> rows = tagService.listAll(name, isHot, orderBy);
            return Result.success(rows);
        }
        int p = page != null ? page : 1;
        int ps = pageSize != null ? pageSize : 20;
        return Result.success(tagService.listPage(name, isHot, p, ps));
    }

    @Operation(summary = "获取标签详情", description = "根据ID获取标签详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable int id) {
        return Result.success(tagService.getById(id));
    }

    @Operation(summary = "创建标签", description = "创建一个新的标签")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.success("创建成功", tagService.create(body));
    }

    @Operation(summary = "更新标签", description = "更新指定标签的信息")
    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        tagService.update(id, body);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除标签", description = "删除指定的标签")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        tagService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 批量设置热门标签
     */
    @Operation(summary = "批量设置热门标签", description = "批量设置多个标签的热门状态")
    @PostMapping("/batch-hot")
    public Result<Void> batchSetHot(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) payload.get("ids");
        Boolean isHot = (Boolean) payload.get("is_hot");
        if (ids == null || ids.isEmpty()) {
            return Result.error("ids 不能为空");
        }
        tagService.batchSetHot(ids, isHot != null && isHot);
        return Result.success("状态已更新", null);
    }
}
