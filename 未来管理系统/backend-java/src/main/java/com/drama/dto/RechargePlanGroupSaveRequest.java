package com.drama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RechargePlanGroupSaveRequest {

    private Integer id;
    private String name;
    private String groupName;
    /** 展示用分组 ID，一般不入库修改（由系统或迁移写入） */
    private String groupId;

    private List<Integer> planIds;
    private List<Integer> rechargePlanIds;

    private Integer sortOrder;
    private String description;
    private String status;
    private String itemNo;
    private String itemToken;
    private String groupUuid;
    private String mediaPlatform;
    private String pixelId;
    private String pixelToken;

    private String creator;
    private Integer createdBy;
    private String createdByName;
}
