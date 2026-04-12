package com.drama.service;

import com.drama.dto.TagStatsRow;
import com.drama.entity.Tag;
import com.drama.exception.BusinessException;
import com.drama.mapper.TagMapper;
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
public class TagService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TagMapper tagMapper;

    public Map<String, Object> stats() {
        TagStatsRow row = tagMapper.selectStats();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total_count", nzLong(row != null ? row.getTotalCount() : null));
        m.put("tags_linked_count", nzLong(row != null ? row.getTagsLinkedCount() : null));
        m.put("total_links", nzLong(row != null ? row.getTotalLinks() : null));
        m.put("dramas_with_tags_count", nzLong(row != null ? row.getDramasWithTagsCount() : null));
        return m;
    }

    public List<Map<String, Object>> listAll(String name, Boolean isHot, String orderBy) {
        List<Tag> rows;
        if (name == null || name.isBlank()) {
            rows = tagMapper.selectAll();
        } else {
            String n = name.trim();
            long total = tagMapper.countByParam(n, isHot);
            int limit = (int) Math.min(total, 10_000);
            rows = tagMapper.selectByParam(n, isHot, 0, limit);
        }
        return rows.stream().map(this::toApi).collect(Collectors.toList());
    }

    public Map<String, Object> listPage(String name, Boolean isHot, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.min(100, Math.max(1, pageSize));
        int offset = (p - 1) * ps;
        String n = name != null && !name.isBlank() ? name.trim() : null;
        long total = tagMapper.countByParam(n, isHot);
        List<Map<String, Object>> list =
                tagMapper.selectByParam(n, isHot, offset, ps).stream().map(this::toApi).collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("page", p);
        data.put("pageSize", ps);
        data.put("list", list);
        return data;
    }

    public Map<String, Object> getById(int id) {
        Tag t = tagMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(404, "标签不存在");
        }
        return toApi(t);
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String name = str(body.get("name"));
        if (name.isBlank()) {
            throw new BusinessException(400, "标签名称不能为空");
        }
        Tag row = new Tag();
        row.setName(name.trim());
        row.setColor(strOrDefault(body, "color", "#409EFF"));
        row.setSortOrder(intOrDefault(body, "sort_order", 0));
        row.setIsHot(boolOrDefault(body, "is_hot", false));
        try {
            tagMapper.insert(row);
        } catch (Exception e) {
            throw new BusinessException(400, "标签名称已存在");
        }
        Tag saved = tagMapper.selectById(row.getId());
        if (saved == null) {
            throw new BusinessException(500, "创建失败");
        }
        return toApi(saved);
    }

    @Transactional
    public void update(int id, Map<String, Object> body) {
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "标签不存在");
        }
        if (body.containsKey("name")) {
            String name = str(body.get("name"));
            if (name.isBlank()) {
                throw new BusinessException(400, "标签名称不能为空");
            }
            existing.setName(name.trim());
        }
        if (body.containsKey("color")) {
            existing.setColor(strOrDefault(body, "color", "#409EFF"));
        }
        if (body.containsKey("sort_order")) {
            existing.setSortOrder(intOrDefault(body, "sort_order", 0));
        }
        if (body.containsKey("is_hot")) {
            existing.setIsHot(boolOrDefault(body, "is_hot", false));
        }
        try {
            tagMapper.update(existing);
        } catch (Exception e) {
            throw new BusinessException(400, "标签名称已存在");
        }
    }

    @Transactional
    public void delete(int id) {
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "标签不存在");
        }
        if (existing.getUsageCount() != null && existing.getUsageCount() > 0) {
            throw new BusinessException(400, "该标签已被使用 " + existing.getUsageCount() + " 次，无法删除");
        }
        tagMapper.deleteById(id);
    }

    @Transactional
    public void batchSetHot(List<Integer> ids, boolean isHot) {
        for (Integer id : ids) {
            Tag t = new Tag();
            t.setId(id);
            t.setIsHot(isHot);
            tagMapper.update(t);
        }
    }

    private Map<String, Object> toApi(Tag t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName() != null ? t.getName() : "");
        m.put("color", t.getColor() != null ? t.getColor() : "#409EFF");
        m.put("sort_order", t.getSortOrder() != null ? t.getSortOrder() : 0);
        m.put("is_hot", t.getIsHot() != null ? t.getIsHot() : false);
        m.put("usage_count", t.getUsageCount() != null ? t.getUsageCount() : 0);
        int dc = t.getDramaCount() != null ? t.getDramaCount() : 0;
        m.put("drama_count", dc);
        m.put("created_at", t.getCreatedAt() != null ? DT.format(t.getCreatedAt()) : "");
        m.put("updated_at", t.getUpdatedAt() != null ? DT.format(t.getUpdatedAt()) : "");
        return m;
    }

    private static long nzLong(Long v) {
        return v != null ? v : 0L;
    }

    private static String str(Object o) {
        return o == null ? "" : Objects.toString(o, "");
    }

    private static String strOrDefault(Map<String, Object> body, String key, String def) {
        String s = str(body.get(key));
        return s.isBlank() ? def : s;
    }

    private static int intOrDefault(Map<String, Object> body, String key, int def) {
        return intOrZero(body.get(key), def);
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
