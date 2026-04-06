package com.drama.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CallbackConfig {

    private Integer id;

    @JsonProperty("link_id")
    private String linkId;

    private String platform;

    @JsonProperty("cold_start_count")
    private Integer coldStartCount;

    @JsonProperty("min_price_limit")
    private Integer minPriceLimit;

    /** 复充回传：true 开启 */
    @JsonProperty("replenish_callback_enabled")
    private Boolean replenishCallbackEnabled;

    /** 策略 JSON，如 {"strategies":[{"amount_min":0,"amount_max":100,"params":"..."}]} */
    @JsonProperty("config_json")
    private String configJson;

    private String creator;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
