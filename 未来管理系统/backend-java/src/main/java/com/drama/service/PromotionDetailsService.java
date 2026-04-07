package com.drama.service;

import com.drama.dto.*;
import com.drama.entity.AdAccount;
import com.drama.entity.PromotionDetailsSummary;
import com.drama.entity.PromotionLink;
import com.drama.entity.TiktokCostRecord;
import com.drama.mapper.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionDetailsService {

    private final PromotionDetailsSummaryMapper promotionDetailsSummaryMapper;
    private final TiktokCostRecordMapper tiktokCostRecordMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final PromotionLinkMapper promotionLinkMapper;
    private final UserMapper userMapper;
    private final AdAccountMapper adAccountMapper;

    private static final List<String> DEFAULT_PLATFORMS =
            List.of("tiktok", "facebook", "google");
    private static final List<String> DEFAULT_COUNTRY_CODES =
            List.of("ID", "TH", "US", "VN", "PH", "MY");

    /** #082/#083：投放媒体（ad_accounts.media）；异常或空表时返回默认列表 */
    public List<String> getPlatforms() {
        try {
            List<String> list = adAccountMapper.selectDistinctPlatforms();
            if (list == null || list.isEmpty()) {
                return new ArrayList<>(DEFAULT_PLATFORMS);
            }
            return list;
        } catch (Exception e) {
            log.warn("getPlatforms fallback: {}", e.toString());
            return new ArrayList<>(DEFAULT_PLATFORMS);
        }
    }

    /** #082/#083：国家 { code, name }；异常或空表时返回默认列表 */
    public List<Map<String, String>> getCountries() {
        try {
            List<String> codes = adAccountMapper.selectDistinctCountries();
            if (codes == null || codes.isEmpty()) {
                return countryRowsFromCodes(DEFAULT_COUNTRY_CODES);
            }
            List<Map<String, String>> out = new ArrayList<>();
            for (String raw : codes) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String code = raw.trim().toUpperCase(Locale.ROOT);
                Map<String, String> row = new LinkedHashMap<>();
                row.put("code", code);
                row.put("name", countryNameZh(code));
                out.add(row);
            }
            if (out.isEmpty()) {
                return countryRowsFromCodes(DEFAULT_COUNTRY_CODES);
            }
            return out;
        } catch (Exception e) {
            log.warn("getCountries fallback: {}", e.toString());
            return countryRowsFromCodes(DEFAULT_COUNTRY_CODES);
        }
    }

    private static List<Map<String, String>> countryRowsFromCodes(List<String> codes) {
        List<Map<String, String>> out = new ArrayList<>();
        for (String code : codes) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("code", code);
            row.put("name", countryNameZh(code));
            out.add(row);
        }
        return out;
    }

    /**
     * 以账户管理为准覆盖推广明细汇总行上的 platform / country（#082）。
     * {@code media} 列映射为与筛选一致的 platform：tiktok / facebook / google。
     */
    public void applyPlatformCountryFromAdAccount(PromotionDetailsSummary s, String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return;
        }
        AdAccount acc = adAccountMapper.selectFirstByAccountId(accountId.trim());
        if (acc == null) {
            return;
        }
        String p = normalizeMediaToPlatform(acc.getMedia());
        if (p != null && !p.isEmpty()) {
            s.setPlatform(p);
        }
        if (acc.getCountry() != null && !acc.getCountry().isBlank()) {
            s.setCountry(acc.getCountry().trim().toUpperCase(Locale.ROOT));
        }
    }

    private static String countryNameZh(String code) {
        if (code == null) {
            return "";
        }
        String k = code.toUpperCase(Locale.ROOT);
        Map<String, String> m = new HashMap<>();
        m.put("ID", "印尼");
        m.put("TH", "泰国");
        m.put("US", "美国");
        m.put("VN", "越南");
        m.put("PH", "菲律宾");
        m.put("MY", "马来西亚");
        return m.getOrDefault(k, code);
    }

    private static String normalizeMediaToPlatform(String media) {
        if (media == null) {
            return null;
        }
        String m = media.trim().toLowerCase(Locale.ROOT);
        if (m.isEmpty()) {
            return null;
        }
        if (m.contains("tiktok")) {
            return "tiktok";
        }
        if (m.contains("facebook") || m.contains("meta")) {
            return "facebook";
        }
        if (m.contains("google")) {
            return "google";
        }
        return m;
    }

    public PromotionDetailsResponseDTO getPromotionDetails(PromotionDetailsQueryDTO query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        query.setPage(page);
        query.setPageSize(Math.min(pageSize, 200));

        List<PromotionDetailsSummary> list = promotionDetailsSummaryMapper.selectByQuery(query);
        long total = promotionDetailsSummaryMapper.countByQuery(query);
        PromotionDetailsSummary agg = promotionDetailsSummaryMapper.selectSummaryAggregateByQuery(query);
        if (agg != null) {
            enrichAggregatedSummary(agg);
        }

        PromotionDetailsResponseDTO res = new PromotionDetailsResponseDTO();
        res.setList(list);
        res.setSummary(agg);
        res.setTotal(total);
        return res;
    }

    public ProfitChartDataDTO getProfitChart(String promotionId, ProfitChartQueryDTO query) {
        ProfitChartDataDTO dto = new ProfitChartDataDTO();
        if (query.getStartDate() == null || query.getEndDate() == null) {
            dto.setChartData(List.of());
            return dto;
        }
        String g = query.getGranularity() == null ? "day" : query.getGranularity().toLowerCase(Locale.ROOT);
        boolean multiDay = !query.getStartDate().equals(query.getEndDate());
        if ("hour".equals(g) && multiDay) {
            g = "day";
        }
        if ("hour".equals(g)) {
            dto.setChartData(profitChartHourly(promotionId, query.getStartDate(), query.getEndDate()));
        } else {
            List<PromotionDetailsSummary> rows =
                    promotionDetailsSummaryMapper.selectForProfitChart(
                            promotionId, query.getStartDate(), query.getEndDate());
            List<ProfitChartDataDTO.ChartPoint> pts = new ArrayList<>();
            DateTimeFormatter dayLabel = DateTimeFormatter.ofPattern("MM-dd");
            for (PromotionDetailsSummary r : rows) {
                ProfitChartDataDTO.ChartPoint p = new ProfitChartDataDTO.ChartPoint();
                p.setTime(r.getDate() != null ? r.getDate().format(dayLabel) : "");
                p.setProfit(r.getProfit() != null ? r.getProfit() : BigDecimal.ZERO);
                pts.add(p);
            }
            dto.setChartData(pts);
        }
        return dto;
    }

    /**
     * #086：当前列表筛选条件下，多推广利润曲线（按天为汇总表 SUM；按小时为各推广消耗/充值在「结束日」逐小时合计）。
     */
    public ProfitChartDataDTO getProfitChartAll(
            ProfitChartQueryDTO chartQuery, PromotionDetailsQueryDTO listFilters) {
        ProfitChartDataDTO dto = new ProfitChartDataDTO();
        if (chartQuery.getStartDate() == null || chartQuery.getEndDate() == null) {
            dto.setChartData(List.of());
            return dto;
        }
        PromotionDetailsQueryDTO q = new PromotionDetailsQueryDTO();
        q.setStartDate(chartQuery.getStartDate());
        q.setEndDate(chartQuery.getEndDate());
        copyListFilters(listFilters, q);

        String g =
                chartQuery.getGranularity() == null
                        ? "day"
                        : chartQuery.getGranularity().toLowerCase(Locale.ROOT);
        boolean multiDay = !chartQuery.getStartDate().equals(chartQuery.getEndDate());
        if ("hour".equals(g) && multiDay) {
            g = "day";
        }
        if ("hour".equals(g)) {
            dto.setChartData(profitChartHourlyAll(q));
        } else {
            List<ProfitChartDataDTO.ChartPoint> pts =
                    promotionDetailsSummaryMapper.selectAggregatedProfitChartByQuery(q);
            dto.setChartData(pts != null ? pts : List.of());
        }
        return dto;
    }

    /** 按日重算汇总行（#078 三表 + 投放链接元数据） */
    public void rollUpDaily(LocalDate day) {
        Map<String, PromotionLink> links =
                promotionLinkMapper.selectAllOrderByIdDesc().stream()
                        .collect(Collectors.toMap(PromotionLink::getPromoteId, Function.identity(), (a, b) -> a));
        Map<String, PromotionRechargeAggRow> rechargeMap =
                rechargeOrderMapper.selectAggByDate(day).stream()
                        .collect(Collectors.toMap(PromotionRechargeAggRow::getPromotionId, Function.identity(), (a, b) -> a));
        Map<String, PromotionTiktokDayAggRow> tiktokMap =
                tiktokCostRecordMapper.selectDayAggByDate(day).stream()
                        .collect(Collectors.toMap(PromotionTiktokDayAggRow::getPromotionId, Function.identity(), (a, b) -> a));
        Map<String, PromotionUserNewAggRow> userMap =
                userMapper.selectNewUserAggByDate(day).stream()
                        .collect(Collectors.toMap(PromotionUserNewAggRow::getPromotionId, Function.identity(), (a, b) -> a));

        Set<String> all = new LinkedHashSet<>();
        all.addAll(rechargeMap.keySet());
        all.addAll(tiktokMap.keySet());
        all.addAll(userMap.keySet());

        for (String pid : all) {
            PromotionDetailsSummary s = new PromotionDetailsSummary();
            s.setDate(day);
            s.setPromotionId(pid);
            PromotionLink link = links.get(pid);
            if (link != null) {
                s.setPromotionName(link.getPromoteName());
                s.setPlatform(link.getPlatform());
                s.setCountry(link.getCountry());
                s.setDramaId(link.getDramaId());
                s.setDramaName(link.getDramaName());
            }
            PromotionTiktokDayAggRow ta = tiktokMap.get(pid);
            BigDecimal cost =
                    ta != null && ta.getDayCost() != null ? ta.getDayCost() : BigDecimal.ZERO;
            long imp = ta != null && ta.getMaxImpressions() != null ? ta.getMaxImpressions() : 0L;
            s.setImpressions(imp);
            if (ta != null && ta.getAccountId() != null && !ta.getAccountId().isBlank()) {
                applyPlatformCountryFromAdAccount(s, ta.getAccountId());
            }
            PromotionRechargeAggRow ra = rechargeMap.get(pid);
            if (ra != null) {
                s.setRechargeAmount(ra.getRechargeAmount() != null ? ra.getRechargeAmount() : BigDecimal.ZERO);
                s.setOrderCount(ra.getOrderCount() != null ? ra.getOrderCount() : 0);
                s.setFirstRechargeCount(ra.getFirstRechargeCount() != null ? ra.getFirstRechargeCount() : 0);
                s.setRepeatRechargeCount(ra.getRepeatRechargeCount() != null ? ra.getRepeatRechargeCount() : 0);
            } else {
                s.setRechargeAmount(BigDecimal.ZERO);
                s.setOrderCount(0);
                s.setFirstRechargeCount(0);
                s.setRepeatRechargeCount(0);
            }
            PromotionUserNewAggRow ua = userMap.get(pid);
            s.setUserCount(ua != null && ua.getUserCount() != null ? ua.getUserCount() : 0);
            s.setCost(cost);
            s.setProfit(s.getRechargeAmount().subtract(s.getCost()));
            if (imp > 0 && cost.compareTo(BigDecimal.ZERO) > 0) {
                s.setCpm(
                        cost.divide(BigDecimal.valueOf(imp), 10, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(1000))
                                .setScale(4, RoundingMode.HALF_UP));
            } else {
                s.setCpm(BigDecimal.ZERO);
            }
            s.setSpeed(BigDecimal.ZERO);
            PromotionTiktokSyncService.enrichDerivedMetrics(s);
            promotionDetailsSummaryMapper.upsert(s);
        }
        log.info("rollUpDaily done date={} keys={}", day, all.size());
    }

    private void enrichAggregatedSummary(PromotionDetailsSummary s) {
        BigDecimal cost = s.getCost() != null ? s.getCost() : BigDecimal.ZERO;
        BigDecimal profit = s.getProfit() != null ? s.getProfit() : BigDecimal.ZERO;
        if (cost.compareTo(BigDecimal.ZERO) > 0) {
            s.setRoi(profit.divide(cost, 4, RoundingMode.HALF_UP));
        } else {
            s.setRoi(BigDecimal.ZERO);
        }
        int orders = s.getOrderCount() != null ? s.getOrderCount() : 0;
        int first = s.getFirstRechargeCount() != null ? s.getFirstRechargeCount() : 0;
        if (orders > 0) {
            s.setFirstRechargeRate(
                    BigDecimal.valueOf(first).divide(BigDecimal.valueOf(orders), 4, RoundingMode.HALF_UP));
        } else {
            s.setFirstRechargeRate(BigDecimal.ZERO);
        }
        BigDecimal recharge = s.getRechargeAmount() != null ? s.getRechargeAmount() : BigDecimal.ZERO;
        if (first > 0) {
            s.setAvgRechargePerUser(recharge.divide(BigDecimal.valueOf(first), 4, RoundingMode.HALF_UP));
        } else {
            s.setAvgRechargePerUser(BigDecimal.ZERO);
        }
        long imp = s.getImpressions() != null ? s.getImpressions() : 0L;
        if (imp > 0 && cost.compareTo(BigDecimal.ZERO) > 0) {
            s.setCpm(
                    cost.divide(BigDecimal.valueOf(imp), 10, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(1000))
                            .setScale(4, RoundingMode.HALF_UP));
        } else {
            s.setCpm(BigDecimal.ZERO);
        }
    }

    private List<ProfitChartDataDTO.ChartPoint> profitChartHourly(
            String promotionId, LocalDate start, LocalDate end) {
        // 按小时视图：固定展示「结束日」当天 00:00～23:00（与日期范围多天时对齐用户所选区间末尾日）
        LocalDate chartDay = end != null ? end : start;
        LocalDateTime t0 = chartDay.atStartOfDay();
        LocalDateTime t1 = chartDay.plusDays(1).atStartOfDay();
        List<TiktokCostRecord> records =
                tiktokCostRecordMapper.selectByPromotionBetween(promotionId, t0, t1);
        Map<String, List<TiktokCostRecord>> byHour =
                records.stream()
                        .collect(
                                Collectors.groupingBy(
                                        r -> r.getRecordTime() == null
                                                ? ""
                                                : r.getRecordTime()
                                                        .withMinute(0)
                                                        .withSecond(0)
                                                        .withNano(0)
                                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                        LinkedHashMap::new,
                                        Collectors.toList()));
        DateTimeFormatter bucketKey = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter axisHour = DateTimeFormatter.ofPattern("HH:mm");
        List<ProfitChartDataDTO.ChartPoint> out = new ArrayList<>();
        for (LocalDateTime h = t0; h.isBefore(t1); h = h.plusHours(1)) {
            String key = h.format(bucketKey);
            List<TiktokCostRecord> chunk = byHour.getOrDefault(key, List.of());
            BigDecimal spend = bucketSpendDelta(chunk);
            LocalDateTime next = h.plusHours(1);
            BigDecimal recharge =
                    rechargeOrderMapper.sumAmountByPromotionAndHourStart(
                            promotionId, h.format(bucketKey), next.format(bucketKey));
            if (recharge == null) {
                recharge = BigDecimal.ZERO;
            }
            ProfitChartDataDTO.ChartPoint p = new ProfitChartDataDTO.ChartPoint();
            p.setTime(h.format(axisHour));
            p.setProfit(recharge.subtract(spend));
            out.add(p);
        }
        return out;
    }

    private List<ProfitChartDataDTO.ChartPoint> profitChartHourlyAll(PromotionDetailsQueryDTO rangeFilter) {
        LocalDate chartDay =
                rangeFilter.getEndDate() != null ? rangeFilter.getEndDate() : rangeFilter.getStartDate();
        if (chartDay == null) {
            return List.of();
        }
        PromotionDetailsQueryDTO dayFilter = new PromotionDetailsQueryDTO();
        copyListFilters(rangeFilter, dayFilter);
        dayFilter.setStartDate(chartDay);
        dayFilter.setEndDate(chartDay);
        List<String> pids = promotionDetailsSummaryMapper.selectDistinctPromotionIdsByQuery(dayFilter);
        if (pids == null || pids.isEmpty()) {
            return emptyHourlyBuckets(chartDay);
        }
        LocalDateTime t0 = chartDay.atStartOfDay();
        LocalDateTime t1 = chartDay.plusDays(1).atStartOfDay();
        List<TiktokCostRecord> allRecords =
                tiktokCostRecordMapper.selectByPromotionIdsBetween(pids, t0, t1);
        DateTimeFormatter bucketKey = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter axisHour = DateTimeFormatter.ofPattern("HH:mm");
        Map<String, Map<String, List<TiktokCostRecord>>> byPromoHour = new HashMap<>();
        for (TiktokCostRecord r : allRecords) {
            if (r.getPromotionId() == null || r.getRecordTime() == null) {
                continue;
            }
            LocalDateTime hourStart =
                    r.getRecordTime().withMinute(0).withSecond(0).withNano(0);
            String hk = hourStart.format(bucketKey);
            byPromoHour
                    .computeIfAbsent(r.getPromotionId(), k -> new HashMap<>())
                    .computeIfAbsent(hk, k -> new ArrayList<>())
                    .add(r);
        }
        List<ProfitChartDataDTO.ChartPoint> out = new ArrayList<>();
        for (LocalDateTime h = t0; h.isBefore(t1); h = h.plusHours(1)) {
            String key = h.format(bucketKey);
            BigDecimal spend = BigDecimal.ZERO;
            for (String pid : pids) {
                List<TiktokCostRecord> chunk =
                        byPromoHour.getOrDefault(pid, Map.of()).getOrDefault(key, List.of());
                spend = spend.add(bucketSpendDelta(chunk));
            }
            LocalDateTime next = h.plusHours(1);
            BigDecimal recharge =
                    rechargeOrderMapper.sumAmountByPromotionsAndHourStart(
                            pids, h.format(bucketKey), next.format(bucketKey));
            if (recharge == null) {
                recharge = BigDecimal.ZERO;
            }
            ProfitChartDataDTO.ChartPoint p = new ProfitChartDataDTO.ChartPoint();
            p.setTime(h.format(axisHour));
            p.setProfit(recharge.subtract(spend));
            out.add(p);
        }
        return out;
    }

    private static void copyListFilters(PromotionDetailsQueryDTO src, PromotionDetailsQueryDTO dest) {
        dest.setPromotionId(src.getPromotionId());
        dest.setPromotionName(src.getPromotionName());
        dest.setPlatform(src.getPlatform());
        dest.setDramaId(src.getDramaId());
        dest.setDramaName(src.getDramaName());
        dest.setCountry(src.getCountry());
        dest.setAccountId(src.getAccountId());
    }

    private static List<ProfitChartDataDTO.ChartPoint> emptyHourlyBuckets(LocalDate chartDay) {
        LocalDateTime t0 = chartDay.atStartOfDay();
        LocalDateTime t1 = chartDay.plusDays(1).atStartOfDay();
        DateTimeFormatter axisHour = DateTimeFormatter.ofPattern("HH:mm");
        List<ProfitChartDataDTO.ChartPoint> out = new ArrayList<>();
        for (LocalDateTime h = t0; h.isBefore(t1); h = h.plusHours(1)) {
            ProfitChartDataDTO.ChartPoint p = new ProfitChartDataDTO.ChartPoint();
            p.setTime(h.format(axisHour));
            p.setProfit(BigDecimal.ZERO);
            out.add(p);
        }
        return out;
    }

    private static BigDecimal bucketSpendDelta(List<TiktokCostRecord> sortedChunk) {
        if (sortedChunk == null || sortedChunk.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> costs =
                sortedChunk.stream()
                        .map(TiktokCostRecord::getCost)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList();
        if (costs.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return costs.get(costs.size() - 1).subtract(costs.get(0));
    }
}
