package com.drama.dto;

import lombok.Data;

@Data
public class RechargeQueryParam {
    private String userId;
    private String promotionId;
    private String orderId;
    private String externalOrderId;
    /** 投放媒体/渠道 */
    private String platform;
    /** 广告账户 ID */
    private String accountId;
    private String country;
    private String startDate;
    private String endDate;
    private String status;
    private int page = 1;
    private int pageSize = 20;
}
