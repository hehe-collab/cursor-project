package com.drama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** TikTok OAuth access_token / refresh_token 接口响应。 */
@Data
public class TikTokTokenResponseDTO {

    private Integer code;
    private String message;
    private TokenData data;

    @Data
    public static class TokenData {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("advertiser_id")
        private String advertiserId;

        @JsonProperty("advertiser_name")
        private String advertiserName;

        @JsonProperty("expires_in")
        private Long expiresIn;

        @JsonProperty("refresh_token_expires_in")
        private Long refreshTokenExpiresIn;

        private String scope;
    }
}
