package com.drama.service;

import com.drama.dto.RechargeQueryParam;
import com.drama.dto.RechargeStatsRow;
import com.drama.entity.RechargeRecord;
import com.drama.exception.BusinessException;
import com.drama.mapper.RechargeRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class RechargeService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RechargeRecordMapper rechargeRecordMapper;

    /** 统计（与列表页相同筛选维度；{@code status} 一般不传以统计全量） */
    public Map<String, Object> stats(RechargeQueryParam q) {
        RechargeStatsRow row =
                rechargeRecordMapper.selectStatsAggregate(
                        q.getUserId(),
                        q.getPromotionId(),
                        q.getOrderId(),
                        q.getExternalOrderId(),
                        q.getCountry(),
                        q.getStartDate(),
                        q.getEndDate(),
                        null,
                        q.getPlatform(),
                        q.getAccountId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total_count", nzLong(row != null ? row.getTotalCount() : null));
        m.put("pending_count", nzLong(row != null ? row.getPendingCount() : null));
        m.put("success_count", nzLong(row != null ? row.getSuccessCount() : null));
        m.put("failed_count", nzLong(row != null ? row.getFailedCount() : null));
        m.put("total_amount", row != null && row.getTotalAmount() != null ? row.getTotalAmount() : BigDecimal.ZERO);
        m.put(
                "success_amount",
                row != null && row.getSuccessAmount() != null ? row.getSuccessAmount() : BigDecimal.ZERO);
        m.put("first_recharge_count", nzLong(row != null ? row.getFirstRechargeCount() : null));
        m.put("new_user_count", nzLong(row != null ? row.getNewUserCount() : null));
        m.put("total", m.get("total_count"));
        m.put("pending", m.get("pending_count"));
        return m;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String userId = str(body.get("user_id"));
        if (userId.isBlank()) {
            throw new BusinessException(400, "user_id 不能为空");
        }
        Long id = longOrNull(body.get("id"));
        if (id == null) {
            id = rechargeRecordMapper.selectMaxId() + 1;
        }
        if (rechargeRecordMapper.selectById(id) != null) {
            throw new BusinessException(400, "记录 id 已存在");
        }
        RechargeRecord r = new RechargeRecord();
        r.setId(id);
        applyBodyToRecord(r, body, true);
        if (r.getPaymentStatus() == null || r.getPaymentStatus().isBlank()) {
            r.setPaymentStatus("pending");
        }
        if (r.getPayStatus() == null || r.getPayStatus().isBlank()) {
            r.setPayStatus("pending");
        }
        if (r.getAmount() == null) {
            r.setAmount(BigDecimal.ZERO);
        }
        if (r.getDramaName() == null) {
            r.setDramaName("");
        }
        rechargeRecordMapper.insert(r);
        RechargeRecord saved = rechargeRecordMapper.selectById(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        if (saved != null) {
            data.putAll(mapRow(saved));
        }
        return data;
    }

    @Transactional
    public void update(long id, Map<String, Object> body) {
        RechargeRecord existing = rechargeRecordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "记录不存在");
        }
        applyBodyToRecord(existing, body, false);
        rechargeRecordMapper.updateById(existing);
    }

    @Transactional
    public void delete(long id) {
        RechargeRecord existing = rechargeRecordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "记录不存在");
        }
        rechargeRecordMapper.deleteById(id);
    }

    public Map<String, Object> list(RechargeQueryParam q) {
        int page = Math.max(1, q.getPage());
        int pageSize = Math.min(100, Math.max(1, q.getPageSize()));
        int offset = (page - 1) * pageSize;
        long total =
                rechargeRecordMapper.countByParam(
                        q.getUserId(),
                        q.getPromotionId(),
                        q.getOrderId(),
                        q.getExternalOrderId(),
                        q.getCountry(),
                        q.getStartDate(),
                        q.getEndDate(),
                        q.getStatus(),
                        q.getPlatform(),
                        q.getAccountId());
        List<RechargeRecord> rows =
                rechargeRecordMapper.selectByParam(
                        q.getUserId(),
                        q.getPromotionId(),
                        q.getOrderId(),
                        q.getExternalOrderId(),
                        q.getCountry(),
                        q.getStartDate(),
                        q.getEndDate(),
                        q.getStatus(),
                        q.getPlatform(),
                        q.getAccountId(),
                        offset,
                        pageSize);
        List<Map<String, Object>> list =
                rows.stream().map(r -> mapRow(r)).collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", list);
        return data;
    }

    public Map<String, Object> getById(long id) {
        RechargeRecord r = rechargeRecordMapper.selectById(id);
        if (r == null) {
            throw new BusinessException(404, "记录不存在");
        }
        return mapDetail(r);
    }

    private Map<String, Object> mapRow(RechargeRecord r) {
        String ps = normalizePayStatus(r);
        LocalDateTime orderTime = r.getLocalOrderTime() != null ? r.getLocalOrderTime() : r.getCreatedAt();
        boolean paid = "paid".equals(ps);
        boolean first =
                r.getIsFirstRecharge() != null
                        ? r.getIsFirstRecharge()
                        : (paid
                                && orderTime != null
                                && rechargeRecordMapper.countEarlierPaid(r.getUserId(), orderTime, r.getId())
                                        == 0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("order_id", r.getId());
        m.put("user_id", r.getUserId());
        m.put("drama_name", r.getDramaName() != null ? r.getDramaName() : "");
        m.put("amount", r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO);
        m.put("coins", r.getCoins() != null ? r.getCoins() : 0);
        m.put("payment_status", ps);
        m.put("promotion_id", r.getPromotionId() != null ? r.getPromotionId() : "");
        m.put("new_user_id", r.getNewUserId() != null ? r.getNewUserId() : "");
        m.put("is_first_recharge", first);
        m.put("is_new_user", r.getIsNewUser() != null ? r.getIsNewUser() : false);
        m.put("local_register_time", formatLocal(r.getLocalRegisterTime(), r.getLocalTime(), r.getCreatedAt()));
        m.put("local_order_time", formatLocal(r.getLocalOrderTime(), r.getCreatedAt()));
        m.put("country", r.getCountry() != null ? r.getCountry() : "");
        m.put("external_order_id", r.getExternalOrderId() != null ? r.getExternalOrderId() : "");
        m.put("order_no", r.getOrderNo());
        m.put("platform", r.getPlatform() != null ? r.getPlatform() : "");
        m.put("ad_account_id", r.getAdAccountId() != null ? r.getAdAccountId() : "");
        m.put("ad_account_name", r.getAdAccountName() != null ? r.getAdAccountName() : "");
        m.put("callback_sent", r.getCallbackSent() != null && r.getCallbackSent());
        m.put("created_at", r.getCreatedAt() != null ? DT.format(r.getCreatedAt()) : "");
        m.put(
                "actual_amount",
                paid && r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO);
        return m;
    }

    private Map<String, Object> mapDetail(RechargeRecord r) {
        String ps = normalizePayStatus(r);
        LocalDateTime orderTime = r.getLocalOrderTime() != null ? r.getLocalOrderTime() : r.getCreatedAt();
        boolean paid = "paid".equals(ps);
        boolean first =
                r.getIsFirstRecharge() != null
                        ? r.getIsFirstRecharge()
                        : (paid
                                && orderTime != null
                                && rechargeRecordMapper.countEarlierPaid(r.getUserId(), orderTime, r.getId())
                                        == 0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("user_id", r.getUserId());
        m.put(
                "order_id",
                r.getOrderNo() != null && !r.getOrderNo().isEmpty()
                        ? r.getOrderNo()
                        : String.valueOf(r.getId()));
        m.put("external_order_id", r.getExternalOrderId() != null ? r.getExternalOrderId() : "");
        m.put("amount", r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO);
        m.put("coins", r.getCoins() != null ? r.getCoins() : 0);
        m.put("payment_status", normalizePayStatus(r));
        m.put(
                "payment_method",
                r.getPaymentMethod() != null ? r.getPaymentMethod() : "stripe");
        m.put("platform", r.getPlatform() != null ? r.getPlatform() : "");
        m.put("ad_account_id", r.getAdAccountId() != null ? r.getAdAccountId() : "");
        m.put("ad_account_name", r.getAdAccountName() != null ? r.getAdAccountName() : "");
        m.put("callback_sent", r.getCallbackSent() != null && r.getCallbackSent());
        m.put("country", r.getCountry() != null ? r.getCountry() : "");
        m.put("new_user_id", r.getNewUserId() != null ? r.getNewUserId() : "");
        m.put("is_first_recharge", first);
        m.put("is_new_user", r.getIsNewUser() != null ? r.getIsNewUser() : false);
        m.put("created_at", r.getCreatedAt() != null ? DT.format(r.getCreatedAt()) : "");
        m.put("local_register_time", formatLocal(r.getLocalRegisterTime(), r.getLocalTime(), r.getCreatedAt()));
        m.put("local_order_time", formatLocal(r.getLocalOrderTime(), r.getCreatedAt()));
        return m;
    }

    private String formatLocal(LocalDateTime primary, LocalDateTime... fallbacks) {
        if (primary != null) {
            return DT.format(primary);
        }
        for (LocalDateTime fb : fallbacks) {
            if (fb != null) {
                return DT.format(fb);
            }
        }
        return "";
    }

    private String normalizePayStatus(RechargeRecord r) {
        if ("pending".equalsIgnoreCase(r.getPaymentStatus()) || "paid".equalsIgnoreCase(r.getPaymentStatus())) {
            return r.getPaymentStatus().toLowerCase();
        }
        if ("failed".equalsIgnoreCase(r.getPaymentStatus())) {
            return "failed";
        }
        String s = (r.getPayStatus() != null ? r.getPayStatus() : "").trim().toLowerCase();
        if (s.equals("pending") || s.equals("待支付") || s.equals("unpaid")) {
            return "pending";
        }
        if (s.equals("paid") || s.equals("success") || s.equals("已支付")) {
            return "paid";
        }
        if (s.equals("failed")) {
            return "failed";
        }
        return "paid";
    }

    private void applyBodyToRecord(RechargeRecord r, Map<String, Object> body, boolean forCreate) {
        if (body.containsKey("order_no") || forCreate) {
            r.setOrderNo(str(body.get("order_no")));
        }
        if (body.containsKey("user_id")) {
            r.setUserId(str(body.get("user_id")));
        }
        if (body.containsKey("drama_id")) {
            r.setDramaId(intOrNull(body.get("drama_id")));
        }
        if (body.containsKey("drama_name")) {
            r.setDramaName(str(body.get("drama_name")));
        }
        if (body.containsKey("amount")) {
            r.setAmount(bigDecimalOrZero(body.get("amount")));
        }
        if (body.containsKey("coins")) {
            r.setCoins(intOrNull(body.get("coins")));
        }
        if (body.containsKey("payment_status")) {
            r.setPaymentStatus(str(body.get("payment_status")));
        }
        if (body.containsKey("pay_status")) {
            r.setPayStatus(str(body.get("pay_status")));
        }
        if (body.containsKey("promotion_id")) {
            r.setPromotionId(str(body.get("promotion_id")));
        }
        if (body.containsKey("promote_id")) {
            r.setPromoteId(str(body.get("promote_id")));
        }
        if (body.containsKey("new_user_id")) {
            r.setNewUserId(str(body.get("new_user_id")));
        }
        if (body.containsKey("is_first_recharge")) {
            r.setIsFirstRecharge(boolOrNull(body.get("is_first_recharge")));
        }
        if (body.containsKey("is_new_user")) {
            r.setIsNewUser(boolOrNull(body.get("is_new_user")));
        }
        if (body.containsKey("local_register_time")) {
            r.setLocalRegisterTime(parseDateTime(body.get("local_register_time")));
        }
        if (body.containsKey("local_order_time")) {
            r.setLocalOrderTime(parseDateTime(body.get("local_order_time")));
        }
        if (body.containsKey("local_time")) {
            r.setLocalTime(parseDateTime(body.get("local_time")));
        }
        if (body.containsKey("country")) {
            r.setCountry(str(body.get("country")));
        }
        if (body.containsKey("external_order_id")) {
            r.setExternalOrderId(str(body.get("external_order_id")));
        }
        if (body.containsKey("external_order_no")) {
            r.setExternalOrderNo(str(body.get("external_order_no")));
        }
        if (body.containsKey("payment_method")) {
            r.setPaymentMethod(str(body.get("payment_method")));
        }
        if (body.containsKey("platform")) {
            r.setPlatform(str(body.get("platform")));
        }
        if (body.containsKey("ad_account_id")) {
            r.setAdAccountId(nullIfBlank(str(body.get("ad_account_id"))));
        }
        if (body.containsKey("ad_account_name")) {
            r.setAdAccountName(nullIfBlank(str(body.get("ad_account_name"))));
        }
        if (body.containsKey("callback_sent")) {
            r.setCallbackSent(boolOrNull(body.get("callback_sent")));
        }
    }

    private static String nullIfBlank(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static long nzLong(Long v) {
        return v != null ? v : 0L;
    }

    private static String str(Object o) {
        return o == null ? "" : Objects.toString(o, "");
    }

    private static Long longOrNull(Object o) {
        if (o == null || str(o).isBlank()) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return Long.parseLong(o.toString().trim());
    }

    private static Integer intOrNull(Object o) {
        if (o == null || str(o).isBlank()) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return Integer.parseInt(o.toString().trim());
    }

    private static BigDecimal bigDecimalOrZero(Object o) {
        if (o == null || str(o).isBlank()) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        }
        if (o instanceof Number) {
            return BigDecimal.valueOf(((Number) o).doubleValue());
        }
        return new BigDecimal(o.toString().trim());
    }

    private static Boolean boolOrNull(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue() != 0;
        }
        String s = o.toString().trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "是".equals(s)) {
            return true;
        }
        if ("false".equals(s) || "0".equals(s) || "否".equals(s)) {
            return false;
        }
        return null;
    }

    private static LocalDateTime parseDateTime(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDateTime) {
            return (LocalDateTime) o;
        }
        String s = str(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            if (s.contains("T")) {
                String norm = s.replace('T', ' ');
                int dot = norm.indexOf('.');
                if (dot > 0) {
                    norm = norm.substring(0, dot);
                }
                if (norm.length() > 19) {
                    norm = norm.substring(0, 19);
                }
                return LocalDateTime.parse(norm, DT);
            }
            if (s.length() >= 19) {
                return LocalDateTime.parse(s.substring(0, 19), DT);
            }
            return LocalDateTime.parse(s, DT);
        } catch (Exception ignored) {
            return null;
        }
    }
}
