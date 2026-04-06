package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserWatchHistory {
    private Long id;
    private Integer userPk;
    private Integer dramaId;
    private Integer episodeNum;
    private Integer progressSec;
    private LocalDateTime updatedAt;
}
