package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "推广链接")
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

    /** UTM 流量来源，默认 tiktok */
    private String utmSource;
    /** UTM 媒介类型，默认 paid */
    private String utmMedium;
    /** 是否附加 TikTok 宏变量（1=是，0=否） */
    private Integer useTiktokMacros;

    /** LEFT JOIN dramas.public_id：减少二次 IN 查剧 */
    private String joinedDramaPublicId;
}
