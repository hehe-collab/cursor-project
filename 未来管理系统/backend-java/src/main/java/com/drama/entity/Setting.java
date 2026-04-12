package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "系统配置项")
@Data
public class Setting {
    private Integer id;
    private String keyName;
    private String value;
    private LocalDateTime updatedAt;
}
