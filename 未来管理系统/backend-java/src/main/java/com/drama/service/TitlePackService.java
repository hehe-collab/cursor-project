package com.drama.service;

import com.drama.entity.TitlePack;
import com.drama.exception.BusinessException;
import com.drama.mapper.TitlePackMapper;
import com.drama.util.ExcelExportUtil;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TitlePackService {

    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter EXPORT_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CN);

    private final TitlePackMapper titlePackMapper;

    public Map<String, Object> listPage(int page, int pageSize, String titlePackId, String title) {
        List<TitlePack> filtered = filter(titlePackMapper.selectAllOrderByIdDesc(), titlePackId, title);
        int total = filtered.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> list = filtered.subList(from, to).stream().map(this::toClientRow).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", total);
        return data;
    }

    public byte[] exportExcel(String titlePackId, String title) throws IOException {
        List<TitlePack> filtered = filter(titlePackMapper.selectAllOrderByIdDesc(), titlePackId, title);
        String[] headers = {"标题包ID", "标题名称", "标题内容", "创建人", "创建时间"};
        List<Object[]> rows = new ArrayList<>();
        for (TitlePack r : filtered) {
            Map<String, Object> c = toClientRow(r);
            rows.add(
                    new Object[] {
                        c.get("id"),
                        c.get("name"),
                        c.get("content"),
                        c.get("created_by_name"),
                        c.get("created_at")
                    });
        }
        if (rows.isEmpty()) {
            rows.add(new Object[headers.length]);
        }
        return ExcelExportUtil.buildXlsx("标题包", headers, rows);
    }

    private List<TitlePack> filter(List<TitlePack> list, String titlePackId, String title) {
        List<TitlePack> out = new ArrayList<>(list);
        if (titlePackId != null && !titlePackId.isBlank()) {
            String k = titlePackId.trim();
            out = out.stream()
                    .filter(
                            r -> String.valueOf(r.getId()).equals(k) || String.valueOf(r.getId()).contains(k))
                    .collect(Collectors.toList());
        }
        if (title != null && !title.isBlank()) {
            String t = title.trim();
            out = out.stream()
                    .filter(r -> r.getName() != null && r.getName().contains(t))
                    .collect(Collectors.toList());
        }
        return out;
    }

    private Map<String, Object> toClientRow(TitlePack r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("name", r.getName() != null ? r.getName() : "");
        m.put("content", r.getContent() != null ? r.getContent() : "");
        m.put("created_by_name", r.getCreatedByName() != null ? r.getCreatedByName() : "");
        m.put(
                "created_at",
                r.getCreatedAt() != null ? EXPORT_DT.format(r.getCreatedAt().atZone(CN)) : "");
        return m;
    }

    @Transactional
    public int create(String name, String content, Integer adminId, String createdByName) {
        TitlePack row = new TitlePack();
        row.setName(name);
        row.setContent(content);
        row.setCreatedBy(adminId != null ? adminId : 0);
        row.setCreatedByName(createdByName != null ? createdByName : "");
        titlePackMapper.insert(row);
        return row.getId();
    }

    @Transactional
    public void update(int id, String name, String content) {
        TitlePack existing = titlePackMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "记录不存在");
        }
        TitlePack row = new TitlePack();
        row.setId(id);
        row.setName(name);
        row.setContent(content);
        row.setCreatedBy(existing.getCreatedBy());
        row.setCreatedByName(existing.getCreatedByName());
        titlePackMapper.update(row);
    }

    @Transactional
    public void deleteOne(int id) {
        if (titlePackMapper.selectById(id) == null) {
            throw new BusinessException(404, "记录不存在");
        }
        titlePackMapper.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的记录");
        }
        titlePackMapper.deleteByIds(ids);
    }
}
