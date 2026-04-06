package com.drama.dto;

import lombok.Data;

/** 短剧表聚合统计（与 `dramas` 实际列一致） */
@Data
public class DramaStatsRow {

    private Long totalCount;
    private Long publishedCount;
    private Long draftCount;
    private Long offlineCount;
    private Long totalViews;
    /** SUM(total_episodes)，各剧「标称总集数」之和 */
    private Long totalEpisodesSum;
}
