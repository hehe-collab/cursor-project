package com.drama.dto;

import lombok.Data;

@Data
public class UserQueryParam {
    private String userId;
    private String username;
    private String token;
    private String promotionId;
    private String country;
    /** 注册时间起（含），YYYY-MM-DD，对应 `created_at` */
    private String startDate;
    /** 注册时间止（含） */
    private String endDate;
    private int page = 1;
    private int pageSize = 20;
}
