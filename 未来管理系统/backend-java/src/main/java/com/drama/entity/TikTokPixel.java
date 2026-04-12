package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TikTok Pixel（对齐 tiktok_pixels） */
@Schema(description = "TikTok Pixel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TikTokPixel {

    private Long id;
    private String advertiserId;
    private String pixelId;
    private String pixelName;
    private String pixelCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
