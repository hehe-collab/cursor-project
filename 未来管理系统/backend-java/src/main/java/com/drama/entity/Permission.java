package com.drama.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "权限")
@Data
public class Permission {
    private Integer id;
    private String name;
    private String code;
    private String module;
    private String description;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}