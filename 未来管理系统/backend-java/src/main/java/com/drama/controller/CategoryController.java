package com.drama.controller;

import com.drama.common.Result;
import com.drama.service.CategoryService;
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

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(categoryService.stats());
    }

    /**
     * 无分页参数时 {@code data} 为数组（兼容短剧页下拉）；有 {@code page} 时 {@code data} 为
     * {@code { list, total, page, pageSize }}。
     */
    @GetMapping
    public Result<?> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        if (page == null && pageSize == null) {
            List<Map<String, Object>> rows = categoryService.listAll(name);
            return Result.success(rows);
        }
        int p = page != null ? page : 1;
        int ps = pageSize != null ? pageSize : 20;
        return Result.success(categoryService.listPage(name, p, ps));
    }

    @GetMapping("/{id:\\d+}")
    public Result<Map<String, Object>> detail(@PathVariable int id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.success("创建成功", categoryService.create(body));
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        categoryService.update(id, body);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable int id) {
        categoryService.delete(id);
        return Result.success("删除成功", null);
    }
}
