package com.drama.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "角色")
@Data
public class Role {
    private Integer id;
    private String name;
    private String code;
    private String description;

    @JsonProperty("is_system")
    private Boolean isSystem;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    /** 非表字段：关联的权限列表 */
    private List<Permission> permissions;
}