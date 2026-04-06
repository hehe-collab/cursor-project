package com.drama.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdMaterial {

    private Integer id;
    private String materialId;
    private String materialName;
    private String type;
    private String entityName;
    private String accountId;
    private String videoId;
    private String coverUrl;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
