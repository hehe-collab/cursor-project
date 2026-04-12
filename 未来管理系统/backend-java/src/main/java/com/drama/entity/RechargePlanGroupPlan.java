package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "充值方案组-方案关联")
@Data
public class RechargePlanGroupPlan {

    private Long id;
    private Integer groupId;
    private Integer planId;
    private Integer sortOrder;
}
