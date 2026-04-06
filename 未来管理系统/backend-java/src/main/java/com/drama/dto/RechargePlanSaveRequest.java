package com.drama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RechargePlanSaveRequest {

    private Integer id;
    private String name;
    private Boolean unlockFullSeries;
    private Integer actualCoins;
    private Integer bonusCoins;
    private Integer beanCount;
    private Integer extraBean;
    private String paymentPlatform;
    private String payPlatform;
    private BigDecimal amount;
    private String currency;
    private String rechargeInfo;
    private String description;
    private Boolean isRecommended;
    private Boolean isHot;
    private String status;
}
