package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "广告任务")
@Data
public class AdTask {

    private Integer id;
    private String taskId;
    private String accountIds;
    private String accountNames;
    private String promotionType;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    /** JSON 字符串 */
    private String configJson;
}
