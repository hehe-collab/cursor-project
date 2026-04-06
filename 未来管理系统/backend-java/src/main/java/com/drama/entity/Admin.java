package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 后台管理员 */
@Data
public class Admin {

    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
