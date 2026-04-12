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
import org.springframework.transaction.annotation.Transactional;

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

    public List<Map<String, Object>> listAll(String name, Boolean isEnabled) {
        if (name == null || name.isBlank()) {
            return categoryMapper.selectAll().stream().map(this::toApi).collect(Collectors.toList());
        }
        String n = name.trim();
        long total = categoryMapper.countByParam(n, isEnabled);
        int limit = (int) Math.min(total, 10_000);
        return categoryMapper.selectByParam(n, isEnabled, 0, limit).stream().map(this::toApi).collect(Collectors.toList());
    }

    public Map<String, Object> listPage(String name, Boolean isEnabled, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.min(100, Math.max(1, pageSize));
        int offset = (p - 1) * ps;
        String n = name != null && !name.isBlank() ? name.trim() : null;
        long total = categoryMapper.countByParam(n, isEnabled);
        List<Map<String, Object>> list =
                categoryMapper.selectByParam(n, isEnabled, offset, ps).stream().map(this::toApi).collect(Collectors.toList());
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

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String name = str(body.get("name"));
        if (name.isBlank()) {
            throw new BusinessException(400, "分类名称不能为空");
        }
        Category row = new Category();
        row.setName(name.trim());
        row.setSlug(slugFromBody(body));
        row.setDescription(str(body.get("description")));
        row.setSort(intOrDefault(body, "sort_order", body.get("sort"), 0));
        row.setIsEnabled(boolOrDefault(body, "is_enabled", true));
        categoryMapper.insert(row);
        Category saved = categoryMapper.selectById(row.getId());
        if (saved == null) {
            throw new BusinessException(500, "创建失败");
        }
        return toApi(saved);
    }

    @Transactional
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
        if (body.containsKey("slug")) {
            existing.setSlug(str(body.get("slug")));
        }
        if (body.containsKey("description")) {
            existing.setDescription(str(body.get("description")));
        }
        if (body.containsKey("sort_order") || body.containsKey("sort")) {
            existing.setSort(intOrDefault(body, "sort_order", body.get("sort"), existing.getSort() != null ? existing.getSort() : 0));
        }
        if (body.containsKey("is_enabled")) {
            existing.setIsEnabled(boolOrDefault(body, "is_enabled", true));
        }
        categoryMapper.update(existing);
    }

    @Transactional
    public void delete(int id) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "分类不存在");
        }
        if (existing.getDramaCount() != null && existing.getDramaCount() > 0) {
            throw new BusinessException(400, "该分类下还有 " + existing.getDramaCount() + " 部短剧，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    @Transactional
    public void batchUpdateSort(List<Integer> ids) {
        for (int i = 0; i < ids.size(); i++) {
            Category c = new Category();
            c.setId(ids.get(i));
            c.setSort(i + 1);
            categoryMapper.update(c);
        }
    }

    private Map<String, Object> toApi(Category c) {
        Map<String, Object> m = new LinkedHashMap<>();
        int sort = c.getSort() != null ? c.getSort() : 0;
        m.put("id", c.getId());
        m.put("name", c.getName() != null ? c.getName() : "");
        m.put("slug", c.getSlug() != null ? c.getSlug() : "");
        m.put("description", c.getDescription() != null ? c.getDescription() : "");
        m.put("sort_order", sort);
        m.put("sort", sort);
        m.put("is_enabled", c.getIsEnabled() != null ? c.getIsEnabled() : true);
        int dc = c.getDramaCount() != null ? c.getDramaCount() : 0;
        m.put("drama_count", dc);
        m.put("created_at", c.getCreatedAt() != null ? DT.format(c.getCreatedAt()) : "");
        m.put("updated_at", c.getUpdatedAt() != null ? DT.format(c.getUpdatedAt()) : "");
        return m;
    }

    private static String slugFromBody(Map<String, Object> body) {
        String s = str(body.get("slug"));
        if (s.isBlank() && body.containsKey("name")) {
            String name = str(body.get("name")).toLowerCase()
                    .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                    .replaceAll("^-|-$", "");
            return name;
        }
        return s;
    }

    private static long nzLong(Long v) {
        return v != null ? v : 0L;
    }

    private static String str(Object o) {
        return o == null ? "" : Objects.toString(o, "");
    }

    private static int intOrDefault(Map<String, Object> body, String key1, Object key2, int def) {
        if (body.containsKey(key1)) return intOrZero(body.get(key1), def);
        if (body.containsKey(key2 != null ? key2.toString() : "")) return intOrZero(body.get(key2.toString()), def);
        return def;
    }

    private static Boolean boolOrDefault(Map<String, Object> body, String key, boolean def) {
        if (!body.containsKey(key)) return def;
        Object v = body.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        String s = str(v);
        if (s.isBlank()) return def;
        return !"0".equals(s) && !"false".equalsIgnoreCase(s) && !"no".equalsIgnoreCase(s);
    }

    private static int intOrZero(Object o, int def) {
        if (o == null || str(o).isBlank()) return def;
        if (o instanceof Number) return ((Number) o).intValue();
        return Integer.parseInt(o.toString().trim());
    }
}
