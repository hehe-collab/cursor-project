package com.drama.service;

import com.drama.entity.AdMaterial;
import com.drama.entity.AdMaterialRecord;
import com.drama.exception.BusinessException;
import com.drama.mapper.AdMaterialMapper;
import com.drama.mapper.AdMaterialRecordMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdMaterialService {

    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter VIEW_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CN);

    private final AdMaterialMapper adMaterialMapper;
    private final AdMaterialRecordMapper adMaterialRecordMapper;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public Map<String, Object> listPage(int page, int pageSize, Map<String, String> filter) {
        List<AdMaterial> filtered = filterMaterials(adMaterialMapper.selectAllOrderByIdDesc(), filter);
        int total = filtered.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> rows =
                filtered.subList(from, to).stream().map(this::toClientMaterial).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", rows);
        data.put("total", total);
        return data;
    }

    public List<Map<String, Object>> records() {
        return adMaterialRecordMapper.selectAllOrderByCreatedDesc().stream()
                .map(this::toClientRecord)
                .toList();
    }

    @Transactional
    public Map<String, String> saveUploadedFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "未选择文件");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new BusinessException(400, "仅支持图片");
        }
        Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')) : ".jpg";
        String name = "ad-" + System.currentTimeMillis() + "-" + (long) (Math.random() * 1e9) + ext;
        if (!name.matches("^[a-zA-Z0-9._-]+$")) {
            name = "ad-" + System.currentTimeMillis() + ".jpg";
        }
        Path target = dir.resolve(name).normalize();
        if (!target.startsWith(dir)) {
            throw new BusinessException(400, "非法路径");
        }
        file.transferTo(target.toFile());
        Map<String, String> data = new LinkedHashMap<>();
        data.put("url", "/api/uploads/" + name);
        return data;
    }

    @Transactional
    public void logUploadTask(String accountId, String folder, String files) {
        int lines = 0;
        if (files != null) {
            for (String line : files.split("\n")) {
                if (!line.trim().isEmpty()) {
                    lines++;
                }
            }
        }
        AdMaterialRecord r = new AdMaterialRecord();
        r.setAccountId(accountId != null ? accountId : "");
        r.setAccountName("");
        r.setStatus("success");
        r.setTaskType("upload");
        r.setDetail("{\"folder\":\"" + (folder != null ? folder.replace("\"", "'") : "") + "\",\"lines\":" + lines + "}");
        adMaterialRecordMapper.insert(r);
    }

    @Transactional
    public void logSyncTask(String accountIdsRaw) {
        String raw = accountIdsRaw != null ? accountIdsRaw : "";
        List<String> lines =
                java.util.Arrays.stream(raw.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        AdMaterialRecord r = new AdMaterialRecord();
        r.setAccountId(lines.isEmpty() ? "" : lines.get(0));
        r.setAccountName(lines.size() > 1 ? "等" + lines.size() + "个账户" : "");
        r.setStatus("success");
        r.setTaskType("sync");
        r.setDetail(String.join(",", lines));
        adMaterialRecordMapper.insert(r);
    }

    @Transactional
    public int create(Map<String, Object> body, Integer adminId) {
        AdMaterial row = normalizeBody(body);
        row.setMaterialId("MAT" + System.currentTimeMillis());
        row.setCreatedBy(adminId != null ? adminId : 0);
        adMaterialMapper.insert(row);
        return row.getId();
    }

    @Transactional
    public void update(int id, Map<String, Object> body) {
        AdMaterial existing = adMaterialMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "记录不存在");
        }
        AdMaterial row = normalizeBody(body);
        row.setId(id);
        row.setMaterialId(existing.getMaterialId());
        row.setCreatedBy(existing.getCreatedBy());
        adMaterialMapper.update(row);
    }

    @Transactional
    public void deleteOne(int id) {
        if (adMaterialMapper.selectById(id) == null) {
            throw new BusinessException(404, "记录不存在");
        }
        adMaterialMapper.deleteById(id);
    }

    private Map<String, Object> toClientMaterial(AdMaterial m) {
        String materialId =
                m.getMaterialId() != null && !m.getMaterialId().isEmpty() ? m.getMaterialId() : ("MAT" + m.getId());
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", m.getId());
        o.put("materialId", materialId);
        o.put("type", m.getType() != null ? m.getType() : "image");
        o.put("entityName", m.getEntityName() != null ? m.getEntityName() : "");
        o.put("accountId", m.getAccountId() != null ? m.getAccountId() : "");
        o.put("videoId", m.getVideoId() != null ? m.getVideoId() : "");
        o.put(
                "materialName",
                m.getMaterialName() != null && !m.getMaterialName().isEmpty()
                        ? m.getMaterialName()
                        : "");
        o.put(
                "coverUrl",
                m.getCoverUrl() != null && !m.getCoverUrl().isEmpty() ? m.getCoverUrl() : "");
        o.put(
                "createdAt",
                m.getCreatedAt() != null ? VIEW_DT.format(m.getCreatedAt().atZone(CN)) : "");
        return o;
    }

    private Map<String, Object> toClientRecord(AdMaterialRecord r) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", r.getId());
        o.put("accountId", r.getAccountId() != null ? r.getAccountId() : "");
        o.put("accountName", r.getAccountName() != null ? r.getAccountName() : "");
        o.put("status", "success".equals(r.getStatus()) ? "success" : "pending");
        o.put(
                "createdAt",
                r.getCreatedAt() != null ? VIEW_DT.format(r.getCreatedAt().atZone(CN)) : "");
        o.put("taskType", r.getTaskType() != null ? r.getTaskType() : "");
        return o;
    }

    private List<AdMaterial> filterMaterials(List<AdMaterial> list, Map<String, String> filter) {
        List<AdMaterial> out = new ArrayList<>(list);
        String accountId = filter.get("accountId");
        if (accountId != null && !accountId.isBlank()) {
            String aid = accountId.trim();
            out = out.stream()
                    .filter(r -> aid.equals((r.getAccountId() != null ? r.getAccountId() : "").trim()))
                    .collect(Collectors.toList());
        }
        String materialId = filter.get("materialId");
        if (materialId != null && !materialId.isBlank()) {
            String k = materialId.trim();
            out = out.stream()
                    .filter(r -> {
                        String mid = r.getMaterialId() != null ? r.getMaterialId() : ("MAT" + r.getId());
                        return mid.contains(k) || String.valueOf(r.getId()).equals(k);
                    })
                    .collect(Collectors.toList());
        }
        String nameFuzzy = filter.get("materialName");
        if (nameFuzzy == null || nameFuzzy.isBlank()) {
            nameFuzzy = filter.get("name");
        }
        if (nameFuzzy != null && !nameFuzzy.isBlank()) {
            String k = nameFuzzy;
            out = out.stream()
                    .filter(r -> {
                        String n = r.getMaterialName() != null ? r.getMaterialName() : "";
                        return n.contains(k);
                    })
                    .collect(Collectors.toList());
        }
        String entityName = filter.get("entityName");
        if (entityName != null && !entityName.isBlank()) {
            String e = entityName.trim();
            out = out.stream()
                    .filter(r -> e.equals(r.getEntityName() != null ? r.getEntityName() : ""))
                    .collect(Collectors.toList());
        }
        return out;
    }

    private AdMaterial normalizeBody(Map<String, Object> body) {
        AdMaterial m = new AdMaterial();
        Object mn = body.get("materialName");
        if (mn == null) {
            mn = body.get("material_name");
        }
        if (mn == null) {
            mn = body.get("name");
        }
        m.setMaterialName(mn != null ? String.valueOf(mn) : "");
        m.setType(body.get("type") != null ? String.valueOf(body.get("type")) : "image");
        Object en = body.get("entityName");
        if (en == null) {
            en = body.get("entity_name");
        }
        m.setEntityName(en != null ? String.valueOf(en) : "");
        Object cu = body.get("coverUrl");
        if (cu == null) {
            cu = body.get("cover_url");
        }
        if (cu == null) {
            cu = body.get("url");
        }
        m.setCoverUrl(cu != null ? String.valueOf(cu) : "");
        Object aid = body.get("accountId");
        if (aid == null) {
            aid = body.get("account_id");
        }
        m.setAccountId(aid != null ? String.valueOf(aid) : "");
        Object vid = body.get("videoId");
        if (vid == null) {
            vid = body.get("video_id");
        }
        m.setVideoId(vid != null ? String.valueOf(vid) : "");
        return m;
    }
}
