package com.drama.service;

import com.drama.dto.DramaStatsRow;
import com.drama.entity.Drama;
import com.drama.entity.DramaEpisode;
import com.drama.entity.DramaTag;
import com.drama.exception.BusinessException;
import com.drama.mapper.DramaEpisodeMapper;
import com.drama.mapper.DramaMapper;
import com.drama.mapper.DramaTagMapper;
import com.drama.mapper.TagMapper;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.drama.service.cache.DramaCacheSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DramaService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 业务剧 ID：易辨识、去易混字符 0/O/1/I */
    private static final String DRAMA_PUBLIC_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final DramaMapper dramaMapper;
    private final DramaCacheSupport dramaCacheSupport;
    private final DramaEpisodeMapper dramaEpisodeMapper;
    private final DramaTagMapper dramaTagMapper;
    private final TagMapper tagMapper;

    public Map<String, Object> stats(String title, Integer categoryId, String status, Integer id, String publicId) {
        String t = title != null && !title.isBlank() ? title.trim() : null;
        String dbStatus = toDbStatusFilter(status);
        String pub = publicId != null && !publicId.isBlank() ? publicId.trim() : null;
        DramaStatsRow row = dramaMapper.selectStatsAggregate(t, categoryId, dbStatus, id, pub);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total_count", nzLong(row != null ? row.getTotalCount() : null));
        m.put("published_count", nzLong(row != null ? row.getPublishedCount() : null));
        m.put("draft_count", nzLong(row != null ? row.getDraftCount() : null));
        m.put("offline_count", nzLong(row != null ? row.getOfflineCount() : null));
        m.put("total_views", nzLong(row != null ? row.getTotalViews() : null));
        m.put(
                "total_episodes_sum",
                nzLong(row != null ? row.getTotalEpisodesSum() : null));
        m.put("total", m.get("total_count"));
        return m;
    }

    public Map<String, Object> list(
            String title, Integer categoryId, String status, Integer id, String publicId, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.min(100, Math.max(1, pageSize));
        int offset = (p - 1) * ps;
        String dbStatus = toDbStatusFilter(status);
        String pub = publicId != null && !publicId.isBlank() ? publicId.trim() : null;
        long total = dramaMapper.countByParam(title, categoryId, dbStatus, id, pub);
        List<Drama> rows = dramaMapper.selectByParam(title, categoryId, dbStatus, id, pub, offset, ps);
        List<Map<String, Object>> list = rows.stream().map(this::toApiListItem).collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("page", p);
        data.put("pageSize", ps);
        data.put("list", list);
        return data;
    }

    public Map<String, Object> getById(int id) {
        Drama d = dramaCacheSupport.fetchWithCategory(id);
        if (d == null) {
            throw new BusinessException(404, "短剧不存在");
        }
        String categoryName = d.getCategoryName() != null ? d.getCategoryName() : "";
        List<String> tagNames = tagMapper.selectNamesByDramaId(id);
        List<DramaEpisode> eps = dramaEpisodeMapper.selectByDramaId(id);
        List<Map<String, Object>> epList =
                eps.stream().map(this::episodeToApi).collect(Collectors.toList());
        Map<String, Object> data = toApiDetail(d, categoryName);
        data.put("tags", tagNames);
        data.put("episodes", epList);
        return data;
    }

    public Map<String, Object> listEpisodes(int dramaId) {
        if (dramaMapper.selectById(dramaId) == null) {
            throw new BusinessException(404, "短剧不存在");
        }
        List<DramaEpisode> eps = dramaEpisodeMapper.selectByDramaId(dramaId);
        List<Map<String, Object>> list = eps.stream().map(this::episodeToApi).collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return data;
    }

    @Transactional
    public Map<String, Object> createEpisode(int dramaId, Map<String, Object> body) {
        if (dramaMapper.selectById(dramaId) == null) {
            throw new BusinessException(404, "短剧不存在");
        }
        int epNum = episodeNumFromBody(body);
        if (epNum <= 0) {
            throw new BusinessException(400, "集序号 episode_number / episode_num 须为正整数");
        }
        DramaEpisode e = new DramaEpisode();
        e.setDramaId(dramaId);
        e.setEpisodeNum(epNum);
        e.setTitle(nullIfBlank(str(body.get("title"))));
        e.setVideoId(nullIfBlank(str(body.get("video_id"))));
        e.setVideoUrl(nullIfBlank(str(body.get("video_url"))));
        e.setDuration(body.get("duration") != null ? intOrZero(body.get("duration")) : 0);
        dramaEpisodeMapper.insert(e);
        DramaEpisode saved = dramaEpisodeMapper.selectById(e.getId());
        if (saved == null) {
            throw new BusinessException(500, "创建分集失败");
        }
        return episodeToApi(saved);
    }

    @Transactional
    public void updateEpisode(int dramaId, int episodeId, Map<String, Object> body) {
        DramaEpisode existing = dramaEpisodeMapper.selectById(episodeId);
        if (existing == null || existing.getDramaId() == null || existing.getDramaId() != dramaId) {
            throw new BusinessException(404, "分集不存在");
        }
        if (body.containsKey("episode_number") || body.containsKey("episode_num")) {
            int n = episodeNumFromBody(body);
            if (n > 0) {
                existing.setEpisodeNum(n);
            }
        }
        if (body.containsKey("title")) {
            existing.setTitle(nullIfBlank(str(body.get("title"))));
        }
        if (body.containsKey("video_id")) {
            existing.setVideoId(nullIfBlank(str(body.get("video_id"))));
        }
        if (body.containsKey("video_url")) {
            existing.setVideoUrl(nullIfBlank(str(body.get("video_url"))));
        }
        if (body.containsKey("duration")) {
            existing.setDuration(intOrZero(body.get("duration")));
        }
        dramaEpisodeMapper.update(existing);
    }

    @Transactional
    public void deleteEpisode(int dramaId, int episodeId) {
        DramaEpisode existing = dramaEpisodeMapper.selectById(episodeId);
        if (existing == null || existing.getDramaId() == null || existing.getDramaId() != dramaId) {
            throw new BusinessException(404, "分集不存在");
        }
        dramaEpisodeMapper.deleteById(episodeId);
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        Object titleObj = body.get("title");
        if (titleObj == null || titleObj.toString().isBlank()) {
            throw new BusinessException(400, "标题不能为空");
        }
        Drama d = new Drama();
        d.setTitle(titleObj.toString().trim());
        d.setCover(str(body.get("cover_image")));
        d.setDescription(str(body.get("description")));
        d.setCategoryId(intOrNull(body.get("category_id")));
        d.setStatus(toDbStatusSave(str(body.get("status"))));
        d.setTotalEpisodes(intOrZero(body.get("total_episodes")));
        d.setViewCount(intOrZero(body.get("view_count")));
        d.setSort(0);
        d.setFreeEpisodes(intOrZero(body.get("free_episodes")));
        d.setBeansPerEpisode(body.get("beans_per_episode") != null ? intOrZero(body.get("beans_per_episode")) : 5);
        if (d.getViewCount() == null) {
            d.setViewCount(0);
        }
        if (d.getFreeEpisodes() == null) {
            d.setFreeEpisodes(0);
        }
        d.setDisplayName(nullIfBlank(str(body.get("display_name"))));
        d.setDisplayText(nullIfBlank(str(body.get("display_text"))));
        d.setOssPath(nullIfBlank(str(body.get("oss_path"))));
        d.setCategory(nullIfBlank(str(body.get("category"))));
        d.setTaskStatus(str(body.get("task_status")));
        if (d.getTaskStatus() == null) {
            d.setTaskStatus("");
        }
        d.setIsOnline("published".equals(d.getStatus()) ? 1 : 0);
        d.setPublicId(allocateUniquePublicId());
        dramaMapper.insert(d);
        Drama saved = dramaMapper.selectById(d.getId());
        List<Integer> tagIds = intList(body.get("tag_ids"));
        if (tagIds != null) {
            for (Integer tid : tagIds) {
                if (tid == null) {
                    continue;
                }
                DramaTag dt = new DramaTag();
                dt.setDramaId(d.getId());
                dt.setTagId(tid);
                dramaTagMapper.insert(dt);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", d.getId());
        data.put("public_id", saved != null && saved.getPublicId() != null ? saved.getPublicId() : d.getPublicId());
        data.put("title", d.getTitle());
        if (saved != null && saved.getCreatedAt() != null) {
            data.put("created_at", DT.format(saved.getCreatedAt()));
        }
        return data;
    }

    private String allocateUniquePublicId() {
        for (int i = 0; i < 40; i++) {
            String s = randomPublicId15();
            if (dramaMapper.countByPublicId(s) == 0) {
                return s;
            }
        }
        throw new BusinessException(500, "生成业务剧 ID 失败，请重试");
    }

    private static String randomPublicId15() {
        StringBuilder sb = new StringBuilder(15);
        for (int i = 0; i < 15; i++) {
            sb.append(DRAMA_PUBLIC_ID_CHARS.charAt(RANDOM.nextInt(DRAMA_PUBLIC_ID_CHARS.length())));
        }
        return sb.toString();
    }

    @Transactional
    public void update(int id, Map<String, Object> body) {
        Drama existing = dramaMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "短剧不存在");
        }
        if (body.containsKey("title")) {
            String t = str(body.get("title"));
            if (!t.isBlank()) {
                existing.setTitle(t);
            }
        }
        if (body.containsKey("cover_image")) {
            existing.setCover(str(body.get("cover_image")));
        }
        if (body.containsKey("description")) {
            existing.setDescription(str(body.get("description")));
        }
        if (body.containsKey("category_id")) {
            existing.setCategoryId(intOrNull(body.get("category_id")));
        }
        if (body.containsKey("status")) {
            existing.setStatus(toDbStatusSave(str(body.get("status"))));
        }
        if (body.containsKey("total_episodes")) {
            existing.setTotalEpisodes(intOrZero(body.get("total_episodes")));
        }
        if (body.containsKey("view_count")) {
            existing.setViewCount(intOrZero(body.get("view_count")));
        }
        if (body.containsKey("free_episodes")) {
            existing.setFreeEpisodes(intOrZero(body.get("free_episodes")));
        }
        if (body.containsKey("beans_per_episode")) {
            existing.setBeansPerEpisode(intOrZero(body.get("beans_per_episode")));
        }
        if (body.containsKey("sort")) {
            existing.setSort(intOrZero(body.get("sort")));
        }
        if (body.containsKey("is_online")) {
            existing.setIsOnline(intOrZero(body.get("is_online")));
        } else if (body.containsKey("status")) {
            existing.setIsOnline("published".equals(existing.getStatus()) ? 1 : 0);
        }
        if (body.containsKey("display_name")) {
            existing.setDisplayName(nullIfBlank(str(body.get("display_name"))));
        }
        if (body.containsKey("display_text")) {
            existing.setDisplayText(nullIfBlank(str(body.get("display_text"))));
        }
        if (body.containsKey("oss_path")) {
            existing.setOssPath(nullIfBlank(str(body.get("oss_path"))));
        }
        if (body.containsKey("category")) {
            existing.setCategory(nullIfBlank(str(body.get("category"))));
        }
        if (body.containsKey("task_status")) {
            existing.setTaskStatus(str(body.get("task_status")));
            if (existing.getTaskStatus() == null) {
                existing.setTaskStatus("");
            }
        }
        dramaMapper.update(existing);
        dramaCacheSupport.evictById(id);
        if (body.containsKey("tag_ids")) {
            dramaTagMapper.deleteByDramaId(id);
            List<Integer> tagIds = intList(body.get("tag_ids"));
            if (tagIds != null) {
                for (Integer tid : tagIds) {
                    if (tid == null) {
                        continue;
                    }
                    DramaTag dt = new DramaTag();
                    dt.setDramaId(id);
                    dt.setTagId(tid);
                    dramaTagMapper.insert(dt);
                }
            }
        }
    }

    @Transactional
    public void delete(int id) {
        Drama existing = dramaMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "短剧不存在");
        }
        dramaCacheSupport.evictById(id);
        dramaMapper.deleteById(id);
    }

    private Map<String, Object> toApiListItem(Drama d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("public_id", d.getPublicId() != null ? d.getPublicId() : "");
        m.put("title", d.getTitle());
        m.put("description", d.getDescription() != null ? d.getDescription() : "");
        m.put("cover_image", d.getCover() != null ? d.getCover() : "");
        m.put("category_id", d.getCategoryId());
        m.put("category_name", d.getCategoryName() != null ? d.getCategoryName() : "");
        m.put("status", toApiStatus(d.getStatus()));
        m.put("total_episodes", d.getTotalEpisodes() != null ? d.getTotalEpisodes() : 0);
        m.put("view_count", d.getViewCount() != null ? d.getViewCount() : 0);
        m.put("created_at", d.getCreatedAt() != null ? DT.format(d.getCreatedAt()) : "");
        m.put("name", d.getTitle());
        m.put("is_online", d.getIsOnline() != null ? d.getIsOnline() : 0);
        m.put("display_name", d.getDisplayName() != null ? d.getDisplayName() : "");
        m.put("display_text", d.getDisplayText() != null ? d.getDisplayText() : "");
        m.put("oss_path", d.getOssPath() != null ? d.getOssPath() : "");
        m.put("task_status", d.getTaskStatus() != null ? d.getTaskStatus() : "");
        m.put("beans_per_episode", d.getBeansPerEpisode() != null ? d.getBeansPerEpisode() : 0);
        m.put("free_episodes", d.getFreeEpisodes() != null ? d.getFreeEpisodes() : 0);
        return m;
    }

    private Map<String, Object> toApiDetail(Drama d, String categoryName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("public_id", d.getPublicId() != null ? d.getPublicId() : "");
        m.put("title", d.getTitle());
        m.put("description", d.getDescription() != null ? d.getDescription() : "");
        m.put("cover_image", d.getCover() != null ? d.getCover() : "");
        m.put("category_id", d.getCategoryId());
        m.put("category_name", categoryName);
        m.put("status", toApiStatus(d.getStatus()));
        m.put("total_episodes", d.getTotalEpisodes() != null ? d.getTotalEpisodes() : 0);
        m.put("view_count", d.getViewCount() != null ? d.getViewCount() : 0);
        m.put("created_at", d.getCreatedAt() != null ? DT.format(d.getCreatedAt()) : "");
        m.put("beans_per_episode", d.getBeansPerEpisode() != null ? d.getBeansPerEpisode() : 0);
        m.put("free_episodes", d.getFreeEpisodes() != null ? d.getFreeEpisodes() : 0);
        m.put("is_online", d.getIsOnline() != null ? d.getIsOnline() : 0);
        m.put("name", d.getTitle());
        m.put("display_name", d.getDisplayName() != null ? d.getDisplayName() : "");
        m.put("display_text", d.getDisplayText() != null ? d.getDisplayText() : "");
        m.put("oss_path", d.getOssPath() != null ? d.getOssPath() : "");
        m.put("task_status", d.getTaskStatus() != null ? d.getTaskStatus() : "");
        return m;
    }

    /** 查询参数：archived → 映射为 DB 的 offline */
    private static String toDbStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if ("archived".equalsIgnoreCase(status)) {
            return "archived";
        }
        return status;
    }

    /** 保存：archived → offline；允许 draft/published/offline */
    private static String toDbStatusSave(String status) {
        if (status == null || status.isBlank()) {
            return "draft";
        }
        if ("archived".equalsIgnoreCase(status)) {
            return "offline";
        }
        return status;
    }

    private static String toApiStatus(String db) {
        if (db == null) {
            return "";
        }
        if ("offline".equals(db)) {
            return "archived";
        }
        return db;
    }

    private Map<String, Object> episodeToApi(DramaEpisode e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("drama_id", e.getDramaId());
        m.put("episode_number", e.getEpisodeNum());
        m.put("episode_num", e.getEpisodeNum());
        m.put("title", e.getTitle() != null ? e.getTitle() : "");
        m.put("video_id", e.getVideoId() != null ? e.getVideoId() : "");
        m.put("video_url", e.getVideoUrl() != null ? e.getVideoUrl() : "");
        m.put("duration", e.getDuration() != null ? e.getDuration() : 0);
        if (e.getCreatedAt() != null) {
            m.put("created_at", DT.format(e.getCreatedAt()));
        }
        if (e.getUpdatedAt() != null) {
            m.put("updated_at", DT.format(e.getUpdatedAt()));
        }
        return m;
    }

    private static int episodeNumFromBody(Map<String, Object> body) {
        if (body.containsKey("episode_number")) {
            return intOrZero(body.get("episode_number"));
        }
        if (body.containsKey("episode_num")) {
            return intOrZero(body.get("episode_num"));
        }
        return 0;
    }

    private static long nzLong(Long v) {
        return v != null ? v : 0L;
    }

    private static String nullIfBlank(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String str(Object o) {
        return o == null ? "" : Objects.toString(o, "");
    }

    private static Integer intOrNull(Object o) {
        if (o == null || o.toString().isBlank()) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return Integer.parseInt(o.toString().trim());
    }

    private static int intOrZero(Object o) {
        Integer n = intOrNull(o);
        return n != null ? n : 0;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> intList(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof List) {
            List<?> raw = (List<?>) o;
            List<Integer> out = new ArrayList<>();
            for (Object x : raw) {
                if (x == null) {
                    continue;
                }
                if (x instanceof Number) {
                    out.add(((Number) x).intValue());
                } else {
                    out.add(Integer.parseInt(x.toString().trim()));
                }
            }
            return out;
        }
        return null;
    }
}
