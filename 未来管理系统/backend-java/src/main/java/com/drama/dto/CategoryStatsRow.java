package com.drama.dto;

import lombok.Data;

/** 分类相关聚合统计 */
@Data
public class CategoryStatsRow {

    private Long totalCount;
    /** 至少关联了一个分类的短剧数（category_id 非空） */
    private Long dramasLinkedCount;
    /** 短剧总条数 */
    private Long totalDramas;
}
