package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 剧集（表 drama_episodes） */
@Data
public class DramaEpisode {
    private Integer id;
    private Integer dramaId;
    private Integer episodeNum;
    private String title;
    private String videoId;
    private String videoUrl;
    private Integer duration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
