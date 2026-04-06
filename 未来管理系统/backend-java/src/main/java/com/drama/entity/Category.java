package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Category {
    private Integer id;
    private String name;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
