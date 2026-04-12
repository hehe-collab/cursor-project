package com.drama.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "广告账户")
public class AdAccount {

    @Schema(description = "账户ID")
    private Integer id;

    @Schema(description = "账户媒体", example = "TikTok")
    private String media;

    @Schema(description = "国家", example = "TH")
    private String country;

    @Schema(description = "账户主体/主体名称")
    private String subjectName;

    @Schema(description = "广告主ID")
    private String accountId;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "媒体别名")
    private String mediaAlias;

    @Schema(description = "账户代理")
    private String accountAgent;

    @Schema(description = "访问令牌（加密，AES-256）")
    @JsonIgnore
    private String accessTokenEncrypted;

    @Schema(description = "刷新令牌（加密，AES-256）")
    @JsonIgnore
    private String refreshTokenEncrypted;

    @Schema(description = "令牌过期时间")
    private LocalDateTime tokenExpiresAt;

    @Schema(description = "创建人ID")
    private Integer createdBy;

    @Schema(description = "创建人名称")
    private String createdByName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // 临时字段，不存数据库，用于 API 传参
    @JsonIgnore
    private transient String accessToken;

    @JsonIgnore
    private transient String refreshToken;
}
