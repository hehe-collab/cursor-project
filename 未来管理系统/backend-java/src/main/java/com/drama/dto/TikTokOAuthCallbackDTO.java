package com.drama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** TikTok OAuth 浏览器回跳查询参数（可与 JSON 摘要互转）。 */
@Data
public class TikTokOAuthCallbackDTO {

    @JsonProperty("auth_code")
    private String authCode;

    private String state;

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;
}
