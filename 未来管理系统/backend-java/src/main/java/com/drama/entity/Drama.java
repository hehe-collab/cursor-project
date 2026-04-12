package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "短剧")
@Data
public class Drama {
    private Integer id;
    /** 业务侧 15 位剧 ID（内部仍用 id 做主键与关联） */
    private String publicId;
    /** 列表联表查询时由 `categories.name` 填充，非表字段持久化 */
    private String categoryName;

    private String title;
    private String cover;
    private String description;
    private Integer categoryId;
    private String status;
    private Integer viewCount;
    private Integer sort;
    private String displayName;
    private String displayText;
    private Integer beansPerEpisode;
    private Integer totalEpisodes;
    private Integer freeEpisodes;
    private String ossPath;
    private String category;
    private String taskStatus;
    private Integer isOnline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
