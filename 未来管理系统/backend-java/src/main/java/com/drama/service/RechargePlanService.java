package com.drama.service;

import com.drama.dto.RechargePlanSaveRequest;
import com.drama.entity.RechargePlan;
import com.drama.exception.BusinessException;
import com.drama.mapper.RechargePlanMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RechargePlanService {

    private final RechargePlanMapper rechargePlanMapper;

    public Map<String, Object> list(int page, int pageSize, String name, String id, String paymentPlatform) {
        int ps = Math.min(1000, Math.max(1, pageSize));
        int p = Math.max(1, page);
        int offset = (p - 1) * ps;
        String idToken = id != null && !id.isBlank() ? id.trim() : null;
        String nm = name != null && !name.isBlank() ? name.trim() : null;
        String plat = paymentPlatform != null && !paymentPlatform.isBlank() ? paymentPlatform.trim() : null;
        long total = rechargePlanMapper.countFiltered(nm, idToken, plat, null);
        List<RechargePlan> rows = rechargePlanMapper.selectFilteredPage(nm, idToken, plat, null, offset, ps);
        List<Map<String, Object>> list = rows.stream().map(this::toApiRow).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", total);
        return data;
    }

    public RechargePlan require(int id) {
        RechargePlan row = rechargePlanMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "充值方案不存在");
        }
        return row;
    }

    @Transactional
    public int create(RechargePlanSaveRequest req, Integer adminId, String adminName) {
        RechargePlan row = fromSaveRequest(req, null, adminId, adminName);
        rechargePlanMapper.insert(row);
        return row.getId();
    }

    @Transactional
    public void update(int id, RechargePlanSaveRequest req) {
        RechargePlan existing = require(id);
        RechargePlan row = fromSaveRequest(req, existing, existing.getCreatedBy(), existing.getCreatedByName());
        row.setId(id);
        row.setCreatedBy(existing.getCreatedBy());
        rechargePlanMapper.update(row);
    }

    @Transactional
    public void delete(int id) {
        require(id);
        rechargePlanMapper.deleteById(id);
    }

    /** 供方案组与用户端组装：与 Node enrichRechargePlan 一致 */
    public Map<String, Object> toApiRow(RechargePlan p) {
        if (p == null) {
            return Map.of();
        }
        boolean unlock = Boolean.TRUE.equals(p.getUnlockFullSeries());
        int bean = p.getBeanCount() != null ? p.getBeanCount() : 0;
        int extra = p.getExtraBean() != null ? p.getExtraBean() : 0;
        String plat = p.getPayPlatform() != null ? p.getPayPlatform() : "";
        String desc =
                p.getDescription() != null && !p.getDescription().isEmpty()
                        ? p.getDescription()
                        : (p.getRechargeInfo() != null ? p.getRechargeInfo() : "");
        String info =
                p.getRechargeInfo() != null && !p.getRechargeInfo().isEmpty()
                        ? p.getRechargeInfo()
                        : desc;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("unlock_full_series", unlock);
        m.put("bean_count", bean);
        m.put("extra_bean", extra);
        m.put("actual_coins", bean);
        m.put("bonus_coins", extra);
        m.put("payment_platform", plat);
        m.put("pay_platform", plat);
        m.put("currency", p.getCurrency() != null ? p.getCurrency() : "USD");
        m.put("status", p.getStatus() != null ? p.getStatus() : "active");
        m.put("description", desc);
        m.put("recharge_info", info);
        m.put("is_recommended", Boolean.TRUE.equals(p.getIsRecommended()));
        m.put("is_hot", Boolean.TRUE.equals(p.getIsHot()));
        m.put("amount", p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
        m.put("uuid", p.getPlanUuid());
        m.put("display_id", p.getPlanUuid() != null ? p.getPlanUuid() : String.valueOf(p.getId()));
        m.put("created_at", p.getCreatedAt());
        m.put("updated_at", p.getUpdatedAt());
        m.put("created_by", p.getCreatedBy());
        m.put("created_by_name", p.getCreatedByName());
        return m;
    }

    public Map<String, Object> toFrontendPlanMap(RechargePlan p) {
        Map<String, Object> m = toApiRow(p);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", m.get("id"));
        o.put("name", m.get("name"));
        o.put("amount", m.get("amount"));
        o.put("currency", m.get("currency"));
        o.put("bean_count", m.get("bean_count"));
        o.put("extra_bean", m.get("extra_bean"));
        o.put("coins", m.get("bean_count"));
        o.put("bonus_coins", m.get("bonus_coins"));
        o.put("actual_coins", m.get("actual_coins"));
        o.put("unlock_full_series", m.get("unlock_full_series"));
        o.put("description", m.get("description"));
        o.put("pay_platform", m.get("pay_platform"));
        o.put("payment_platform", m.get("payment_platform"));
        o.put("recharge_info", m.get("recharge_info"));
        o.put("is_recommended", m.get("is_recommended"));
        o.put("is_hot", m.get("is_hot"));
        o.put("status", m.get("status"));
        return o;
    }

    private RechargePlan fromSaveRequest(
            RechargePlanSaveRequest body, RechargePlan prev, Integer adminId, String adminName) {
        Integer bean =
                body.getActualCoins() != null
                        ? body.getActualCoins()
                        : (body.getBeanCount() != null ? body.getBeanCount() : (prev != null ? prev.getBeanCount() : 0));
        int extra =
                body.getBonusCoins() != null
                        ? body.getBonusCoins()
                        : (body.getExtraBean() != null
                                ? body.getExtraBean()
                                : (prev != null ? prev.getExtraBean() : 0));
        String pay =
                body.getPaymentPlatform() != null
                        ? body.getPaymentPlatform()
                        : (body.getPayPlatform() != null
                                ? body.getPayPlatform()
                                : (prev != null ? prev.getPayPlatform() : ""));
        String info =
                body.getRechargeInfo() != null && !body.getRechargeInfo().isEmpty()
                        ? body.getRechargeInfo()
                        : (body.getDescription() != null ? body.getDescription() : "");
        boolean unlock =
                body.getUnlockFullSeries() != null
                        ? Boolean.TRUE.equals(body.getUnlockFullSeries())
                        : (prev != null && Boolean.TRUE.equals(prev.getUnlockFullSeries()));
        String st =
                "inactive".equalsIgnoreCase(String.valueOf(body.getStatus()))
                        ? "inactive"
                        : (body.getStatus() != null
                                ? body.getStatus()
                                : (prev != null ? prev.getStatus() : "active"));
        RechargePlan row = new RechargePlan();
        row.setName(body.getName() != null ? body.getName() : (prev != null ? prev.getName() : ""));
        row.setUnlockFullSeries(unlock);
        row.setBeanCount(bean != null ? bean : 0);
        row.setExtraBean(extra);
        row.setAmount(body.getAmount() != null ? body.getAmount() : (prev != null ? prev.getAmount() : BigDecimal.ZERO));
        row.setRechargeInfo(info);
        row.setDescription(body.getDescription() != null ? body.getDescription() : info);
        row.setPayPlatform(pay != null ? pay : "");
        row.setCurrency(body.getCurrency() != null ? body.getCurrency() : (prev != null ? prev.getCurrency() : "USD"));
        row.setStatus(st);
        row.setIsRecommended(
                body.getIsRecommended() != null
                        ? Boolean.TRUE.equals(body.getIsRecommended())
                        : Boolean.TRUE.equals(prev != null ? prev.getIsRecommended() : false));
        row.setIsHot(
                body.getIsHot() != null
                        ? Boolean.TRUE.equals(body.getIsHot())
                        : Boolean.TRUE.equals(prev != null ? prev.getIsHot() : false));
        String uuid =
                prev != null && prev.getPlanUuid() != null && !prev.getPlanUuid().isEmpty()
                        ? prev.getPlanUuid()
                        : UUID.randomUUID().toString();
        row.setPlanUuid(uuid);
        row.setCreatedBy(adminId != null ? adminId : (prev != null ? prev.getCreatedBy() : null));
        row.setCreatedByName(
                adminName != null && !adminName.isEmpty()
                        ? adminName
                        : (prev != null ? prev.getCreatedByName() : ""));
        return row;
    }
}
