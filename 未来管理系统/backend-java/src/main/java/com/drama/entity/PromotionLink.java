package com.drama.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PromotionLink {

    private Integer id;
    private String promoteId;
    private String platform;
    private String country;
    private String promoteName;
    private Integer dramaId;
    private Integer planGroupId;
    private Integer beanCount;
    private Integer freeEpisodes;
    private Integer previewEpisodes;
    private String domain;
    private String dramaName;
    private String status;
    private String stat;
    private BigDecimal amount;
    private BigDecimal spend;
    private String target;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
