package com.drama.service;

import com.drama.dto.CategoryStatsRow;
import com.drama.entity.Category;
import com.drama.exception.BusinessException;
import com.drama.mapper.CategoryMapper;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CategoryMapper categoryMapper;

    public Map<String, Object> stats() {
        CategoryStatsRow row = categoryMapper.selectStats();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total_count", nzLong(row != null ? row.getTotalCount() : null));
        m.put("dramas_linked_count", nzLong(row != null ? row.getDramasLinkedCount() : null));
        m.put("total_dramas", nzLong(row != null ? row.getTotalDramas() : null));
        m.put("total", m.get("total_count"));
        return m;
    }

    /** 全量或按名称筛选；用于下拉，返回列表（与历史接口一致） */
    public List<Map<String, Object>> listAll(String name) {
        if (name == null || name.isBlank()) {
            return categoryMapper.selectAll().stream().map(this::toApi).collect(Collectors.toList());
        }
        String n = name.trim();
        long total = categoryMapper.countByParam(n);
        if (total > 10_000) {
            return categoryMapper.selectByParam(n, 0, 10_000).stream().map(this::toApi).collect(Collectors.toList());
        }
        return categoryMapper.selectByParam(n, 0, (int) total).stream().map(this::toApi).collect(Collectors.toList());
    }

    public Map<String, Object> listPage(String name, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.min(100, Math.max(1, pageSize));
        int offset = (p - 1) * ps;
        String n = name != null && !name.isBlank() ? name.trim() : null;
        long total = categoryMapper.countByParam(n);
        List<Map<String, Object>> list =
                categoryMapper.selectByParam(n, offset, ps).stream().map(this::toApi).collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("page", p);
        data.put("pageSize", ps);
        data.put("list", list);
        return data;
    }

    public Map<String, Object> getById(int id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(404, "分类不存在");
        }
        return toApi(c);
    }

    public Map<String, Object> create(Map<String, Object> body) {
        String name = str(body.get("name"));
        if (name.isBlank()) {
            throw new BusinessException(400, "分类名称不能为空");
        }
        Category row = new Category();
        row.setName(name.trim());
        row.setSort(sortFromBody(body, 0));
        categoryMapper.insert(row);
        Category saved = categoryMapper.selectById(row.getId());
        if (saved == null) {
            throw new BusinessException(500, "创建失败");
        }
        return toApi(saved);
    }

    public void update(int id, Map<String, Object> body) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "分类不存在");
        }
        if (body.containsKey("name")) {
            String name = str(body.get("name"));
            if (name.isBlank()) {
                throw new BusinessException(400, "分类名称不能为空");
            }
            existing.setName(name.trim());
        }
        if (body.containsKey("sort_order") || body.containsKey("sort")) {
            existing.setSort(sortFromBody(body, existing.getSort() != null ? existing.getSort() : 0));
        }
        categoryMapper.update(existing);
    }

    public void delete(int id) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "分类不存在");
        }
        categoryMapper.deleteById(id);
    }

    private Map<String, Object> toApi(Category c) {
        Map<String, Object> m = new LinkedHashMap<>();
        int sort = c.getSort() != null ? c.getSort() : 0;
        m.put("id", c.getId());
        m.put("name", c.getName() != null ? c.getName() : "");
        m.put("sort_order", sort);
        m.put("sort", sort);
        if (c.getCreatedAt() != null) {
            m.put("created_at", DT.format(c.getCreatedAt()));
        } else {
            m.put("created_at", "");
        }
        if (c.getUpdatedAt() != null) {
            m.put("updated_at", DT.format(c.getUpdatedAt()));
        } else {
            m.put("updated_at", "");
        }
        return m;
    }

    private static int sortFromBody(Map<String, Object> body, int defaultSort) {
        if (body.containsKey("sort_order")) {
            return intOrZero(body.get("sort_order"), defaultSort);
        }
        if (body.containsKey("sort")) {
            return intOrZero(body.get("sort"), defaultSort);
        }
        return defaultSort;
    }

    private static long nzLong(Long v) {
        return v != null ? v : 0L;
    }

    private static String str(Object o) {
        return o == null ? "" : Objects.toString(o, "");
    }

    private static int intOrZero(Object o, int defaultIfNull) {
        if (o == null || str(o).isBlank()) {
            return defaultIfNull;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return Integer.parseInt(o.toString().trim());
    }
}
