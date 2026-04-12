package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "广告素材记录")
@Data
public class AdMaterialRecord {

    private Integer id;
    private String accountId;
    private String accountName;
    private String status;
    private String taskType;
    private String detail;
    private LocalDateTime createdAt;
}
