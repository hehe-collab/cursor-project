package com.drama.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理员账号")
@Data
public class Admin {
    private Integer id;
    private String username;
    private String password;
    private String nickname;

    /** 角色代码（如 super_admin），旧字段兼容保留 */
    private String role;

    /** 是否已修改过初始密码（0=未改，1=已改）；用于强制首次修改密码 */
    @JsonProperty("password_changed")
    private Integer passwordChanged;

    /** 关联角色 ID（取代 role string，关联 roles 表） */
    @JsonProperty("role_id")
    private Integer roleId;

    /** 非表字段：关联的角色对象 */
    private Role roleObj;

    /** 非表字段：权限代码列表（用于前端判断） */
    private List<String> permissions;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}