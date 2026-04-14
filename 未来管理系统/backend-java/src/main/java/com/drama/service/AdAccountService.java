package com.drama.service;

import com.drama.entity.AdAccount;
import com.drama.entity.TikTokAccount;
import com.drama.exception.BusinessException;
import com.drama.mapper.AdAccountMapper;
import com.drama.mapper.TikTokAccountMapper;
import com.drama.util.EncryptUtil;
import com.drama.util.ExcelExportUtil;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdAccountService {

    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter EXPORT_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CN);
    private static final Map<String, String> COUNTRY_LABEL = Map.of(
            "TH", "泰国",
            "ID", "印尼",
            "VN", "越南",
            "US", "美国");
    private static final Map<String, String> AGENT_LABEL = Map.of(
            "agent_a", "代理商A",
            "agent_b", "代理商B",
            "agent_c", "代理商C",
            "direct", "直营");

    private final AdAccountMapper adAccountMapper;
    private final TikTokAccountMapper tikTokAccountMapper;
    private final EncryptUtil encryptUtil;

    public List<Map<String, String>> entitiesOptions() {
        List<Map<String, String>> merged = new ArrayList<>();
        merged.add(Map.of("label", "测试一", "value", "测试一"));
        merged.add(Map.of("label", "测试二", "value", "测试二"));
        Set<String> seen = new LinkedHashSet<>(List.of("测试一", "测试二"));
        for (String name : adAccountMapper.selectDistinctSubjectNames()) {
            String v = name != null ? name.trim() : "";
            if (!v.isEmpty() && seen.add(v)) {
                merged.add(Map.of("label", v, "value", v));
            }
        }
        return merged;
    }

    public List<String> countries() {
        return adAccountMapper.selectDistinctCountries();
    }

    public List<Map<String, Object>> executableOptions(String media, String oauthStatus) {
        List<Map<String, Object>> rows = adAccountMapper.selectExecutableAccountOptions(media, oauthStatus);
        Map<String, Map<String, Object>> dedup = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String accountId = stringVal(row.get("accountId"));
            if (accountId.isBlank() || dedup.containsKey(accountId)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("media", stringVal(row.get("media")));
            item.put("country", stringVal(row.get("country")));
            item.put("subjectName", stringVal(row.get("subjectName")));
            item.put("accountId", accountId);
            item.put("accountName", stringVal(row.get("accountName")));
            item.put("mediaAlias", stringVal(row.get("mediaAlias")));
            item.put("accountAgent", stringVal(row.get("accountAgent")));
            item.put("advertiserId", stringVal(row.get("advertiserId")));
            item.put("advertiserName", stringVal(row.get("advertiserName")));
            item.put("oauthStatus", stringVal(row.get("oauthStatus")));
            dedup.put(accountId, item);
        }
        return dedup.values().stream()
                .sorted(Comparator.comparing(m -> stringVal(m.get("accountId")).toLowerCase()))
                .toList();
    }

    public Map<String, Object> listPage(int page, int pageSize, Map<String, String> filter) {
        List<AdAccount> filtered =
                filterAccounts(enrichAccountsWithOauth(adAccountMapper.selectAllOrderByIdDesc()), filter);
        int total = filtered.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(from + pageSize, total);
        List<AdAccount> slice = filtered.subList(from, to);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", slice);
        data.put("total", total);
        return data;
    }

    public byte[] exportExcel(Map<String, String> filter) throws IOException {
        List<AdAccount> filtered =
                filterAccounts(enrichAccountsWithOauth(adAccountMapper.selectAllOrderByIdDesc()), filter);
        String[] headers = {"账户媒体", "国家", "账户主体", "账户ID", "账户名称", "账户代理", "创建人", "创建时间"};
        List<Object[]> rows = new ArrayList<>();
        for (AdAccount acc : filtered) {
            rows.add(
                    new Object[] {
                        acc.getMedia() != null ? acc.getMedia() : "",
                        COUNTRY_LABEL.getOrDefault(acc.getCountry(), acc.getCountry() != null ? acc.getCountry() : ""),
                        acc.getSubjectName() != null ? acc.getSubjectName() : "",
                        acc.getAccountId() != null ? acc.getAccountId() : "",
                        acc.getAccountName() != null ? acc.getAccountName() : "",
                        formatAgent(acc.getAccountAgent()),
                        acc.getCreatedByName() != null && !acc.getCreatedByName().isBlank()
                                ? acc.getCreatedByName()
                                : (acc.getCreatedBy() != null ? String.valueOf(acc.getCreatedBy()) : ""),
                        acc.getCreatedAt() != null ? EXPORT_DT.format(acc.getCreatedAt().atZone(CN)) : ""
                    });
        }
        if (rows.isEmpty()) {
            rows.add(new Object[headers.length]);
        }
        return ExcelExportUtil.buildXlsx("账户管理", headers, rows);
    }

    private String formatAgent(String v) {
        if (v == null || v.isEmpty()) {
            return "";
        }
        return AGENT_LABEL.getOrDefault(v, v);
    }

    private List<AdAccount> filterAccounts(List<AdAccount> list, Map<String, String> q) {
        String mediaVal = firstNonBlank(q.get("media"), q.get("platform"));
        List<AdAccount> out = new ArrayList<>(list);
        if (mediaVal != null && !mediaVal.isBlank()) {
            String norm = mapPlatformToMedia(mediaVal);
            out = out.stream()
                    .filter(r -> r.getMedia() != null && r.getMedia().equalsIgnoreCase(norm))
                    .collect(Collectors.toList());
        }
        String country = q.get("country");
        if (country != null && !country.isBlank()) {
            String c = country.trim();
            out = out.stream().filter(r -> c.equals(r.getCountry())).collect(Collectors.toList());
        }
        String alias = q.get("platformAlias");
        if (alias != null && !alias.isBlank()) {
            String a = alias.trim();
            out = out.stream()
                    .filter(r -> r.getMediaAlias() != null && r.getMediaAlias().contains(a))
                    .collect(Collectors.toList());
        }
        String subj = firstNonBlank(q.get("subject"), q.get("entityName"));
        if (subj != null && !subj.isBlank()) {
            String s = subj.trim();
            out = out.stream()
                    .filter(r -> r.getSubjectName() != null && r.getSubjectName().contains(s))
                    .collect(Collectors.toList());
        }
        String accId = firstNonBlank(q.get("accountId"), q.get("spid"));
        if (accId != null && !accId.isBlank()) {
            String s = accId.trim();
            out = out.stream()
                    .filter(r -> r.getAccountId() != null && r.getAccountId().contains(s))
                    .collect(Collectors.toList());
        }
        String accName = q.get("accountName");
        if (accName != null && !accName.isBlank()) {
            String s = accName.trim();
            out = out.stream()
                    .filter(r -> r.getAccountName() != null && r.getAccountName().contains(s))
                    .collect(Collectors.toList());
        }
        String oauthStatus = q.get("oauthStatus");
        if (oauthStatus != null && !oauthStatus.isBlank()) {
            String s = oauthStatus.trim().toLowerCase();
            out = out.stream()
                    .filter(r -> matchesOauthStatus(r, s))
                    .collect(Collectors.toList());
        }
        String keyword = q.get("keyword");
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim().toLowerCase();
            out = out.stream()
                    .filter(
                            r -> (r.getAccountId() != null && r.getAccountId().toLowerCase().contains(k))
                                    || (r.getAccountName() != null && r.getAccountName().toLowerCase().contains(k))
                                    || (r.getSubjectName() != null && r.getSubjectName().toLowerCase().contains(k)))
                    .collect(Collectors.toList());
        }
        return out;
    }

    private List<AdAccount> enrichAccountsWithOauth(List<AdAccount> list) {
        Map<String, TikTokAccount> oauthByAdvertiserId = new LinkedHashMap<>();
        for (TikTokAccount row : tikTokAccountMapper.selectAllOrderByIdAsc()) {
            String advertiserId = row.getAdvertiserId() != null ? row.getAdvertiserId().trim() : "";
            if (!advertiserId.isEmpty()) {
                oauthByAdvertiserId.put(advertiserId, row);
            }
        }
        for (AdAccount account : list) {
            fillOauthFields(account, oauthByAdvertiserId.get(normalizeAccountId(account.getAccountId())));
        }
        return list;
    }

    private void fillOauthFields(AdAccount account, TikTokAccount oauthRow) {
        account.setOauthStatus("");
        account.setOauthAdvertiserName("");
        account.setOauthTokenExpiresAt(null);
        account.setExecutable(Boolean.FALSE);
        if (!isTikTokMedia(account.getMedia())) {
            return;
        }
        if (oauthRow == null) {
            account.setOauthStatus("unauthorized");
            return;
        }
        String status = oauthRow.getStatus() != null ? oauthRow.getStatus().trim() : "";
        account.setOauthStatus(status.isEmpty() ? "unauthorized" : status);
        account.setOauthAdvertiserName(oauthRow.getAdvertiserName() != null ? oauthRow.getAdvertiserName() : "");
        account.setOauthTokenExpiresAt(oauthRow.getTokenExpiresAt());
        account.setExecutable("active".equalsIgnoreCase(status));
    }

    private boolean matchesOauthStatus(AdAccount row, String oauthStatus) {
        if (!isTikTokMedia(row.getMedia())) {
            return false;
        }
        String current = row.getOauthStatus() != null ? row.getOauthStatus().trim().toLowerCase() : "";
        if ("unauthorized".equals(oauthStatus) || "none".equals(oauthStatus)) {
            return current.isEmpty() || "unauthorized".equals(current);
        }
        return current.equals(oauthStatus);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static String mapPlatformToMedia(String v) {
        if (v == null || v.isBlank()) {
            return "";
        }
        String s = v.trim();
        return switch (s.toLowerCase()) {
            case "tiktok" -> "TikTok";
            case "facebook" -> "Facebook";
            case "google" -> "Google";
            case "meta" -> "Meta";
            default -> s;
        };
    }

    private static boolean isTikTokMedia(String media) {
        return media != null && "tiktok".equalsIgnoreCase(media.trim());
    }

    private static String normalizeAccountId(String accountId) {
        return accountId != null ? accountId.trim() : "";
    }

    @Transactional
    public int create(Map<String, Object> body, Integer adminId, String createdByName) {
        AdAccount row = normalizeBody(body);
        row = ensureAccountName(row);
        row.setCreatedBy(adminId != null ? adminId : 0);
        row.setCreatedByName(createdByName != null ? createdByName : "");
        adAccountMapper.insert(row);
        return row.getId();
    }

    @Transactional
    public void update(int id, Map<String, Object> body) {
        AdAccount existing = adAccountMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "记录不存在");
        }
        AdAccount row = normalizeBody(body);
        row = ensureAccountName(row);
        row.setId(id);
        row.setCreatedBy(existing.getCreatedBy());
        row.setCreatedByName(existing.getCreatedByName());
        adAccountMapper.update(row);
    }

    @Transactional
    public void deleteOne(int id) {
        if (adAccountMapper.selectById(id) == null) {
            throw new BusinessException(404, "记录不存在");
        }
        adAccountMapper.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的记录");
        }
        adAccountMapper.deleteByIds(ids);
    }

    private AdAccount ensureAccountName(AdAccount n) {
        if (n.getAccountName() != null && !n.getAccountName().trim().isEmpty()) {
            n.setAccountName(n.getAccountName().trim());
            return n;
        }
        String sid = n.getAccountId() != null ? n.getAccountId() : "";
        String tail = sid.length() >= 4 ? sid.substring(sid.length() - 4) : sid;
        String subj = n.getSubjectName() != null ? n.getSubjectName() : "";
        n.setAccountName(subj + "_" + tail);
        return n;
    }

    private AdAccount normalizeBody(Map<String, Object> body) {
        Object rawMedia = body.get("media");
        if (rawMedia == null) {
            rawMedia = body.get("platform");
        }
        String media = rawMedia != null && !String.valueOf(rawMedia).trim().isEmpty()
                ? mapPlatformToMedia(String.valueOf(rawMedia).trim())
                : "";
        Object sn = body.get("subject_name");
        if (sn == null) {
            sn = body.get("entityName");
        }
        Object aid = body.get("account_id");
        if (aid == null) {
            aid = body.get("spid");
        }
        Object an = body.get("account_name");
        if (an == null) {
            an = body.get("accountName");
        }
        Object ma = body.get("media_alias");
        if (ma == null) {
            ma = body.get("platformAlias");
        }
        Object ag = body.get("account_agent");
        if (ag == null) {
            ag = body.get("accountAgent");
        }
        AdAccount a = new AdAccount();
        a.setMedia(media);
        a.setCountry(body.get("country") != null ? String.valueOf(body.get("country")) : "");
        a.setSubjectName(sn != null ? String.valueOf(sn) : "");
        a.setAccountId(aid != null ? String.valueOf(aid) : "");
        a.setAccountName(an != null ? String.valueOf(an).trim() : "");
        a.setMediaAlias(ma != null ? String.valueOf(ma) : "");
        a.setAccountAgent(ag != null ? String.valueOf(ag) : "");

        // Token 加密
        Object accessToken = body.get("access_token");
        if (accessToken != null && !String.valueOf(accessToken).trim().isEmpty()) {
            String encrypted = encryptUtil.encrypt(String.valueOf(accessToken).trim());
            a.setAccessTokenEncrypted(encrypted);
            log.debug("AccessToken 已加密: accountId={}", aid);
        }
        Object refreshToken = body.get("refresh_token");
        if (refreshToken != null && !String.valueOf(refreshToken).trim().isEmpty()) {
            String encrypted = encryptUtil.encrypt(String.valueOf(refreshToken).trim());
            a.setRefreshTokenEncrypted(encrypted);
        }

        return a;
    }

    public String decryptAccessToken(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return null;
        }
        return encryptUtil.decrypt(encrypted);
    }

    public String decryptRefreshToken(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return null;
        }
        return encryptUtil.decrypt(encrypted);
    }

    private static String stringVal(Object v) {
        return v != null ? String.valueOf(v) : "";
    }
}
