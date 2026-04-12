package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "短剧标签关联")
@Data
public class DramaTag {
    private Integer dramaId;
    private Integer tagId;
    private LocalDateTime createdAt;
}
