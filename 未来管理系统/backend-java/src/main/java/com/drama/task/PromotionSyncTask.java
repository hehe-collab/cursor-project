package com.drama.task;

import com.drama.service.PromotionDetailsService;
import com.drama.service.PromotionTiktokSyncService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionSyncTask {

    private final PromotionTiktokSyncService promotionTiktokSyncService;
    private final PromotionDetailsService promotionDetailsService;

    /** 每 10 分钟：模拟写入 tiktok_cost_records 并刷新当日汇总中的时速等 */
    @Scheduled(cron = "${app.promotion.tiktok-sync-cron}")
    public void syncTikTokData() {
        try {
            promotionTiktokSyncService.runMockSyncAll();
        } catch (Exception e) {
            log.error("TikTok sync failed", e);
        }
    }

    /** 每天凌晨 1 点：按日汇总写入 promotion_details_summary */
    @Scheduled(cron = "${app.promotion.daily-rollup-cron}")
    public void dailySummary() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            promotionDetailsService.rollUpDaily(yesterday);
        } catch (Exception e) {
            log.error("daily rollUp failed", e);
        }
    }
}
