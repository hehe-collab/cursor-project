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

    /** 全量或按名称筛选；用于下拉，返回列表 */
    public List<Map<String, Object>> listAll(String name) {
        if (name == null || name.isBlank()) {
            return tagMapper.selectAll().stream().map(this::toApi).collect(Collectors.toList());
        }
        String n = name.trim();
        long total = tagMapper.countByParam(n);
        if (total > 10_000) {
            return tagMapper.selectByParam(n, 0, 10_000).stream().map(this::toApi).collect(Collectors.toList());
        }
        return tagMapper.selectByParam(n, 0, (int) total).stream().map(this::toApi).collect(Collectors.toList());
    }

    public Map<String, Object> listPage(String name, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.min(100, Math.max(1, pageSize));
        int offset = (p - 1) * ps;
        String n = name != null && !name.isBlank() ? name.trim() : null;
        long total = tagMapper.countByParam(n);
        List<Map<String, Object>> list =
                tagMapper.selectByParam(n, offset, ps).stream().map(this::toApi).collect(Collectors.toList());
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

    public Map<String, Object> create(Map<String, Object> body) {
        String name = str(body.get("name"));
        if (name.isBlank()) {
            throw new BusinessException(400, "标签名称不能为空");
        }
        Tag row = new Tag();
        row.setName(name.trim());
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
        try {
            tagMapper.update(existing);
        } catch (Exception e) {
            throw new BusinessException(400, "标签名称已存在");
        }
    }

    public void delete(int id) {
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "标签不存在");
        }
        tagMapper.deleteById(id);
    }

    private Map<String, Object> toApi(Tag t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName() != null ? t.getName() : "");
        int dc = t.getDramaCount() != null ? t.getDramaCount() : 0;
        m.put("drama_count", dc);
        if (t.getCreatedAt() != null) {
            m.put("created_at", DT.format(t.getCreatedAt()));
        } else {
            m.put("created_at", "");
        }
        if (t.getUpdatedAt() != null) {
            m.put("updated_at", DT.format(t.getUpdatedAt()));
        } else {
            m.put("updated_at", "");
        }
        return m;
    }

    private static long nzLong(Long v) {
        return v != null ? v : 0L;
    }

    private static String str(Object o) {
        return o == null ? "" : Objects.toString(o, "");
    }
}
