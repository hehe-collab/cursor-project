package com.drama.dto;

import lombok.Data;

/** users 表聚合统计（列名与 MyBatis map-underscore-to-camel-case 对齐） */
@Data
public class UserStatsRow {

    private Long totalCount;
    private Long activeCount;
    private Long inactiveCount;
    private Long totalCoins;
}
