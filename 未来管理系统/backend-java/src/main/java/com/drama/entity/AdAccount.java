package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdAccount {

    private Integer id;
    private String media;
    private String country;
    private String subjectName;
    private String accountId;
    private String accountName;
    private String mediaAlias;
    private String accountAgent;
    private Integer createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
