package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TitlePack {

    private Integer id;
    private String name;
    private String content;
    private Integer createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
