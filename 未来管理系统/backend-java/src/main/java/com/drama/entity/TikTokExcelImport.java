package com.drama.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TikTok Excel 导入批次（对齐 tiktok_excel_imports） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TikTokExcelImport {

    private Long id;
    private String advertiserId;
    private String importType;
    private String filePath;
    private String originalFilename;
    private Long fileSize;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private String errorLogs;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
