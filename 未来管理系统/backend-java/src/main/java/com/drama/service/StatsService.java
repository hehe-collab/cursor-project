package com.drama.service;

import com.drama.dto.StatsDailyAggRow;
import com.drama.mapper.StatsMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    public Map<String, Object> dailyRechargeStats(String dramaIdRaw, String media, int page, int pageSize) {
        Integer dramaId = null;
        if (dramaIdRaw != null && !dramaIdRaw.isBlank()) {
            try {
                dramaId = Integer.parseInt(dramaIdRaw, 10);
            } catch (NumberFormatException ignored) {
                dramaId = null;
            }
        }
        List<StatsDailyAggRow> rows = statsMapper.selectRechargeDailyAgg(dramaId, media);
        List<Map<String, Object>> list = new ArrayList<>();
        for (StatsDailyAggRow r : rows) {
            Map<String, Object> row = new HashMap<>();
            String time = r.getStatDate() != null ? r.getStatDate().toString() : "";
            row.put("time", time);
            long orderCount = r.getOrderCount() != null ? r.getOrderCount() : 0;
            row.put("orderCount", orderCount);
            BigDecimal amt = r.getRechargeAmount() != null ? r.getRechargeAmount() : BigDecimal.ZERO;
            row.put("rechargeAmount", amt);
            long consume = Math.round(amt.doubleValue() * 0.6);
            row.put("consume", consume);
            if (consume > 0) {
                row.put("roi", amt.divide(BigDecimal.valueOf(consume), 2, RoundingMode.HALF_UP).toPlainString());
            } else {
                row.put("roi", "-");
            }
            list.add(row);
        }
        int total = list.size();
        int p = Math.max(1, page);
        int ps = Math.max(1, pageSize);
        int from = (p - 1) * ps;
        List<Map<String, Object>> pageList =
                list.subList(Math.min(from, total), Math.min(from + ps, total));
        Map<String, Object> data = new HashMap<>();
        data.put("list", pageList);
        data.put("total", total);
        return data;
    }
}
