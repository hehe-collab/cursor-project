package com.drama.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CallbackConfig {

    private Integer id;

    @JsonProperty("link_id")
    private String linkId;

    /** 列表展示：业务推广ID（与投放链接 promote_id 一致）；库内 link_id 若为数字主键则由 CallbackService 列表接口回填 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("promote_id")
    private String promoteId;

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
