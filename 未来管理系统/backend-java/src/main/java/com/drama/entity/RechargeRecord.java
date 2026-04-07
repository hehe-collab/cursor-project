package com.drama.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 充值记录 */
@Data
public class RechargeRecord {

    private Long id;
    private String orderNo;
    private String userId;
    private Integer dramaId;
    private String dramaName;
    private BigDecimal amount;
    private Integer coins;
    private String paymentStatus;
    private String payStatus;
    private String promotionId;
    private String promoteId;
    private String newUserId;
    private Boolean isFirstRecharge;
    private Boolean isNewUser;
    private LocalDateTime localRegisterTime;
    private LocalDateTime localOrderTime;
    private LocalDateTime localTime;
    private String country;
    private String externalOrderId;
    private String externalOrderNo;
    private String paymentMethod;
    /** 媒体/渠道（统计 media 筛选） */
    private String platform;
    private String adAccountId;
    private String adAccountName;
    private Boolean callbackSent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** LEFT JOIN users：列表联查填充，非表字段 */
    private String joinUsername;
    private String joinUserPromoteId;
    private String joinUserCountry;
}
