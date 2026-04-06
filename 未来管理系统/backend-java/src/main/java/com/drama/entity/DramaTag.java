package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DramaTag {
    private Integer dramaId;
    private Integer tagId;
    private LocalDateTime createdAt;
}
