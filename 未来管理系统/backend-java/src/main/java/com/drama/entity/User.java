package com.drama.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** 终端用户（C 端） */
@Schema(description = "C端用户")
@Data
public class User {

    private Integer id;
    /** 8 位展示用编码（与内部主键分离） */
    private String userCode;
    private String username;
    private String phone;
    private String avatar;
    private Integer status;
    private String promoteId;
    private String promoteName;
    private Integer coinBalance;
    private String token;
    /** H5 匿名用户设备标识 */
    private String deviceId;
    private String country;
    private String newUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
