package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserFavorite {
    private Long id;
    private Integer userPk;
    private Integer dramaId;
    private LocalDateTime createdAt;
}
