package com.drama.dto;

import java.math.BigDecimal;
import lombok.Data;

/** 充值记录聚合统计（单行查询结果，列名 snake_case 由 MyBatis 映射为驼峰） */
@Data
public class RechargeStatsRow {

    private Long totalCount;
    private Long pendingCount;
    private Long successCount;
    private Long failedCount;
    private BigDecimal totalAmount;
    private BigDecimal successAmount;
    private Long firstRechargeCount;
    private Long newUserCount;
}
