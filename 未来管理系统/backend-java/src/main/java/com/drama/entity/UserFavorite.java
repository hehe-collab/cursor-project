package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "用户收藏")
@Data
public class UserFavorite {
    private Long id;
    private Integer userPk;
    private Integer dramaId;
    private LocalDateTime createdAt;
}
