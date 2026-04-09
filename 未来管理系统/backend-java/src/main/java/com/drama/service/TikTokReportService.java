package com.drama.service;

import com.drama.config.TikTokIntegrationProperties;
import com.drama.entity.TikTokReport;
import com.drama.integration.tiktok.TikTokIntegratedReportClient;
import com.drama.integration.tiktok.TiktokCampaignReportRow;
import com.drama.mapper.TikTokReportMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TikTokReportService {

    private final TikTokReportMapper reportMapper;
    private final TikTokAccountService accountService;
    private final TikTokIntegratedReportClient integratedReportClient;
    private final TikTokIntegrationProperties integrationProperties;

    private static final String DIM_CAMPAIGN = "campaign";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    public TikTokReport getById(Long id) {
        TikTokReport r = reportMapper.selectById(id);
        if (r == null) {
            throw new IllegalStateException("Report not found: " + id);
        }
        return r;
    }

    public TikTokReport getReportById(Long id) {
        return getById(id);
    }

    /**
     * 本地查询：{@code dimensions} 非空时按维度+日期；否则仅按广告主与日期。
     */
    public List<TikTokReport> getReports(
            String advertiserId, String dimensions, LocalDate startDate, LocalDate endDate) {
        if (StringUtils.hasText(dimensions)) {
            return reportMapper.selectByAdvertiserDimensionsAndDateRange(
                    advertiserId, dimensions, startDate, endDate);
        }
        return reportMapper.selectByAdvertiserAndDateRange(advertiserId, startDate, endDate);
    }

    public TikTokReport getByUniqueKey(
            String advertiserId, String dimensions, String dimensionId, LocalDate statDate) {
        TikTokReport r =
                reportMapper.selectByUniqueKey(advertiserId, dimensions, dimensionId, statDate);
        if (r == null) {
            throw new IllegalStateException("Report not found for key");
        }
        return r;
    }

    public List<TikTokReport> listByAdvertiserAndDateRange(
            String advertiserId, LocalDate start, LocalDate end) {
        return reportMapper.selectByAdvertiserAndDateRange(advertiserId, start, end);
    }

    public List<TikTokReport> listByAdvertiserDimensionsAndDateRange(
            String advertiserId, String dimensions, LocalDate start, LocalDate end) {
        return reportMapper.selectByAdvertiserDimensionsAndDateRange(
                advertiserId, dimensions, start, end);
    }

    @Transactional
    public void upsertReport(TikTokReport row) {
        reportMapper.upsert(row);
    }

    @Transactional
    public void batchUpsertReports(List<TikTokReport> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        reportMapper.batchUpsert(rows);
    }

    @Transactional
    public void deleteById(Long id) {
        getById(id);
        reportMapper.deleteById(id);
    }

    /**
     * 按日期区间同步报表：当前实现为对区间内每一天调用 {@link #syncAuctionCampaignDayFromTikTok}（等价 Monica 稿中
     * campaign 维度 + Integrated Get）。
     *
     * <p>非 {@code campaign} 维度仅记录 warn，仍按 campaign 写入（与现有客户端能力一致）。
     */
    @Transactional
    public List<TikTokReport> syncReportsFromTikTok(
            String advertiserId, String dimensions, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("startDate/endDate 无效");
        }
        if (StringUtils.hasText(dimensions) && !DIM_CAMPAIGN.equalsIgnoreCase(dimensions)) {
            log.warn(
                    "syncReportsFromTikTok: 仅支持 campaign 粒度同步，收到 dimensions={}，将仍使用 Integrated Campaign 报表",
                    dimensions);
        }
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            syncAuctionCampaignDayFromTikTok(advertiserId, d);
        }
        return reportMapper.selectByAdvertiserDimensionsAndDateRange(
                advertiserId, DIM_CAMPAIGN, startDate, endDate);
    }

    /**
     * 拉取 TikTok 竞价推广系列按日报表（与 {@link TikTokIntegratedReportClient} 一致），写入 {@code
     * tiktok_reports}（维度 {@code campaign} / {@code dimension_id = campaign_id}）。
     */
    @Transactional
    public List<TikTokReport> syncAuctionCampaignDayFromTikTok(String advertiserId, LocalDate day) {
        String token = accountService.getValidAccessToken(advertiserId);
        int pageSize = Math.max(1, integrationProperties.getReportPageSize());
        List<TiktokCampaignReportRow> rows =
                integratedReportClient.fetchAuctionCampaignDay(advertiserId, token, day, pageSize);
        List<TikTokReport> mapped = new ArrayList<>();
        for (TiktokCampaignReportRow row : rows) {
            LocalDate statDate;
            try {
                statDate = LocalDate.parse(row.getStatTimeDay(), DAY);
            } catch (Exception e) {
                statDate = day;
            }
            mapped.add(
                    TikTokReport.builder()
                            .advertiserId(advertiserId)
                            .dimensions(DIM_CAMPAIGN)
                            .dimensionId(row.getCampaignId())
                            .statDate(statDate)
                            .spend(row.getSpend())
                            .impressions((int) Math.min(Integer.MAX_VALUE, row.getImpressions()))
                            .build());
        }
        if (!mapped.isEmpty()) {
            reportMapper.batchUpsert(mapped);
        }
        log.info(
                "Synced tiktok_reports auction_campaign day advertiser={} date={} rows={}",
                advertiserId,
                day,
                mapped.size());
        return listByAdvertiserDimensionsAndDateRange(advertiserId, DIM_CAMPAIGN, day, day);
    }
}
