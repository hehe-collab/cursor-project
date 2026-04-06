package com.drama.service;

import com.drama.entity.Admin;
import com.drama.entity.Drama;
import com.drama.entity.PromotionLink;
import com.drama.exception.BusinessException;
import com.drama.mapper.AdminMapper;
import com.drama.mapper.DramaMapper;
import com.drama.mapper.PromotionLinkMapper;
import com.drama.util.ExcelExportUtil;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionLinkService {

    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter EXPORT_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CN);
    private static final String PROMO_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final PromotionLinkMapper promotionLinkMapper;
    private final AdminMapper adminMapper;
    private final DramaMapper dramaMapper;
    private final SecureRandom random = new SecureRandom();

    public Map<String, Object> listPage(
            int page,
            int pageSize,
            String promoId,
            String dramaId,
            String media,
            String country,
            String promoteName,
            String promoDomain) {
        List<PromotionLink> filtered =
                filterLinks(promotionLinkMapper.selectAllOrderByIdDesc(), promoId, dramaId, media, country, promoteName, promoDomain);
        int total = filtered.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(from + pageSize, total);
        Map<Integer, String> dramaPublicMap = dramaPublicMapForLinks(filtered);
        List<Map<String, Object>> slice =
                filtered.subList(from, to).stream()
                        .map(r -> toClientRow(r, dramaPublicMap))
                        .collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", slice);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    public Map<String, Object> getById(int id) {
        PromotionLink row = promotionLinkMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "记录不存在");
        }
        return toClientRow(row, dramaPublicMapForLinks(List.of(row)));
    }

    /** 回传配置等：远程搜索推广链接（全量内存筛选，链接极多时可再改为分页 SQL）。 */
    public List<Map<String, Object>> searchOptions(String keyword, int limit) {
        String k = keyword != null ? keyword.trim().toLowerCase() : "";
        int lim = limit <= 0 ? 30 : Math.min(limit, 100);
        List<PromotionLink> all = promotionLinkMapper.selectAllOrderByIdDesc();
        return all.stream()
                .filter(r -> k.isEmpty() || linkMatchesKeyword(r, k))
                .limit(lim)
                .map(this::toSearchOptionRow)
                .collect(Collectors.toList());
    }

    public byte[] exportExcel(
            String promoId,
            String dramaId,
            String media,
            String country,
            String promoteName,
            String promoDomain)
            throws java.io.IOException {
        List<PromotionLink> filtered =
                filterLinks(promotionLinkMapper.selectAllOrderByIdDesc(), promoId, dramaId, media, country, promoteName, promoDomain);
        filtered.sort(Comparator.comparing(PromotionLink::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .reversed());
        Map<Integer, String> dramaPublicMap = dramaPublicMapForLinks(filtered);
        String[] headers = {
            "推广ID", "剧的ID", "投放媒体", "国家/地区", "推广名称", "剧名", "方案组ID", "金豆数", "免费集数", "预览集数",
            "推广域名", "创建人", "创建时间"
        };
        List<Object[]> rows = new ArrayList<>();
        for (PromotionLink r : filtered) {
            Map<String, Object> c = toClientRow(r, dramaPublicMap);
            Object dramaDisplay =
                    firstNonBlankObj(c.get("drama_public_id"), c.get("drama_id"));
            rows.add(
                    new Object[] {
                        c.get("promo_id"),
                        dramaDisplay,
                        platformLabelForExport((String) c.get("media")),
                        c.get("country"),
                        c.get("promo_name"),
                        c.get("drama_name"),
                        c.get("plan_group_id"),
                        c.get("beans_per_episode"),
                        c.get("free_episodes"),
                        c.get("preview_episodes"),
                        c.get("promo_domain"),
                        c.get("created_by"),
                        c.get("created_at")
                    });
        }
        if (rows.isEmpty()) {
            rows.add(new Object[headers.length]);
        }
        return ExcelExportUtil.buildXlsx("投放链接", headers, rows);
    }

    private static String platformLabelForExport(String mediaLower) {
        if (mediaLower == null || mediaLower.isEmpty()) return "";
        return switch (mediaLower) {
            case "tiktok" -> "TikTok";
            case "meta" -> "Meta";
            case "google" -> "Google";
            case "facebook" -> "Facebook";
            default -> mediaLower;
        };
    }

    @Transactional
    public void batchDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请提供要删除的ID列表");
        }
        promotionLinkMapper.deleteByIds(ids);
    }

    @Transactional
    public void batchStatus(List<Integer> ids, boolean enabled) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请提供要更新的ID列表");
        }
        String st = enabled ? "active" : "inactive";
        for (int id : ids) {
            PromotionLink row = promotionLinkMapper.selectById(id);
            if (row != null) {
                row.setStatus(st);
                promotionLinkMapper.update(row);
            }
        }
    }

    @Transactional
    public Map<String, Object> copy(int sourceId, String newName, Integer adminId) {
        PromotionLink source = promotionLinkMapper.selectById(sourceId);
        if (source == null) {
            throw new BusinessException(404, "原记录不存在");
        }
        String creator = resolveCreatorLabel(adminId);
        PromotionLink row = new PromotionLink();
        copyFieldsForDuplicate(source, row);
        row.setPromoteId(generateUniquePromoId());
        row.setPromoteName(newName != null && !newName.isBlank() ? newName.trim() : source.getPromoteName());
        row.setCreatedBy(creator);
        row.setStatus("active");
        promotionLinkMapper.insert(row);
        PromotionLink saved = promotionLinkMapper.selectById(row.getId());
        return saved != null ? toClientRow(saved, dramaPublicMapForLinks(List.of(saved))) : Map.of("id", row.getId());
    }

    private void copyFieldsForDuplicate(PromotionLink src, PromotionLink dst) {
        dst.setPlatform(src.getPlatform());
        dst.setCountry(src.getCountry());
        dst.setDramaId(src.getDramaId());
        dst.setPlanGroupId(src.getPlanGroupId());
        dst.setBeanCount(src.getBeanCount());
        dst.setFreeEpisodes(src.getFreeEpisodes());
        dst.setPreviewEpisodes(src.getPreviewEpisodes());
        dst.setDomain(src.getDomain());
        dst.setDramaName(src.getDramaName());
        dst.setStat(src.getStat());
        dst.setAmount(src.getAmount());
        dst.setSpend(src.getSpend());
        dst.setTarget(src.getTarget());
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, Integer adminId) {
        String pid = generateUniquePromoId();
        String creator = resolveCreatorLabel(adminId);
        PromotionLink row = new PromotionLink();
        row.setPromoteId(pid);
        row.setPlatform(normalizePlatformFromMedia(stringVal(body.get("media"))));
        if (row.getPlatform().isEmpty()) {
            row.setPlatform("TikTok");
        }
        row.setCountry(stringVal(body.get("country")));
        row.setPromoteName(stringVal(body.get("promo_name")));
        row.setDramaId(intOrNull(body.get("drama_id")));
        row.setPlanGroupId(intOrNull(body.get("plan_group_id")));
        row.setBeanCount(intOrZero(body.get("beans_per_episode"), body.get("bean_count")));
        row.setFreeEpisodes(intOrZero(body.get("free_episodes")));
        row.setPreviewEpisodes(intOrZero(body.get("preview_episodes")));
        row.setDomain(firstNonBlank(stringVal(body.get("promo_domain")), stringVal(body.get("domain"))));
        row.setDramaName(stringVal(body.get("drama_name")));
        row.setStatus("active");
        row.setCreatedBy(creator);
        promotionLinkMapper.insert(row);
        PromotionLink saved = promotionLinkMapper.selectById(row.getId());
        return saved != null ? toClientRow(saved, dramaPublicMapForLinks(List.of(saved))) : Map.of("id", row.getId());
    }

    @Transactional
    public Map<String, Object> update(int id, Map<String, Object> body) {
        PromotionLink existing = promotionLinkMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "记录不存在");
        }
        String oldPid = existing.getPromoteId();
        applyBody(existing, body);
        if (!Objects.equals(oldPid, existing.getPromoteId())
                && promotionLinkMapper.countByPromoteIdExcludeId(existing.getPromoteId(), id) > 0) {
            throw new BusinessException(400, "推广ID已存在");
        }
        promotionLinkMapper.update(existing);
        PromotionLink saved = Objects.requireNonNull(promotionLinkMapper.selectById(id));
        return toClientRow(saved, dramaPublicMapForLinks(List.of(saved)));
    }

    @Transactional
    public void deleteOne(int id) {
        PromotionLink existing = promotionLinkMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "记录不存在");
        }
        promotionLinkMapper.deleteById(id);
    }

    private void applyBody(PromotionLink e, Map<String, Object> body) {
        if (body.containsKey("promo_name")) {
            e.setPromoteName(stringVal(body.get("promo_name")));
        }
        if (body.containsKey("media")) {
            String p = normalizePlatformFromMedia(stringVal(body.get("media")));
            if (!p.isEmpty()) {
                e.setPlatform(p);
            }
        }
        if (body.containsKey("platform") && !body.containsKey("media")) {
            e.setPlatform(stringVal(body.get("platform")));
        }
        if (body.containsKey("country")) {
            e.setCountry(stringVal(body.get("country")));
        }
        if (body.containsKey("drama_id")) {
            e.setDramaId(intOrNull(body.get("drama_id")));
        }
        if (body.containsKey("plan_group_id")) {
            e.setPlanGroupId(intOrNull(body.get("plan_group_id")));
        }
        if (body.containsKey("beans_per_episode")) {
            e.setBeanCount(intOrZero(body.get("beans_per_episode")));
        }
        if (body.containsKey("bean_count")) {
            e.setBeanCount(intOrZero(body.get("bean_count")));
        }
        if (body.containsKey("free_episodes")) {
            e.setFreeEpisodes(intOrZero(body.get("free_episodes")));
        }
        if (body.containsKey("preview_episodes")) {
            e.setPreviewEpisodes(intOrZero(body.get("preview_episodes")));
        }
        if (body.containsKey("promo_domain")) {
            e.setDomain(stringVal(body.get("promo_domain")));
        }
        if (body.containsKey("domain") && !body.containsKey("promo_domain")) {
            e.setDomain(stringVal(body.get("domain")));
        }
        if (body.containsKey("drama_name")) {
            e.setDramaName(stringVal(body.get("drama_name")));
        }
        if (body.containsKey("promo_id")) {
            e.setPromoteId(stringVal(body.get("promo_id")));
        }
        if (body.containsKey("promote_id")) {
            e.setPromoteId(stringVal(body.get("promote_id")));
        }
        if (body.containsKey("enabled")) {
            Object en = body.get("enabled");
            boolean on = en instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(en));
            e.setStatus(on ? "active" : "inactive");
        }
    }

    private List<PromotionLink> filterLinks(
            List<PromotionLink> list,
            String promoId,
            String dramaId,
            String media,
            String country,
            String promoteName,
            String promoDomain) {
        List<PromotionLink> out = new ArrayList<>(list);
        if (promoId != null && !promoId.isBlank()) {
            String p = promoId.trim();
            out = out.stream().filter(r -> (r.getPromoteId() != null && r.getPromoteId().contains(p))).collect(Collectors.toList());
        }
        if (media != null && !media.isBlank()) {
            String m = media.trim().toLowerCase();
            out = out.stream()
                    .filter(r -> (r.getPlatform() != null && r.getPlatform().toLowerCase().equals(m)))
                    .collect(Collectors.toList());
        }
        if (promoteName != null && !promoteName.isBlank()) {
            String p = promoteName.trim();
            out = out.stream()
                    .filter(r -> r.getPromoteName() != null && r.getPromoteName().contains(p))
                    .collect(Collectors.toList());
        }
        if (dramaId != null && !dramaId.isBlank()) {
            Integer did = parseIntFlexible(dramaId);
            if (did != null) {
                final int d = did;
                out = out.stream().filter(r -> r.getDramaId() != null && r.getDramaId() == d).collect(Collectors.toList());
            }
        }
        if (country != null && !country.isBlank()) {
            String c = country.trim();
            out = out.stream().filter(r -> c.equals(r.getCountry())).collect(Collectors.toList());
        }
        if (promoDomain != null && !promoDomain.isBlank()) {
            String d = promoDomain.trim();
            out = out.stream()
                    .filter(r -> r.getDomain() != null && r.getDomain().contains(d))
                    .collect(Collectors.toList());
        }
        return out;
    }

    private boolean linkMatchesKeyword(PromotionLink r, String k) {
        if (containsIc(r.getPromoteId(), k)) {
            return true;
        }
        if (containsIc(r.getPromoteName(), k)) {
            return true;
        }
        if (r.getId() != null && String.valueOf(r.getId()).contains(k)) {
            return true;
        }
        return r.getDramaId() != null && String.valueOf(r.getDramaId()).contains(k);
    }

    private static boolean containsIc(String a, String k) {
        return a != null && a.toLowerCase().contains(k);
    }

    private Map<String, Object> toSearchOptionRow(PromotionLink r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("promo_id", r.getPromoteId() != null ? r.getPromoteId() : "");
        m.put("promo_name", r.getPromoteName() != null ? r.getPromoteName() : "");
        String name = r.getPromoteName() != null ? r.getPromoteName() : "";
        m.put("label", name.isEmpty() ? ("链接 #" + r.getId()) : (name + " (#" + r.getId() + ")"));
        return m;
    }

    private Map<Integer, String> dramaPublicMapForLinks(List<PromotionLink> links) {
        List<Integer> ids =
                links.stream().map(PromotionLink::getDramaId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Drama> rows = dramaMapper.selectPublicIdPairsByIds(ids);
        Map<Integer, String> m = new HashMap<>();
        for (Drama d : rows) {
            if (d.getId() != null) {
                m.put(d.getId(), d.getPublicId() != null ? d.getPublicId() : "");
            }
        }
        return m;
    }

    private Map<String, Object> toClientRow(PromotionLink r, Map<Integer, String> dramaPublicIds) {
        String plat = r.getPlatform() != null ? r.getPlatform() : "";
        String mediaLower = plat.isEmpty() ? "tiktok" : plat.toLowerCase();
        String createdAtStr = "";
        if (r.getCreatedAt() != null) {
            createdAtStr = EXPORT_DT.format(r.getCreatedAt().atZone(CN));
        }
        String createdBy = r.getCreatedBy() != null ? r.getCreatedBy() : "";
        if ("0".equals(createdBy)) {
            createdBy = "";
        }
        String st = r.getStatus() != null ? r.getStatus() : "active";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("promo_id", r.getPromoteId() != null ? r.getPromoteId() : "");
        m.put("drama_id", r.getDramaId());
        String dramaPub = "";
        if (r.getDramaId() != null && dramaPublicIds != null) {
            dramaPub = dramaPublicIds.getOrDefault(r.getDramaId(), "");
        }
        m.put("drama_public_id", dramaPub);
        m.put("media", mediaLower);
        m.put("country", r.getCountry() != null ? r.getCountry() : "");
        m.put("promo_name", r.getPromoteName() != null ? r.getPromoteName() : "");
        m.put("drama_name", r.getDramaName() != null ? r.getDramaName() : "");
        m.put("plan_group_id", r.getPlanGroupId());
        m.put("beans_per_episode", r.getBeanCount() != null ? r.getBeanCount() : 0);
        m.put("free_episodes", r.getFreeEpisodes() != null ? r.getFreeEpisodes() : 0);
        m.put("preview_episodes", r.getPreviewEpisodes() != null ? r.getPreviewEpisodes() : 0);
        m.put("promo_domain", r.getDomain() != null ? r.getDomain() : "");
        m.put("created_by", createdBy);
        m.put("created_at", createdAtStr);
        m.put("enabled", "active".equals(st));
        return m;
    }

    private static Object firstNonBlankObj(Object a, Object b) {
        if (a != null) {
            String s = String.valueOf(a).trim();
            if (!s.isEmpty()) {
                return a;
            }
        }
        return b;
    }

    private String normalizePlatformFromMedia(String media) {
        if (media == null || media.isEmpty()) {
            return "";
        }
        String m = media.trim().toLowerCase();
        return switch (m) {
            case "tiktok" -> "TikTok";
            case "meta" -> "Meta";
            case "google" -> "Google";
            case "facebook" -> "Facebook";
            default -> media.trim();
        };
    }

    private String generateUniquePromoId() {
        List<PromotionLink> all = promotionLinkMapper.selectAllOrderByIdDesc();
        Set<String> ids = all.stream().map(PromotionLink::getPromoteId).filter(Objects::nonNull).collect(Collectors.toSet());
        for (int i = 0; i < 20; i++) {
            String pid = randomPromoId();
            if (!ids.contains(pid)) {
                return pid;
            }
        }
        return randomPromoId();
    }

    private String randomPromoId() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(PROMO_CHARS.charAt(random.nextInt(PROMO_CHARS.length())));
        }
        return sb.toString();
    }

    private static String stringVal(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isEmpty()) return a;
        return b != null ? b : "";
    }

    private static Integer intOrNull(Object o) {
        if (o == null || "".equals(String.valueOf(o).trim())) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseIntFlexible(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static int intOrZero(Object... candidates) {
        for (Object o : candidates) {
            if (o == null) {
                continue;
            }
            if (o instanceof Number n) {
                return n.intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(o).trim());
            } catch (NumberFormatException ignored) {
                // next
            }
        }
        return 0;
    }

    private String resolveCreatorLabel(Integer adminId) {
        if (adminId == null) {
            return "admin";
        }
        Admin a = adminMapper.selectById(adminId);
        if (a == null) {
            return String.valueOf(adminId);
        }
        if (a.getNickname() != null && !a.getNickname().isBlank()) {
            return a.getNickname().trim();
        }
        if (a.getUsername() != null && !a.getUsername().isBlank()) {
            return a.getUsername().trim();
        }
        return String.valueOf(adminId);
    }
}
