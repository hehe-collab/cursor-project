package com.drama.service;

import com.drama.entity.PromotionDetailsSummary;
import com.drama.entity.PromotionLink;
import com.drama.entity.TiktokCostRecord;
import com.drama.dto.PromotionRechargeAggRow;
import com.drama.dto.PromotionTiktokDayAggRow;
import com.drama.mapper.PromotionDetailsSummaryMapper;
import com.drama.mapper.PromotionLinkMapper;
import com.drama.mapper.RechargeOrderMapper;
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

/**
 * TikTok 消耗同步（#079）：当前为模拟累计消耗；接入真实 API 时替换 {@link #runMockSyncAll}。
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

    /** 与定时任务 / 手动 POST 共用 */
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
        LocalDate today = LocalDate.now();
        BigDecimal dayCost =
                tiktokCostRecordMapper.selectIntraDaySpendDelta(promotionId, today);
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

    /** ROI、首充率（首充数/订单数）、人均充值、CPM（有消耗与曝光近似时） */
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
