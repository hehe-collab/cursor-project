package com.drama.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RechargePlan {

    private Integer id;
    private String name;
    private Integer beanCount;
    private Integer extraBean;
    private BigDecimal amount;
    private String rechargeInfo;
    private String payPlatform;
    private String currency;
    private String status;
    private String description;
    private Boolean unlockFullSeries;
    private String planUuid;
    private Integer createdBy;
    private String createdByName;
    private Boolean isRecommended;
    private Boolean isHot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
