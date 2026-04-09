package com.drama.integration.tiktok;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TiktokCampaignReportRow {
    String campaignId;
    String campaignName;
    String statTimeDay;
    BigDecimal spend;
    long impressions;
}
