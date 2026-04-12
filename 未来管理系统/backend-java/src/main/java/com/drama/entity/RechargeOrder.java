package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "充值订单")
@Data
public class RechargeOrder {

    private Integer id;
    private String orderId;
    private Integer userId;
    private String promotionId;
    private BigDecimal amount;
    private Integer coins;
    private Boolean isFirstRecharge;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
