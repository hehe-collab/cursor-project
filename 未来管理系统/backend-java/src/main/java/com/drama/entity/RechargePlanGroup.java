package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "充值方案组")
@Data
public class RechargePlanGroup {

    private Integer id;
    private String name;
    private String groupName;
    /** 对应 API 字段 group_id（RG_ 前缀业务 ID） */
    private String groupPublicId;
    private Integer sortOrder;
    private String description;
    private String status;
    private String groupUuid;
    private String itemNo;
    private String itemToken;
    private String mediaPlatform;
    private String pixelId;
    private String pixelToken;
    private String creator;
    private Integer createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
