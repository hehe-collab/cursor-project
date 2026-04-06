package com.drama.service;

import com.alibaba.fastjson2.JSON;
import com.drama.dto.CallbackLogStatsRow;
import com.drama.entity.CallbackConfig;
import com.drama.entity.CallbackLog;
import com.drama.exception.BusinessException;
import com.drama.mapper.CallbackConfigMapper;
import com.drama.mapper.CallbackLogMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallbackService {

    private static final Map<String, String> STATUS_ALIAS_TO_CN = Map.of(
            "success", "成功",
            "failed", "失败",
            "pending", "待处理");

    private final CallbackConfigMapper callbackConfigMapper;
    private final CallbackLogMapper callbackLogMapper;

    public CallbackLogStatsRow getStats() {
        CallbackLogStatsRow row = callbackLogMapper.selectStats();
        if (row == null) {
            row = new CallbackLogStatsRow();
            row.setTotal(0L);
            row.setSuccess(0L);
            row.setFailed(0L);
            row.setPending(0L);
        }
        return row;
    }

    public Map<String, Object> listConfigs(String platform, String linkId, int page, int pageSize) {
        List<CallbackConfig> all =
                callbackConfigMapper.selectFiltered(emptyToNull(platform), emptyToNull(linkId));
        int total = all.size();
        int p = Math.max(1, page);
        int ps = Math.max(1, pageSize);
        int from = (p - 1) * ps;
        List<CallbackConfig> slice =
                all.subList(Math.min(from, total), Math.min(from + ps, total));
        Map<String, Object> data = new HashMap<>();
        data.put("list", slice);
        data.put("total", total);
        data.put("page", p);
        data.put("pageSize", ps);
        return data;
    }

    public Map<String, Object> listLogs(
            String status,
            String eventType,
            String orderId,
            String dateStart,
            String dateEnd,
            int page,
            int pageSize) {
        String st = normalizeStatusFilter(status);
        String et = emptyToNull(eventType);
        String oid = emptyToNull(orderId);
        String ds = emptyToNull(dateStart);
        String de = emptyToNull(dateEnd);
        int p = Math.max(1, page);
        int ps = Math.max(1, pageSize);
        long total = callbackLogMapper.countFiltered(st, et, oid, ds, de);
        int offset = (p - 1) * ps;
        List<CallbackLog> rows = callbackLogMapper.selectFiltered(st, et, oid, ds, de, offset, ps);
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows.stream().map(this::normalizeLog).collect(Collectors.toList()));
        data.put("total", total);
        data.put("page", p);
        data.put("pageSize", ps);
        return data;
    }

    private Map<String, Object> normalizeLog(CallbackLog row) {
        String orderNo = row.getOrderNo() != null
                ? row.getOrderNo()
                : (row.getOrderId() != null ? row.getOrderId() : "");
        Map<String, Object> m = new HashMap<>();
        m.put("id", row.getId());
        m.put("order_no", orderNo);
        m.put("order_id", row.getOrderId() != null ? row.getOrderId() : orderNo);
        m.put("event", row.getEvent());
        m.put("event_type", row.getEventType());
        m.put("pixel_id", row.getPixelId());
        m.put("status", row.getStatus());
        m.put("error_message", row.getErrorMessage());
        m.put("retry_count", row.getRetryCount());
        m.put("send_time", row.getSendTime() != null ? row.getSendTime() : row.getSentAt());
        m.put("sent_at", row.getSentAt() != null ? row.getSentAt() : row.getSendTime());
        m.put("created_at", row.getCreatedAt());
        return m;
    }

    @Transactional
    public CallbackConfig createOrUpdateConfig(Map<String, Object> body, String creatorName) {
        Object idObj = body.get("id");
        Integer id = idObj != null && !String.valueOf(idObj).isBlank()
                ? Integer.parseInt(String.valueOf(idObj), 10)
                : null;
        String linkId = str(body.get("link_id"));
        String platform = str(body.get("platform"));
        int cold = intVal(body.get("cold_start_count"), 0);
        int minPrice = intVal(body.get("min_price_limit"), 0);
        boolean replenish = replenishFromBody(body);
        boolean touchConfigJson = body.containsKey("config_json") || body.containsKey("strategies");
        if (body.containsKey("strategies")) {
            validateStrategies(body.get("strategies"));
        }
        String configJson = touchConfigJson ? configJsonFromBody(body) : null;

        if (id != null && id > 0) {
            CallbackConfig existing = callbackConfigMapper.selectById(id);
            if (existing == null) {
                return null;
            }
            existing.setLinkId(linkId != null ? linkId : "");
            existing.setPlatform(platform != null ? platform : "");
            existing.setColdStartCount(cold);
            existing.setMinPriceLimit(minPrice);
            existing.setReplenishCallbackEnabled(replenish);
            if (touchConfigJson) {
                existing.setConfigJson(configJson);
            }
            callbackConfigMapper.update(existing);
            return callbackConfigMapper.selectById(id);
        }

        CallbackConfig row = new CallbackConfig();
        row.setLinkId(linkId != null ? linkId : "");
        row.setPlatform(platform != null ? platform : "");
        row.setColdStartCount(cold);
        row.setMinPriceLimit(minPrice);
        row.setReplenishCallbackEnabled(replenish);
        row.setConfigJson(touchConfigJson ? configJson : null);
        row.setCreator(creatorName != null && !creatorName.isBlank() ? creatorName : "admin");
        callbackConfigMapper.insert(row);
        return callbackConfigMapper.selectById(row.getId());
    }

    @Transactional
    public void deleteConfig(int id) {
        callbackConfigMapper.deleteById(id);
    }

    @Transactional
    public void deleteConfigsBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请提供 ids");
        }
        callbackConfigMapper.deleteByIds(ids);
    }

    private String normalizeStatusFilter(String raw) {
        if (raw == null || raw.isBlank() || "全部".equals(raw)) {
            return null;
        }
        return STATUS_ALIAS_TO_CN.getOrDefault(raw.toLowerCase(), raw);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static int intVal(Object o, int def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o), 10);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean replenishFromBody(Map<String, Object> body) {
        Object o = body.get("replenish_callback_enabled");
        if (o == null) {
            return true;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        if (o instanceof Number n) {
            return n.intValue() != 0;
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return true;
        }
        return !"0".equals(s) && !"false".equalsIgnoreCase(s);
    }

    /**
     * 支持 {@code config_json} 字符串，或 {@code strategies} 数组（写入为 {@code {"strategies": ...}}，
     * 条目标准化字段 {@code amount_min}、{@code amount_max}、{@code params} 对象）。
     */
    private static String configJsonFromBody(Map<String, Object> body) {
        Object raw = body.get("config_json");
        if (raw instanceof String s) {
            return s.isBlank() ? null : s.trim();
        }
        if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
            return JSON.toJSONString(raw);
        }
        if (body.get("strategies") != null) {
            List<Map<String, Object>> normalized = normalizeStrategiesForStore(body.get("strategies"));
            return JSON.toJSONString(Map.of("strategies", normalized));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void validateStrategies(Object raw) {
        if (raw == null) {
            return;
        }
        if (!(raw instanceof List<?> list)) {
            throw new BusinessException(400, "strategies 须为 JSON 数组");
        }
        if (list.isEmpty()) {
            throw new BusinessException(400, "请至少保留一条策略");
        }
        int idx = 0;
        for (Object o : list) {
            idx++;
            if (!(o instanceof Map<?, ?> m)) {
                throw new BusinessException(400, "策略 " + idx + " 格式须为对象");
            }
            Map<String, Object> mm = (Map<String, Object>) m;
            Double min = doubleOrNull(mm.get("amount_min"));
            if (min == null) {
                min = doubleOrNull(mm.get("minAmount"));
            }
            Double max = doubleOrNull(mm.get("amount_max"));
            if (max == null) {
                max = doubleOrNull(mm.get("maxAmount"));
            }
            if (min == null) {
                min = 0.0;
            }
            if (max != null && min > max) {
                throw new BusinessException(400, "策略 " + idx + "：最低金额不能大于最高金额");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> normalizeStrategiesForStore(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) {
                continue;
            }
            Map<String, Object> mm = (Map<String, Object>) m;
            double min = nzDouble(mm.get("amount_min"), nzDouble(mm.get("minAmount"), 0.0));
            Double max = doubleOrNull(mm.get("amount_max"));
            if (max == null) {
                max = doubleOrNull(mm.get("maxAmount"));
            }
            Map<String, Object> params = normalizeParams(mm.get("params"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("amount_min", min);
            row.put("amount_max", max);
            row.put("params", params);
            out.add(row);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> normalizeParams(Object raw) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (raw == null) {
            return map;
        }
        if (raw instanceof String s) {
            String t = s.trim();
            if (!t.isEmpty()) {
                map.put("传", t);
            }
            return map;
        }
        if (raw instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                String k = String.valueOf(e.getKey()).trim();
                Object v = e.getValue();
                String sv = v == null ? "" : String.valueOf(v).trim();
                if (!k.isEmpty() && !sv.isEmpty()) {
                    map.put(k, sv);
                }
            }
            return map;
        }
        return map;
    }

    private static Double doubleOrNull(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double nzDouble(Object o, double def) {
        Double d = doubleOrNull(o);
        return d != null ? d : def;
    }
}
