package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdMaterialRecord {

    private Integer id;
    private String accountId;
    private String accountName;
    private String status;
    private String taskType;
    private String detail;
    private LocalDateTime createdAt;
}
