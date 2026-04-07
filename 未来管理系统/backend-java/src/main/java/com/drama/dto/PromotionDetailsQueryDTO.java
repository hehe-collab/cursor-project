package com.drama.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class PromotionDetailsQueryDTO {

    private LocalDate startDate;
    private LocalDate endDate;
    private String promotionId;
    private String promotionName;
    private String platform;
    /** 短剧主键 ID，字符串便于与前端一致 */
    private String dramaId;
    private String dramaName;
    private String country;
    private String accountId;
    /** 分页：从 1 开始 */
    private Integer page = 1;
    private Integer pageSize = 20;

    public int getOffset() {
        int p = page == null || page < 1 ? 1 : page;
        int ps = pageSize == null || pageSize < 1 ? 20 : pageSize;
        return (p - 1) * ps;
    }
}
