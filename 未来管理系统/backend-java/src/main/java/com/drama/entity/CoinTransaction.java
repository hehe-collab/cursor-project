package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "金币交易记录")
@Data
public class CoinTransaction {
    private Long id;
    private Integer userPk;
    private Integer delta;
    private String reason;
    private String refType;
    private String refId;
    private LocalDateTime createdAt;
}
