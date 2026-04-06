package com.drama.dto;

import lombok.Data;

@Data
public class CallbackLogStatsRow {

    private Long total;
    private Long success;
    private Long failed;
    private Long pending;
}
