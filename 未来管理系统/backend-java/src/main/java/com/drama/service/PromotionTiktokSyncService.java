package com.drama.service;

import com.drama.config.TikTokIntegrationProperties;
import com.drama.entity.PromotionDetailsSummary;
import com.drama.entity.PromotionLink;
import com.drama.entity.TikTokAccount;
import com.drama.entity.TiktokCostRecord;
import com.drama.dto.PromotionRechargeAggRow;
import com.drama.dto.PromotionTiktokDayAggRow;
import com.drama.integration.tiktok.TikTokIntegratedReportClient;
import com.drama.integration.tiktok.TikTokOAuthService;
import com.drama.integration.tiktok.TiktokCampaignReportRow;
import com.drama.mapper.PromotionDetailsSummaryMapper;
import com.drama.mapper.PromotionLinkMapper;
import com.drama.mapper.RechargeOrderMapper;
import com.drama.mapper.TikTokAccountMapper;
import com.drama.mapper.TiktokCostRecordMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * TikTok 消耗同步（#079）：默认 Mock；配置 {@link TikTokIntegrationProperties} 且关闭 mock 后调用 TikTok
 * Open API v1.3 {@code GET /report/integrated/get/} 写 {@link TiktokCostRecord} 并刷新汇总。
 *
 * <p>官方总览：<a href="https://business-api.tiktok.com/portal/docs/about-the-guide/v1.3">v1.3 Guide</a>。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionTiktokSyncService {

    private final TiktokCostRecordMapper tiktokCostRecordMapper;
    private final PromotionDetailsSummaryMapper promotionDetailsSummaryMapper;
    private final PromotionLinkMapper promotionLinkMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final PromotionDetailsService promotionDetailsService;
    private final TikTokIntegrationProperties tikTokIntegrationProperties;
    private final TikTokIntegratedReportClient tikTokIntegratedReportClient;
    private final TikTokAccountMapper tikTokAccountMapper;
    private final TikTokOAuthService tikTokOAuthService;

    /** 定时任务 / {@code POST /api/promotion-details/sync} */
    public void runSyncAll() {
        if (tikTokIntegrationProperties.isMockEnabled()) {
            runMockSyncAll();
            return;
        }
        try {
            List<TikTokAccount> accounts = tikTokAccountMapper.selectByStatus("active");
            if (!accounts.isEmpty()) {
                runRealReportSyncForAccounts(accounts);
                return;
            }
            String token = tikTokIntegrationProperties.getAdvertiserAccessToken();
            String adv = tikTokIntegrationProperties.getAdvertiserId();
            if (!StringUtils.hasText(token) || !StringUtils.hasText(adv)) {
                log.warn(
                        "TikTok: 无 tiktok_accounts(active) 且未配置 TIKTOK_ADVERTISER_ACCESS_TOKEN / TIKTOK_ADVERTISER_ID，使用 Mock");
                runMockSyncAll();
                return;
            }
            runRealReportSync();
        } catch (Exception e) {
            log.error("TikTok real sync failed", e);
            if (tikTokIntegrationProperties.isFallbackMockOnError()) {
                runMockSyncAll();
            }
        }
    }

    /** 仅 Mock（兼容历史调用） */
    public void runMockSyncAll() {
        List<String> pids =
                promotionLinkMapper.selectAllOrderByIdDesc().stream()
                        .filter(p -> p.getPlatform() != null
                                && p.getPlatform().toLowerCase(Locale.ROOT).contains("tiktok"))
                        .map(PromotionLink::getPromoteId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        List<String> targets = pids.isEmpty() ? List.of("id:79446") : pids;
        for (String pid : targets) {
            syncMockOne(pid);
        }
        log.info("TikTok mock sync done, promotions={}", targets.size());
    }

    private void runRealReportSyncForAccounts(List<TikTokAccount> accounts) {
        LocalDate today = LocalDate.now();
        List<PromotionLink> tiktokLinks = loadTiktokPromotionLinks();
        int totalRows = 0;
        int totalMatched = 0;
        for (TikTokAccount acc : accounts) {
            try {
                tikTokOAuthService.checkAndRefreshToken(acc);
                TikTokAccount fresh = tikTokAccountMapper.selectByAdvertiserId(acc.getAdvertiserId());
                if (fresh == null || !StringUtils.hasText(fresh.getAccessToken())) {
                    log.warn("TikTok sync skip: no token for advertiser_id={}", acc.getAdvertiserId());
                    continue;
                }
                List<TiktokCampaignReportRow> rows =
                        tikTokIntegratedReportClient.fetchAuctionCampaignDay(
                                fresh.getAdvertiserId(),
                                fresh.getAccessToken(),
                                today,
                                tikTokIntegrationProperties.getReportPageSize());
                String display =
                        StringUtils.hasText(fresh.getAdvertiserName())
                                ? fresh.getAdvertiserName()
                                : "TikTok";
                int m = applyReportRows(rows, fresh.getAdvertiserId(), display, tiktokLinks);
                totalRows += rows.size();
                totalMatched += m;
                log.info(
                        "TikTok real sync account={} reportRows={}, matched={}",
                        fresh.getAdvertiserId(),
                        rows.size(),
                        m);
            } catch (Exception e) {
                log.error("TikTok sync failed advertiser_id={}", acc.getAdvertiserId(), e);
                if (tikTokIntegrationProperties.isFallbackMockOnError()) {
                    log.warn("TikTok fallback to mock after account failure");
                    runMockSyncAll();
                    return;
                }
            }
        }
        log.info("TikTok real sync (multi account) done, reportRows={}, matchedPromotions={}", totalRows, totalMatched);
    }

    private void runRealReportSync() {
        LocalDate today = LocalDate.now();
        List<TiktokCampaignReportRow> rows =
                tikTokIntegratedReportClient.fetchAuctionCampaignDay(
                        today, tikTokIntegrationProperties.getReportPageSize());
        List<PromotionLink> tiktokLinks = loadTiktokPromotionLinks();
        int matched =
                applyReportRows(
                        rows,
                        tikTokIntegrationProperties.getAdvertiserId(),
                        "TikTok",
                        tiktokLinks);
        log.info("TikTok real sync done, reportRows={}, matchedPromotions={}", rows.size(), matched);
    }

    private List<PromotionLink> loadTiktokPromotionLinks() {
        return promotionLinkMapper.selectAllOrderByIdDesc().stream()
                .filter(
                        p -> p.getPlatform() != null
                                && p.getPlatform().toLowerCase(Locale.ROOT).contains("tiktok"))
                .toList();
    }

    /**
     * @return 匹配到本地推广的行数
     */
    private int applyReportRows(
            List<TiktokCampaignReportRow> rows,
            String advId,
            String accountDisplayName,
            List<PromotionLink> tiktokLinks) {
        if (rows.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime slot = floorToTenMinutes(LocalDateTime.now());
        int matched = 0;
        for (TiktokCampaignReportRow r : rows) {
            String pid = resolvePromotionId(r.getCampaignName(), tiktokLinks);
            if (pid == null) {
                continue;
            }
            matched++;
            TiktokCostRecord before = tiktokCostRecordMapper.selectLatestByPromotion(pid);
            BigDecimal apiSpend = r.getSpend() != null ? r.getSpend() : BigDecimal.ZERO;
            BigDecimal incForSpeed = computeSpeedIncrement(before, apiSpend, today);

            TiktokCostRecord row = new TiktokCostRecord();
            row.setPromotionId(pid);
            row.setAccountId(advId);
            row.setAccountName(accountDisplayName != null ? accountDisplayName : "TikTok");
            row.setBalance(null);
            row.setCampaignName(trimCampaignLabel(r));
            row.setCost(apiSpend);
            row.setImpressions(r.getImpressions());
            row.setRecordTime(slot);
            tiktokCostRecordMapper.upsert(row);
            upsertPromotionSummary(pid, row, incForSpeed);
        }
        return matched;
    }

    /**
     * 报表为「当日累计消耗」时的估速：同日上一快照与本次差分；跨日或首条则用本次全日量作为一次估速基数。
     */
    private static BigDecimal computeSpeedIncrement(TiktokCostRecord before, BigDecimal apiSpend, LocalDate today) {
        if (before == null || before.getRecordTime() == null || before.getCost() == null) {
            return apiSpend.max(BigDecimal.ZERO);
        }
        if (!before.getRecordTime().toLocalDate().equals(today)) {
            return apiSpend.max(BigDecimal.ZERO);
        }
        BigDecimal d = apiSpend.subtract(before.getCost());
        return d.compareTo(BigDecimal.ZERO) > 0 ? d : BigDecimal.ZERO;
    }

    private static String trimCampaignLabel(TiktokCampaignReportRow r) {
        String cid = r.getCampaignId() != null ? r.getCampaignId() : "";
        String name = r.getCampaignName() != null ? r.getCampaignName() : "";
        if (cid.isEmpty()) {
            return name;
        }
        return name.isEmpty() ? cid : cid + " " + name;
    }

    private static String resolvePromotionId(String campaignName, List<PromotionLink> tiktokLinks) {
        if (campaignName == null || campaignName.isBlank()) {
            return null;
        }
        for (PromotionLink link : tiktokLinks) {
            String pid = link.getPromoteId();
            if (pid != null && !pid.isBlank() && campaignName.contains(pid)) {
                return pid;
            }
        }
        return null;
    }

    private void syncMockOne(String promotionId) {
        LocalDateTime slot = floorToTenMinutes(LocalDateTime.now());
        TiktokCostRecord last = tiktokCostRecordMapper.selectLatestByPromotion(promotionId);
        BigDecimal base = last != null && last.getCost() != null ? last.getCost() : BigDecimal.ZERO;
        int incInt = 50 + ThreadLocalRandom.current().nextInt(101);
        BigDecimal inc = BigDecimal.valueOf(incInt);

        TiktokCostRecord row = new TiktokCostRecord();
        row.setPromotionId(promotionId);
        row.setAccountId("76211337");
        row.setAccountName("C-GF-xd-49T-EN-02");
        row.setBalance(new BigDecimal("59.57"));
        row.setCampaignName("id:18616");
        row.setCost(base.add(inc));
        row.setImpressions(10000L + ThreadLocalRandom.current().nextInt(5000));
        row.setRecordTime(slot);
        tiktokCostRecordMapper.upsert(row);

        BigDecimal speed = inc.multiply(BigDecimal.valueOf(6));
        upsertPromotionSummaryCore(promotionId, row, speed);
    }

    private void upsertPromotionSummary(String promotionId, TiktokCostRecord row, BigDecimal tenMinuteSpendDelta) {
        BigDecimal speed = tenMinuteSpendDelta.multiply(BigDecimal.valueOf(6));
        upsertPromotionSummaryCore(promotionId, row, speed);
    }

    private void upsertPromotionSummaryCore(String promotionId, TiktokCostRecord row, BigDecimal speed) {
        LocalDate today = LocalDate.now();
        BigDecimal dayCost = tiktokCostRecordMapper.selectIntraDaySpendDelta(promotionId, today);
        if (dayCost == null) {
            dayCost = BigDecimal.ZERO;
        }

        Map<String, PromotionRechargeAggRow> rechargeToday =
                rechargeOrderMapper.selectAggByDate(today).stream()
                        .collect(Collectors.toMap(PromotionRechargeAggRow::getPromotionId, Function.identity(), (a, b) -> a));
        PromotionRechargeAggRow ra = rechargeToday.get(promotionId);

        PromotionDetailsSummary sum = promotionDetailsSummaryMapper.selectByDateAndPromotion(today, promotionId);
        if (sum == null) {
            sum = new PromotionDetailsSummary();
            sum.setDate(today);
            sum.setPromotionId(promotionId);
        }
        PromotionLink link = findLink(promotionId);
        if (link != null) {
            sum.setPromotionName(link.getPromoteName());
            sum.setPlatform(link.getPlatform());
            sum.setCountry(link.getCountry());
            sum.setDramaId(link.getDramaId());
            sum.setDramaName(link.getDramaName());
        }
        promotionDetailsService.applyPlatformCountryFromAdAccount(sum, row.getAccountId());
        sum.setSpeed(speed);
        sum.setCost(dayCost);
        PromotionTiktokDayAggRow dayAgg =
                tiktokCostRecordMapper.selectDayAggByDate(today).stream()
                        .filter(r -> promotionId.equals(r.getPromotionId()))
                        .findFirst()
                        .orElse(null);
        long imp =
                dayAgg != null && dayAgg.getMaxImpressions() != null
                        ? dayAgg.getMaxImpressions()
                        : 0L;
        sum.setImpressions(imp);
        if (imp > 0 && dayCost.compareTo(BigDecimal.ZERO) > 0) {
            sum.setCpm(
                    dayCost.divide(BigDecimal.valueOf(imp), 10, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(1000))
                            .setScale(4, RoundingMode.HALF_UP));
        } else {
            sum.setCpm(BigDecimal.ZERO);
        }
        if (ra != null) {
            sum.setRechargeAmount(nullToZero(ra.getRechargeAmount()));
            sum.setOrderCount(nullToZeroInt(ra.getOrderCount()));
            sum.setFirstRechargeCount(nullToZeroInt(ra.getFirstRechargeCount()));
            sum.setRepeatRechargeCount(nullToZeroInt(ra.getRepeatRechargeCount()));
        } else {
            sum.setRechargeAmount(BigDecimal.ZERO);
            sum.setOrderCount(0);
            sum.setFirstRechargeCount(0);
            sum.setRepeatRechargeCount(0);
        }
        sum.setProfit(sum.getRechargeAmount().subtract(sum.getCost()));
        enrichDerivedMetrics(sum);
        promotionDetailsSummaryMapper.upsert(sum);
    }

    private PromotionLink findLink(String promoteId) {
        return promotionLinkMapper.selectAllOrderByIdDesc().stream()
                .filter(p -> promoteId.equals(p.getPromoteId()))
                .findFirst()
                .orElse(null);
    }

    private static LocalDateTime floorToTenMinutes(LocalDateTime t) {
        int m = (t.getMinute() / 10) * 10;
        return t.truncatedTo(ChronoUnit.MINUTES).withMinute(m).withSecond(0).withNano(0);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static int nullToZeroInt(Integer v) {
        return v != null ? v : 0;
    }

    static void enrichDerivedMetrics(PromotionDetailsSummary s) {
        BigDecimal cost = nullToZero(s.getCost());
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
        BigDecimal recharge = nullToZero(s.getRechargeAmount());
        if (first > 0) {
            s.setAvgRechargePerUser(recharge.divide(BigDecimal.valueOf(first), 4, RoundingMode.HALF_UP));
        } else {
            s.setAvgRechargePerUser(BigDecimal.ZERO);
        }
        if (s.getCpm() == null) {
            s.setCpm(BigDecimal.ZERO);
        }
        if (s.getImpressions() == null) {
            s.setImpressions(0L);
        }
    }
}
