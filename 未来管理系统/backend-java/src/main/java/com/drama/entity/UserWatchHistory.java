package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "用户观看记录")
@Data
public class UserWatchHistory {
    private Long id;
    private Integer userPk;
    private Integer dramaId;
    private Integer episodeNum;
    private Integer progressSec;
    private LocalDateTime updatedAt;
}
