package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

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
