package com.drama.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "短剧标签")
@Data
public class Tag {
    private Integer id;
    private String name;

    /** 标签颜色（HEX），如 #409EFF */
    private String color;

    /** 排序权重 */
    @JsonProperty("sort_order")
    private Integer sortOrder;

    /** 是否热门：1=热门 0=普通 */
    @JsonProperty("is_hot")
    private Boolean isHot;

    /** 使用次数（由 drama_tags COUNT 维护，非表字段） */
    @JsonProperty("usage_count")
    private Integer usageCount;

    /** 关联短剧数（子查询填充，非表字段） */
    @JsonProperty("drama_count")
    private Integer dramaCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
