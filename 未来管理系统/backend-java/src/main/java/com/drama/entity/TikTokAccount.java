package com.drama.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TikTok OAuth / 账户（#096 + #095-4 DDL）。库表无 admin_id 字段，不设 adminId。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TikTokAccount {

    private Long id;
    private String advertiserId;
    private String advertiserName;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;
    private String currency;
    private String timezone;
    private BigDecimal balance;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
