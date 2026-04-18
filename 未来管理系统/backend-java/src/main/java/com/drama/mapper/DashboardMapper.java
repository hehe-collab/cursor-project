package com.drama.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DashboardMapper {

    long countUsers();

    long countDramas();

    long countRecharges();

    BigDecimal sumRechargeAmountPaid();

    long countUsersOnDate(@Param("d") LocalDate d);

    long countRechargesOnDate(@Param("d") LocalDate d);

    BigDecimal sumRechargeAmountPaidOnDate(@Param("d") LocalDate d);

    List<Map<String, Object>> usersByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Map<String, Object>> rechargesByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Map<String, Object>> amountPaidByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** {@code daily_stats} 按日汇总播放量类指标（无数据则趋势为 0） */
    List<Map<String, Object>> viewsByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 所选日期区间内：成功支付笔数（与 PaidFilter 一致） */
    long countRechargePaidInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 所选日期区间内：待支付笔数 */
    long countRechargePendingInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 区间内其余笔数（失败/取消等，非成功非待支付） */
    long countRechargeOtherInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 区间内按国家统计充值金额（已支付） */
    List<Map<String, Object>> rechargeAmountByCountry(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 区间内按平台/渠道统计充值金额（已支付） */
    List<Map<String, Object>> rechargeAmountByPlatform(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 区间内首充用户数 */
    long countFirstRechargeInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 区间内充值用户数（去重） */
    long countRechargeUsersInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 昨日新增用户数 */
    long countUsersOnDate2(@Param("d") LocalDate d);

    /** 昨日充值金额（已支付） */
    BigDecimal sumRechargeAmountPaidOnDate2(@Param("d") LocalDate d);

    /* ---- 短剧概览 ---- */

    /** 上架短剧数 */
    long countOnlineDramas();

    /** 所有短剧总集数 */
    long sumTotalEpisodes();

    /** 按剧聚合充值排行（分页） */
    List<Map<String, Object>> dramaRankingList(
            @Param("start") LocalDate start, @Param("end") LocalDate end,
            @Param("dramaName") String dramaName, @Param("categoryId") Integer categoryId,
            @Param("offset") int offset, @Param("limit") int limit);

    /** 排行总条数（分页用） */
    long dramaRankingCount(
            @Param("start") LocalDate start, @Param("end") LocalDate end,
            @Param("dramaName") String dramaName, @Param("categoryId") Integer categoryId);

    /** 排行汇总行 */
    Map<String, Object> dramaRankingSummary(
            @Param("start") LocalDate start, @Param("end") LocalDate end,
            @Param("dramaName") String dramaName, @Param("categoryId") Integer categoryId);

    /** 按分类聚合充值金额 */
    List<Map<String, Object>> rechargeAmountByCategory(
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 指定短剧的每日充值趋势 */
    List<Map<String, Object>> dramaDailyRecharge(
            @Param("dramaId") Integer dramaId,
            @Param("start") LocalDate start, @Param("end") LocalDate end);
}
