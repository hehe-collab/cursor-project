package com.drama.entity;

import lombok.Data;

@Data
public class RechargePlanGroupPlan {

    private Long id;
    private Integer groupId;
    private Integer planId;
    private Integer sortOrder;
}
