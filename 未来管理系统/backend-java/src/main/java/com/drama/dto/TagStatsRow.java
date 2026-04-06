package com.drama.dto;

import lombok.Data;

/** 标签及 drama_tags 关联聚合 */
@Data
public class TagStatsRow {

    private Long totalCount;
    /** 至少在一条关联中出现过的不重复标签数 */
    private Long tagsLinkedCount;
    /** drama_tags 行数 */
    private Long totalLinks;
    /** 至少打了一个标签的短剧数 */
    private Long dramasWithTagsCount;
}
