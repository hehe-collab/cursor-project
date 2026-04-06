package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Tag {
    private Integer id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 子查询填充：关联短剧数，非表字段 */
    private Integer dramaCount;
}
