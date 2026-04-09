package com.drama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** TikTok Open API 通用外层结构（业务字段在 {@code data}）。 */
@Data
public class TikTokApiResponseDTO<T> {

    private Integer code;
    private String message;
    private T data;

    @JsonProperty("request_id")
    private String requestId;

    public boolean isSuccess() {
        return code != null && code == 0;
    }
}
