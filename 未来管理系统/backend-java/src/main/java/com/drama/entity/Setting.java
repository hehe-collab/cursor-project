package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Setting {
    private Integer id;
    private String keyName;
    private String value;
    private LocalDateTime updatedAt;
}
