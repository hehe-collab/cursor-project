package com.drama.service;

import com.drama.dto.RechargePlanGroupSaveRequest;
import com.drama.entity.RechargePlan;
import com.drama.entity.RechargePlanGroup;
import com.drama.entity.RechargePlanGroupPlan;
import com.drama.exception.BusinessException;
import com.drama.mapper.RechargePlanGroupMapper;
import com.drama.mapper.RechargePlanGroupPlanMapper;
import com.drama.mapper.RechargePlanMapper;
import com.drama.util.RechargeGroupPublicId;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RechargePlanGroupService {

    private final RechargePlanGroupMapper groupMapper;
    private final RechargePlanGroupPlanMapper groupPlanMapper;
    private final RechargePlanMapper rechargePlanMapper;
    private final RechargePlanService rechargePlanService;
    private final PixelVerificationService pixelVerificationService;

    private static final SecureRandom RANDOM = new SecureRandom();

    public Map<String, Object> list(int page, int pageSize, String groupId, String groupName) {
        int ps = Math.min(1000, Math.max(1, pageSize));
        int p = Math.max(1, page);
        String gid = groupId != null && !groupId.isBlank() ? groupId.trim() : null;
        String gn = groupName != null && !groupName.isBlank() ? groupName.trim() : null;
        List<RechargePlanGroup> all = groupMapper.selectFiltered(gn, gid, null);
        int total = all.size();
        int offset = (p - 1) * ps;
        List<RechargePlanGroup> slice =
                offset >= total ? List.of() : all.subList(offset, Math.min(offset + ps, total));
        List<Map<String, Object>> list = new ArrayList<>();
        for (RechargePlanGroup g : slice) {
            list.add(toAdminListRow(g));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", total);
        return data;
    }

    public Map<String, Object> detail(int id) {
        RechargePlanGroup g = require(id);
        return toAdminListRow(g);
    }

    @Transactional
    public int create(RechargePlanGroupSaveRequest body, Integer adminId) {
        RechargePlanGroup row = mergeNewGroup(body, adminId);
        groupMapper.insert(row);
        savePlanRelations(row.getId(), resolvePlanIds(body, null));
        return row.getId();
    }

    @Transactional
    public void update(int id, RechargePlanGroupSaveRequest body) {
        RechargePlanGroup prev = require(id);
        RechargePlanGroup row = mergeUpdateGroup(prev, body);
        groupMapper.update(row);
        if (body.getPlanIds() != null || body.getRechargePlanIds() != null) {
            groupPlanMapper.deleteByGroupId(id);
            savePlanRelations(id, resolvePlanIds(body, prev));
        }
    }

    @Transactional
    public void delete(int id) {
        require(id);
        groupMapper.deleteById(id);
    }

    @Transactional
    public int deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        groupMapper.deleteByIds(ids);
        return ids.size();
    }

    public Map<String, Object> testPixel(Map<String, Object> body) {
        Object pid = body != null ? body.get("pixel_id") : null;
        Object pt = body != null ? body.get("pixel_token") : null;
        Object mp = body != null ? body.get("media_platform") : null;
        return pixelVerificationService.verify(
                pid != null ? String.valueOf(pid) : "",
                pt != null ? String.valueOf(pt) : "",
                mp != null ? String.valueOf(mp) : "");
    }

    public List<Map<String, Object>> frontendGroups() {
        List<RechargePlan> activePlans = rechargePlanMapper.selectAllActiveOrderByIdDesc();
        Map<Integer, RechargePlan> planById =
                activePlans.stream().collect(Collectors.toMap(RechargePlan::getId, x -> x, (a, b) -> a));
        List<RechargePlanGroup> groups = groupMapper.selectActiveOrderBySort();
        List<Map<String, Object>> out = new ArrayList<>();
        for (RechargePlanGroup g : groups) {
            List<RechargePlan> ordered = orderedPlansForGroup(g.getId(), planById);
            if (ordered.isEmpty()) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("name", g.getName());
            m.put("group_name", g.getGroupName() != null ? g.getGroupName() : g.getName());
            m.put("media_platform", nullToEmpty(g.getMediaPlatform()));
            m.put("pixel_id", nullToEmpty(g.getPixelId()));
            m.put("description", nullToEmpty(g.getDescription()));
            m.put("sort_order", g.getSortOrder());
            List<Map<String, Object>> plans =
                    ordered.stream().map(rechargePlanService::toFrontendPlanMap).toList();
            m.put("plans", plans);
            out.add(m);
        }
        return out;
    }

    private RechargePlanGroup require(int id) {
        RechargePlanGroup g = groupMapper.selectById(id);
        if (g == null) {
            throw new BusinessException(404, "充值方案组不存在");
        }
        return g;
    }

    private List<RechargePlan> orderedPlansForGroup(int groupId, Map<Integer, RechargePlan> planById) {
        List<RechargePlanGroupPlan> rels = groupPlanMapper.selectByGroupIdOrdered(groupId);
        List<RechargePlan> ordered = new ArrayList<>();
        for (RechargePlanGroupPlan r : rels) {
            RechargePlan p = planById.get(r.getPlanId());
            if (p != null && "active".equalsIgnoreCase(String.valueOf(p.getStatus() != null ? p.getStatus() : "active"))) {
                ordered.add(p);
            }
        }
        return ordered;
    }

    private Map<String, Object> toAdminListRow(RechargePlanGroup g) {
        List<RechargePlanGroupPlan> rels = groupPlanMapper.selectByGroupIdOrdered(g.getId());
        List<Integer> pids = rels.stream().map(RechargePlanGroupPlan::getPlanId).toList();
        List<RechargePlan> planEntities =
                pids.isEmpty() ? List.of() : rechargePlanMapper.selectByIds(pids);
        String fullToken = g.getPixelToken() != null ? g.getPixelToken() : "";
        String nameVal = g.getName() != null ? g.getName() : "";
        String gname = g.getGroupName() != null && !g.getGroupName().isEmpty() ? g.getGroupName() : nameVal;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", g.getId());
        row.put("name", nameVal.isEmpty() ? gname : nameVal);
        row.put("group_name", gname);
        row.put("group_id", g.getGroupPublicId());
        row.put("sort_order", g.getSortOrder());
        row.put("description", g.getDescription() != null ? g.getDescription() : "");
        row.put("status", g.getStatus() != null ? g.getStatus() : "active");
        row.put("group_uuid", g.getGroupUuid() != null ? g.getGroupUuid() : "");
        row.put("item_no", g.getItemNo() != null ? g.getItemNo() : "");
        row.put("item_token", g.getItemToken() != null ? g.getItemToken() : "");
        row.put("media_platform", g.getMediaPlatform() != null ? g.getMediaPlatform() : "");
        row.put("pixel_id", g.getPixelId() != null ? g.getPixelId() : "");
        row.put("pixel_token", fullToken);
        row.put("pixel_token_masked", maskPixelToken(fullToken));
        row.put("creator", g.getCreator() != null ? g.getCreator() : "");
        row.put("created_by", g.getCreatedBy());
        row.put("created_by_name", g.getCreatedByName() != null ? g.getCreatedByName() : "");
        row.put("created_at", g.getCreatedAt());
        row.put("updated_at", g.getUpdatedAt());
        row.put("plan_ids", pids);
        row.put("recharge_plan_ids", pids);
        List<Map<String, Object>> planItems = new ArrayList<>();
        for (RechargePlan p : planEntities) {
            Map<String, Object> api = rechargePlanService.toApiRow(p);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", api.get("id"));
            item.put("name", api.get("name"));
            item.put("amount", api.get("amount"));
            item.put("bean_count", api.get("bean_count"));
            item.put("extra_bean", api.get("extra_bean"));
            item.put("currency", api.get("currency"));
            item.put("pay_platform", api.get("pay_platform"));
            planItems.add(item);
        }
        row.put("plan_items", planItems);
        String joined = planEntities.stream().map(RechargePlan::getName).collect(Collectors.joining("、"));
        row.put("plans", joined.isEmpty() ? "—" : joined);
        row.put(
                "plan_list_csv",
                planEntities.stream().map(RechargePlan::getName).collect(Collectors.joining(", ")));
        return row;
    }

    private static String maskPixelToken(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        String t = token;
        if (t.length() <= 10) {
            return t;
        }
        return t.substring(0, 6) + "..." + t.substring(t.length() - 4);
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private void savePlanRelations(int groupId, List<Integer> planIds) {
        int i = 0;
        for (Integer pid : planIds) {
            if (pid == null) {
                continue;
            }
            RechargePlan p = rechargePlanMapper.selectById(pid);
            if (p == null) {
                continue;
            }
            RechargePlanGroupPlan rel = new RechargePlanGroupPlan();
            rel.setGroupId(groupId);
            rel.setPlanId(pid);
            rel.setSortOrder(i++);
            groupPlanMapper.insert(rel);
        }
    }

    private List<Integer> resolvePlanIds(RechargePlanGroupSaveRequest body, RechargePlanGroup prev) {
        List<Integer> raw =
                body.getPlanIds() != null
                        ? body.getPlanIds()
                        : (body.getRechargePlanIds() != null ? body.getRechargePlanIds() : null);
        if (raw == null) {
            if (prev == null) {
                return List.of();
            }
            List<RechargePlanGroupPlan> rels = groupPlanMapper.selectByGroupIdOrdered(prev.getId());
            return rels.stream().map(RechargePlanGroupPlan::getPlanId).toList();
        }
        List<Integer> out = new ArrayList<>();
        for (Integer x : raw) {
            if (x == null) {
                continue;
            }
            out.add(x);
        }
        return out;
    }

    private RechargePlanGroup mergeNewGroup(RechargePlanGroupSaveRequest item, Integer adminId) {
        String resolvedName =
                item.getGroupName() != null && !item.getGroupName().isBlank()
                        ? item.getGroupName().trim()
                        : (item.getName() != null && !item.getName().isBlank()
                                ? item.getName().trim()
                                : "");
        String publicId =
                item.getGroupId() != null && !item.getGroupId().isBlank()
                        ? item.getGroupId().trim()
                        : RechargeGroupPublicId.generate();
        String creator =
                item.getCreator() != null && !item.getCreator().isBlank()
                        ? item.getCreator().trim()
                        : (item.getCreatedByName() != null && !item.getCreatedByName().isBlank()
                                ? item.getCreatedByName().trim()
                                : "admin");
        RechargePlanGroup row = new RechargePlanGroup();
        row.setName(resolvedName);
        row.setGroupName(resolvedName);
        row.setGroupPublicId(publicId);
        row.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 999);
        row.setDescription(item.getDescription() != null ? item.getDescription() : "");
        row.setStatus(
                "inactive".equalsIgnoreCase(String.valueOf(item.getStatus())) ? "inactive" : "active");
        row.setGroupUuid(
                item.getGroupUuid() != null && !item.getGroupUuid().isBlank()
                        ? item.getGroupUuid()
                        : UUID.randomUUID().toString());
        row.setItemNo(item.getItemNo() != null ? String.valueOf(item.getItemNo()) : "");
        row.setItemToken(
                item.getItemToken() != null && !String.valueOf(item.getItemToken()).isEmpty()
                        ? String.valueOf(item.getItemToken())
                        : randomHex(64));
        row.setMediaPlatform(item.getMediaPlatform() != null ? String.valueOf(item.getMediaPlatform()) : "");
        row.setPixelId(item.getPixelId() != null ? String.valueOf(item.getPixelId()) : "");
        row.setPixelToken(item.getPixelToken() != null ? String.valueOf(item.getPixelToken()) : "");
        row.setCreator(creator);
        row.setCreatedBy(item.getCreatedBy() != null ? item.getCreatedBy() : adminId);
        row.setCreatedByName(
                item.getCreatedByName() != null ? item.getCreatedByName() : creator);
        return row;
    }

    private RechargePlanGroup mergeUpdateGroup(RechargePlanGroup prev, RechargePlanGroupSaveRequest item) {
        String name =
                item.getGroupName() != null && !item.getGroupName().isBlank()
                        ? item.getGroupName().trim()
                        : (item.getName() != null && !item.getName().isBlank()
                                ? item.getName().trim()
                                : prev.getGroupName());
        RechargePlanGroup row = new RechargePlanGroup();
        row.setId(prev.getId());
        row.setName(name);
        row.setGroupName(name);
        row.setGroupPublicId(
                item.getGroupId() != null && !item.getGroupId().isBlank()
                        ? item.getGroupId().trim()
                        : prev.getGroupPublicId());
        row.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : prev.getSortOrder());
        row.setDescription(
                item.getDescription() != null ? item.getDescription() : prev.getDescription());
        row.setStatus(
                item.getStatus() != null
                        ? ("inactive".equalsIgnoreCase(item.getStatus()) ? "inactive" : "active")
                        : prev.getStatus());
        row.setGroupUuid(
                item.getGroupUuid() != null ? item.getGroupUuid() : prev.getGroupUuid());
        row.setItemNo(item.getItemNo() != null ? String.valueOf(item.getItemNo()) : prev.getItemNo());
        row.setItemToken(
                item.getItemToken() != null ? String.valueOf(item.getItemToken()) : prev.getItemToken());
        row.setMediaPlatform(
                item.getMediaPlatform() != null
                        ? String.valueOf(item.getMediaPlatform())
                        : prev.getMediaPlatform());
        row.setPixelId(
                item.getPixelId() != null ? String.valueOf(item.getPixelId()) : prev.getPixelId());
        row.setPixelToken(
                item.getPixelToken() != null ? String.valueOf(item.getPixelToken()) : prev.getPixelToken());
        row.setCreator(prev.getCreator());
        row.setCreatedBy(prev.getCreatedBy());
        row.setCreatedByName(
                item.getCreatedByName() != null ? item.getCreatedByName() : prev.getCreatedByName());
        return row;
    }

    private static String randomHex(int nChars) {
        int n = nChars / 2;
        byte[] buf = new byte[n];
        RANDOM.nextBytes(buf);
        StringBuilder sb = new StringBuilder(nChars);
        for (byte b : buf) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
