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
}
