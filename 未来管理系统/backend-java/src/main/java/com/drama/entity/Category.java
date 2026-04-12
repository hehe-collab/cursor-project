package com.drama.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "短剧分类")
@Data
public class Category {
    private Integer id;
    private String name;

    /** URL 友好标识，SEO 使用 */
    private String slug;

    /** 分类描述 */
    private String description;

    /** 排序权重（数值越小越靠前） */
    @JsonProperty("sort_order")
    private Integer sort;

    /** 是否启用：1=启用 0=禁用 */
    @JsonProperty("is_enabled")
    private Boolean isEnabled;

    /** 关联短剧数（子查询填充，非表字段） */
    @JsonProperty("drama_count")
    private Integer dramaCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
