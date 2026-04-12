package com.drama.controller;

import com.drama.annotation.LogOperation;
import com.drama.common.Result;
import com.drama.service.CategoryService;
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

@Tag(name = "分类管理", description = "短剧分类的 CRUD 操作")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取分类统计", description = "获取分类的数量统计信息")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(categoryService.stats());
    }

    /**
     * 无分页参数时 data 为数组（兼容短剧页下拉）；有 page 时 data 为 { list, total, page, pageSize }。
     */
    @Operation(summary = "获取分类列表", description = "获取分类列表，支持筛选和分页")
    @GetMapping
    public Result<?> list(
            @Parameter(description = "分类名称") @RequestParam(required = false) String name,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean isEnabled,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize) {
        if (page == null && pageSize == null) {
            List<Map<String, Object>> rows = categoryService.listAll(name, isEnabled);
            return Result.success(rows);
        }
        int p = page != null ? page : 1;
        int ps = pageSize != null ? pageSize : 20;
        return Result.success(categoryService.listPage(name, isEnabled, p, ps));
    }

    @Operation(summary = "获取分类详情", description = "根据ID获取分类详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable int id) {
        return Result.success(categoryService.getById(id));
    }

    @Operation(summary = "创建分类", description = "创建一个新的分类")
    @PostMapping
    @LogOperation(type = "CREATE", desc = "创建分类", targetType = "category")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.success("创建成功", categoryService.create(body));
    }

    @Operation(summary = "更新分类", description = "更新指定分类的信息")
    @PutMapping("/{id:\\d+}")
    @LogOperation(type = "UPDATE", desc = "更新分类", targetType = "category")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        categoryService.update(id, body);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除分类", description = "删除指定的分类")
    @DeleteMapping("/{id:\\d+}")
    @LogOperation(type = "DELETE", desc = "删除分类", targetType = "category")
    public Result<Void> delete(@PathVariable int id) {
        categoryService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 批量更新排序
     */
    @Operation(summary = "批量更新分类排序", description = "批量更新多个分类的排序顺序")
    @PostMapping("/batch-sort")
    public Result<Void> batchSort(@RequestBody Map<String, List<Integer>> payload) {
        List<Integer> ids = payload.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("ids 不能为空");
        }
        categoryService.batchUpdateSort(ids);
        return Result.success("排序已更新", null);
    }
}
