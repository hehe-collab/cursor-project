package com.drama.service;

import com.drama.dto.TikTokApiResponseDTO;
import com.drama.entity.AdMaterial;
import com.drama.entity.AdMaterialRecord;
import com.drama.exception.BusinessException;
import com.drama.mapper.AdMaterialMapper;
import com.drama.mapper.AdMaterialRecordMapper;
import com.drama.util.TikTokApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdMaterialService {

    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter VIEW_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CN);
    private static final TypeReference<TikTokApiResponseDTO<Map<String, Object>>> MAP_RESPONSE =
            new TypeReference<>() {};
    private static final Set<String> VIDEO_EXTENSIONS =
            Set.of("mp4", "mov", "m4v", "webm", "avi", "mpeg", "mpg");
    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

    private final AdMaterialMapper adMaterialMapper;
    private final AdMaterialRecordMapper adMaterialRecordMapper;
    private final AdAccountService adAccountService;
    private final TikTokAccountService tikTokAccountService;
    private final TikTokApiClient tikTokApiClient;
    private final ObjectMapper objectMapper;

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

    public List<Map<String, Object>> accountOptions() {
        return adMaterialMapper.selectDistinctAccountOptions().stream()
                .map(
                        row -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("accountId", stringVal(row.get("accountId")));
                            item.put("accountName", stringVal(row.get("accountName")));
                            item.put("subjectName", stringVal(row.get("subjectName")));
                            item.put("materialCount", row.get("materialCount"));
                            return item;
                        })
                .filter(item -> !stringVal(item.get("accountId")).isBlank())
                .sorted(Comparator.comparing(item -> stringVal(item.get("accountId"))))
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
    public Map<String, Object> uploadMaterials(
            String accountId, String folder, String files, Integer adminId) {
        String normalizedAccountId = stringVal(accountId).trim();
        if (normalizedAccountId.isBlank()) {
            throw new BusinessException(400, "请选择账户ID");
        }

        Map<String, Object> executableAccount =
                adAccountService.executableOptions("tiktok", "active").stream()
                        .filter(item -> normalizedAccountId.equals(stringVal(item.get("accountId")).trim()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                400,
                                                "该账户不是可执行 TikTok 账户，请先在账户管理录入并确保 OAuth 为 active"));

        List<String> urls = parseUploadUrls(files);
        if (urls.isEmpty()) {
            throw new BusinessException(400, "请输入至少一个下载URL");
        }

        String accountName =
                firstNonBlank(
                        stringVal(executableAccount.get("accountName")),
                        stringVal(executableAccount.get("advertiserName")));
        String accessToken = tikTokAccountService.getValidAccessToken(normalizedAccountId);
        List<Map<String, Object>> errors = new ArrayList<>();
        List<Map<String, Object>> uploaded = new ArrayList<>();
        int successCount = 0;

        for (String url : urls) {
            try {
                UploadedMaterial uploadedMaterial =
                        uploadSingleMaterial(
                                normalizedAccountId,
                                accountName,
                                StringUtils.hasText(folder) ? folder.trim() : "",
                                url,
                                accessToken,
                                adminId);
                uploaded.add(uploadedMaterial.toMap());
                successCount++;
            } catch (Exception e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("url", url);
                error.put("message", e.getMessage());
                errors.add(error);
            }
        }

        AdMaterialRecord r = new AdMaterialRecord();
        r.setAccountId(normalizedAccountId);
        r.setAccountName(accountName);
        r.setTaskType("upload");
        r.setStatus(resolveTaskStatus(successCount, errors.size()));
        r.setDetail(writeJsonSafely(Map.of(
                "folder", stringVal(folder).trim(),
                "totalCount", urls.size(),
                "successCount", successCount,
                "failedCount", errors.size(),
                "materials", uploaded,
                "errors", errors)));
        adMaterialRecordMapper.insert(r);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("accountId", normalizedAccountId);
        summary.put("accountName", accountName);
        summary.put("totalCount", urls.size());
        summary.put("successCount", successCount);
        summary.put("failedCount", errors.size());
        summary.put("status", r.getStatus());
        summary.put("materials", uploaded);
        summary.put("errors", errors);
        return summary;
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

    private UploadedMaterial uploadSingleMaterial(
            String accountId,
            String accountName,
            String folder,
            String url,
            String accessToken,
            Integer adminId) {
        MaterialKind kind = detectMaterialKind(url);
        String fileName = extractFileName(url);

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("advertiser_id", accountId);
        body.add("upload_type", "UPLOAD_BY_URL");
        body.add("file_name", fileName);

        String endpoint;
        if (kind == MaterialKind.VIDEO) {
            endpoint = "file/video/ad/upload/";
            body.add("video_url", url);
        } else {
            endpoint = "file/image/ad/upload/";
            body.add("image_url", url);
        }

        TikTokApiResponseDTO<Map<String, Object>> response =
                tikTokApiClient.postMultipart(accountId, accessToken, endpoint, body, MAP_RESPONSE);
        if (!response.isSuccess()) {
            throw new BusinessException(
                    400,
                    firstNonBlank(response.getMessage(), "TikTok 素材上传失败"));
        }

        Map<String, Object> data = response.getData();
        String remoteId =
                kind == MaterialKind.VIDEO
                        ? firstNonBlank(valueOf(data, "video_id"), valueOf(data, "videoId"))
                        : firstNonBlank(valueOf(data, "image_id"), valueOf(data, "imageId"));
        if (!StringUtils.hasText(remoteId)) {
            throw new BusinessException(500, "TikTok 上传成功但未返回素材ID");
        }

        String coverUrl =
                kind == MaterialKind.VIDEO
                        ? firstNonBlank(
                                valueOf(data, "poster_url"),
                                valueOf(data, "posterUrl"),
                                valueOf(data, "cover_url"),
                                valueOf(data, "coverUrl"))
                        : url;

        String materialName = deriveMaterialName(fileName);
        AdMaterial material = adMaterialMapper.selectByMaterialId(remoteId);
        if (material == null) {
            material = new AdMaterial();
            material.setMaterialId(remoteId);
            material.setCreatedBy(adminId != null ? adminId : 0);
        } else {
            material.setId(material.getId());
            material.setCreatedBy(material.getCreatedBy());
        }
        material.setMaterialName(materialName);
        material.setType(kind == MaterialKind.VIDEO ? "video" : "image");
        material.setEntityName(folder);
        material.setAccountId(accountId);
        material.setVideoId(kind == MaterialKind.VIDEO ? remoteId : "");
        material.setCoverUrl(coverUrl);

        if (material.getId() == null) {
            adMaterialMapper.insert(material);
        } else {
            adMaterialMapper.update(material);
        }

        return new UploadedMaterial(
                material.getMaterialId(),
                material.getMaterialName(),
                material.getType(),
                material.getVideoId(),
                material.getCoverUrl(),
                url,
                accountId,
                firstNonBlank(accountName, ""));
    }

    private List<String> parseUploadUrls(String rawFiles) {
        if (!StringUtils.hasText(rawFiles)) {
            return List.of();
        }
        return Arrays.stream(rawFiles.split("\\r?\\n"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private MaterialKind detectMaterialKind(String url) {
        String path;
        try {
            path = URI.create(url).getPath();
        } catch (Exception e) {
            throw new BusinessException(400, "无效的素材 URL: " + url);
        }
        String ext = "";
        if (path != null) {
            int idx = path.lastIndexOf('.');
            if (idx >= 0 && idx < path.length() - 1) {
                ext = path.substring(idx + 1).toLowerCase(Locale.ROOT);
            }
        }
        if (VIDEO_EXTENSIONS.contains(ext)) {
            return MaterialKind.VIDEO;
        }
        if (IMAGE_EXTENSIONS.contains(ext)) {
            return MaterialKind.IMAGE;
        }
        throw new BusinessException(400, "无法从 URL 识别素材类型，请使用带图片/视频后缀的直链: " + url);
    }

    private String extractFileName(String url) {
        String fallback = "material_" + System.currentTimeMillis();
        try {
            String path = URI.create(url).getPath();
            if (!StringUtils.hasText(path)) {
                return fallback;
            }
            int slash = path.lastIndexOf('/');
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            if (!StringUtils.hasText(name)) {
                return fallback;
            }
            return name.length() > 100 ? name.substring(name.length() - 100) : name;
        } catch (Exception e) {
            return fallback;
        }
    }

    private String deriveMaterialName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    private String resolveTaskStatus(int successCount, int failedCount) {
        if (failedCount == 0) {
            return "success";
        }
        if (successCount > 0) {
            return "partial";
        }
        return "failed";
    }

    private String writeJsonSafely(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
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
        o.put("status", r.getStatus() != null ? r.getStatus() : "pending");
        o.put(
                "createdAt",
                r.getCreatedAt() != null ? VIEW_DT.format(r.getCreatedAt().atZone(CN)) : "");
        o.put("taskType", r.getTaskType() != null ? r.getTaskType() : "");
        o.put("detail", r.getDetail() != null ? r.getDetail() : "");
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

    private static String stringVal(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private static String valueOf(Map<String, Object> data, String key) {
        if (data == null || key == null) {
            return "";
        }
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private enum MaterialKind {
        IMAGE,
        VIDEO
    }

    private record UploadedMaterial(
            String materialId,
            String materialName,
            String type,
            String videoId,
            String coverUrl,
            String sourceUrl,
            String accountId,
            String accountName) {
        private Map<String, Object> toMap() {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", materialId);
            item.put("materialName", materialName);
            item.put("type", type);
            item.put("videoId", videoId);
            item.put("coverUrl", coverUrl);
            item.put("sourceUrl", sourceUrl);
            item.put("accountId", accountId);
            item.put("accountName", accountName);
            return item;
        }
    }
}
