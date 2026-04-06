package com.drama.service;

import com.drama.mapper.DashboardMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DashboardMapper dashboardMapper;

    public Map<String, Object> stats(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(4);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        if (end.isBefore(start)) {
            LocalDate t = start;
            start = end;
            end = t;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total_users", dashboardMapper.countUsers());
        data.put("total_dramas", dashboardMapper.countDramas());
        data.put("total_recharge", dashboardMapper.countRecharges());
        data.put(
                "total_amount",
                round2(dashboardMapper.sumRechargeAmountPaid()));
        LocalDate today = LocalDate.now();
        data.put("today_users", dashboardMapper.countUsersOnDate(today));
        data.put("today_recharge", dashboardMapper.countRechargesOnDate(today));
        data.put(
                "today_amount",
                round2(dashboardMapper.sumRechargeAmountPaidOnDate(today)));

        Map<String, Long> uMap = toCountMap(dashboardMapper.usersByDateRange(start, end));
        Map<String, Long> rMap = toCountMap(dashboardMapper.rechargesByDateRange(start, end));
        Map<String, BigDecimal> aMap = toAmountMap(dashboardMapper.amountPaidByDateRange(start, end));

        List<String> dates = new ArrayList<>();
        List<Long> users = new ArrayList<>();
        List<Long> recharge = new ArrayList<>();
        List<BigDecimal> amount = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            String key = d.format(DAY);
            dates.add(key);
            users.add(uMap.getOrDefault(key, 0L));
            recharge.add(rMap.getOrDefault(key, 0L));
            amount.add(round2(aMap.getOrDefault(key, BigDecimal.ZERO)));
        }
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("dates", dates);
        chart.put("users", users);
        chart.put("recharge", recharge);
        chart.put("amount", amount);
        data.put("chart_data", chart);

        Map<String, Long> rechargeStatusDist = new LinkedHashMap<>();
        rechargeStatusDist.put("success", dashboardMapper.countRechargePaidInRange(start, end));
        rechargeStatusDist.put("pending", dashboardMapper.countRechargePendingInRange(start, end));
        rechargeStatusDist.put("failed", dashboardMapper.countRechargeOtherInRange(start, end));
        data.put("recharge_status_dist", rechargeStatusDist);

        return data;
    }

    /**
     * 按日趋势（最近 {@code days} 天，含今天）：{@code user} / {@code recharge} / {@code view}。 播放趋势取自表
     * {@code daily_stats}（metric 为 views 等），无埋点数据时为 0。
     */
    public Map<String, Object> trends(String type, Integer days) {
        int d = (days == null || days <= 0) ? 7 : Math.min(366, days);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(d - 1);
        String t = type != null ? type.trim().toLowerCase() : "user";

        List<Map<String, Object>> rows = new ArrayList<>();
        switch (t) {
            case "recharge":
                {
                    Map<String, Long> rc = toCountMap(dashboardMapper.rechargesByDateRange(start, end));
                    Map<String, BigDecimal> am = toAmountMap(dashboardMapper.amountPaidByDateRange(start, end));
                    for (LocalDate cur = start; !cur.isAfter(end); cur = cur.plusDays(1)) {
                        String key = cur.format(DAY);
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("date", key);
                        row.put("count", rc.getOrDefault(key, 0L));
                        row.put("amount", round2(am.getOrDefault(key, BigDecimal.ZERO)));
                        rows.add(row);
                    }
                    break;
                }
            case "view":
                {
                    Map<String, Long> vm = toCountMap(dashboardMapper.viewsByDateRange(start, end));
                    for (LocalDate cur = start; !cur.isAfter(end); cur = cur.plusDays(1)) {
                        String key = cur.format(DAY);
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("date", key);
                        row.put("count", vm.getOrDefault(key, 0L));
                        rows.add(row);
                    }
                    break;
                }
            case "user":
            default:
                t = "user";
                Map<String, Long> um = toCountMap(dashboardMapper.usersByDateRange(start, end));
                for (LocalDate cur = start; !cur.isAfter(end); cur = cur.plusDays(1)) {
                    String key = cur.format(DAY);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("date", key);
                    row.put("count", um.getOrDefault(key, 0L));
                    rows.add(row);
                }
                break;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", t);
        out.put("days", d);
        out.put("start_date", start.format(DAY));
        out.put("end_date", end.format(DAY));
        out.put("data", rows);
        return out;
    }

    /**
     * 推广明细（演示数据，共 100 条）：分页、日期区间与多字段组合筛选。
     */
    public Map<String, Object> getPromotionDetails(
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int pageSize,
            String promotionId,
            String promotionName,
            String dramaId,
            String dramaName,
            String account,
            String media,
            String country) {
        int p = Math.max(1, page);
        int ps = Math.min(100, Math.max(1, pageSize));
        List<Map<String, Object>> all = buildDemoPromotionRows(100);

        if (startDate != null && endDate != null) {
            LocalDate s = startDate.isBefore(endDate) ? startDate : endDate;
            LocalDate e = startDate.isBefore(endDate) ? endDate : startDate;
            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> row : all) {
                LocalDate d = LocalDate.parse(row.get("date").toString().substring(0, 10));
                if (!d.isBefore(s) && !d.isAfter(e)) {
                    filtered.add(row);
                }
            }
            all = filtered;
        }

        all = filterByStringField(all, promotionId, "promotion_id");
        all = filterByStringField(all, promotionName, "promotion_name");
        all = filterByStringField(all, dramaId, "drama_id");
        all = filterByStringField(all, dramaName, "drama_name");
        all = filterByStringField(all, account, "account");
        all = filterByMedia(all, media);
        all = filterByCountryList(all, country);

        all.sort(Comparator.comparing((Map<String, Object> m) -> m.get("date").toString()).reversed());
        long total = all.size();
        int from = (p - 1) * ps;
        List<Map<String, Object>> slice = new ArrayList<>();
        if (from < total) {
            int to = (int) Math.min(from + ps, total);
            slice = all.subList(from, to);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", slice);
        out.put("total", total);
        out.put("page", p);
        out.put("pageSize", ps);
        return out;
    }

    private static List<Map<String, Object>> filterByStringField(
            List<Map<String, Object>> rows, String needle, String key) {
        if (needle == null || needle.isBlank()) {
            return rows;
        }
        String n = needle.trim();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object v = row.get(key);
            String s = v != null ? v.toString() : "";
            if (s.contains(n)) {
                out.add(row);
            }
        }
        return out;
    }

    /** 逗号分隔多选：行 media 与任一选中值相等（忽略大小写） */
    private static List<Map<String, Object>> filterByMedia(List<Map<String, Object>> rows, String filter) {
        if (filter == null || filter.isBlank()) {
            return rows;
        }
        String[] parts = filter.split(",");
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object mv = row.get("media");
            String m = mv != null ? mv.toString() : "";
            boolean ok = false;
            for (String part : parts) {
                String t = part.trim();
                if (t.isEmpty()) {
                    continue;
                }
                if (m.equalsIgnoreCase(t)) {
                    ok = true;
                    break;
                }
            }
            if (ok) {
                out.add(row);
            }
        }
        return out;
    }

    /** 逗号分隔多选：行 country 与任一选中值相等 */
    private static List<Map<String, Object>> filterByCountryList(List<Map<String, Object>> rows, String filter) {
        if (filter == null || filter.isBlank()) {
            return rows;
        }
        String[] parts = filter.split(",");
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object cv = row.get("country");
            String c = cv != null ? cv.toString() : "";
            boolean ok = false;
            for (String part : parts) {
                String t = part.trim();
                if (!t.isEmpty() && t.equals(c)) {
                    ok = true;
                    break;
                }
            }
            if (ok) {
                out.add(row);
            }
        }
        return out;
    }

    private static List<Map<String, Object>> buildDemoPromotionRows(int count) {
        String[] promotionNames = {
            "C-GFhar-火车的故事-2.8w粉丝-11",
            "B-KM-X-我在监狱里的日子-2.5w粉丝-17",
            "IDN-A-WP-卜筮-2.1w粉丝-LHRETI-17",
            "C-GFrsw-火车的故事-2.8w粉丝-8",
            "IDN-A-WP-卜筮-2.8%粉丝",
            "C-GF-ha-火车的故事-2.8w粉丝-11",
            "IDN-A-WZ-卜筮-2.8%粉丝"
        };
        String[] dramaNames = {
            "火车的故事", "我在监狱里的日子", "卜筮", "霸道总裁爱上我", "重生之都市修仙"
        };
        // 与广告账户 country 编码一致（TH/ID/VN/PH），便于与账户管理筛选项对齐
        String[] countries = {"TH", "ID", "VN", "PH"};
        String[] medias = {"tiktok", "facebook", "google", "snapchat"};
        Random rnd = new Random(42);
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < count; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            LocalDate date = today.minusDays(rnd.nextInt(30));
            item.put("date", date.toString());
            item.put("promotion_name", promotionNames[rnd.nextInt(promotionNames.length)]);
            item.put("drama_name", dramaNames[rnd.nextInt(dramaNames.length)]);
            item.put("drama_id", "D" + (10000 + rnd.nextInt(90000)));
            item.put("country", countries[rnd.nextInt(countries.length)]);
            item.put("media", medias[rnd.nextInt(medias.length)]);
            item.put("account", "账户" + (rnd.nextInt(5) + 1));
            item.put("promotion_id", "TG" + (100000 + rnd.nextInt(900000)));

            double cost = 100 + rnd.nextDouble() * 1500;
            item.put("cost", Math.round(cost * 100.0) / 100.0);
            int impressions = 10000 + rnd.nextInt(100000);
            int clicks = (int) (impressions * (0.01 + rnd.nextDouble() * 0.05));
            item.put("impressions", impressions);
            item.put("clicks", clicks);
            double cpm = impressions > 0 ? cost / impressions * 1000.0 : 0;
            item.put("cpm", Math.round(cpm * 100.0) / 100.0);

            int registrations = (int) (clicks * (0.1 + rnd.nextDouble() * 0.3));
            item.put("registrations", registrations);
            int rechargeUsers = (int) (registrations * (0.3 + rnd.nextDouble() * 0.5));
            int rechargeCount = (int) (rechargeUsers * (1 + rnd.nextDouble() * 2));
            double rechargeAmount = rechargeCount * (5 + rnd.nextDouble() * 20);
            item.put("recharge_users", rechargeUsers);
            item.put("recharge_count", rechargeCount);
            item.put("recharge_amount", Math.round(rechargeAmount * 100.0) / 100.0);

            int userCount = registrations + rnd.nextInt(50);
            item.put("user_count", userCount);
            item.put("order_count", rechargeCount);
            item.put("paid_users", rechargeUsers);

            double roi = cost > 0 ? rechargeAmount / cost : 0;
            item.put("roi", Math.round(roi * 100.0) / 100.0);
            double profit = rechargeAmount - cost;
            item.put("profit", Math.round(profit * 100.0) / 100.0);
            double avgCost =
                    registrations > 0 ? Math.round((cost / registrations) * 100.0) / 100.0 : 0;
            item.put("avg_cost_per_user", avgCost);

            list.add(item);
        }
        return list;
    }

    private static Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> m = new HashMap<>();
        if (rows == null) {
            return m;
        }
        for (Map<String, Object> row : rows) {
            Object sd = row.get("stat_date");
            Object n = row.get("n");
            if (sd == null) {
                continue;
            }
            String key = sd instanceof java.sql.Date
                    ? ((java.sql.Date) sd).toLocalDate().format(DAY)
                    : LocalDate.parse(sd.toString().substring(0, 10)).format(DAY);
            long v = n instanceof Number ? ((Number) n).longValue() : Long.parseLong(n.toString());
            m.put(key, v);
        }
        return m;
    }

    private static Map<String, BigDecimal> toAmountMap(List<Map<String, Object>> rows) {
        Map<String, BigDecimal> m = new HashMap<>();
        if (rows == null) {
            return m;
        }
        for (Map<String, Object> row : rows) {
            Object sd = row.get("stat_date");
            Object amt = row.get("amt");
            if (sd == null) {
                continue;
            }
            String key = sd instanceof java.sql.Date
                    ? ((java.sql.Date) sd).toLocalDate().format(DAY)
                    : LocalDate.parse(sd.toString().substring(0, 10)).format(DAY);
            BigDecimal v =
                    amt instanceof BigDecimal ? (BigDecimal) amt : new BigDecimal(amt.toString());
            m.put(key, v);
        }
        return m;
    }

    private static BigDecimal round2(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
