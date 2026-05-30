package com.drama.util;

import com.drama.entity.TikTokAdGroup;
import java.math.BigDecimal;
import org.springframework.util.StringUtils;

/**
 * 批量工具「优化目标」→ TikTok 广告组 API 字段映射。
 *
 * <p>UI/Excel 语义：
 * <ul>
 *   <li>价值 / 价值 ROAS / value_roas → TikTok {@code optimization_goal=VALUE} + {@code roas_bid}</li>
 *   <li>转化 / conversion → TikTok {@code optimization_goal=CONVERT} + {@code bid_price}</li>
 * </ul>
 */
public final class TikTokOptimizationHelper {

    public static final String GOAL_VALUE = "VALUE";
    public static final String GOAL_CONVERT = "CONVERT";

    private TikTokOptimizationHelper() {}

    public enum SemanticGoal {
        VALUE_ROAS,
        CONVERSION
    }

    public static SemanticGoal parseSemanticGoal(String optimizationGoal) {
        if (!StringUtils.hasText(optimizationGoal)) {
            return SemanticGoal.CONVERSION;
        }
        String g = optimizationGoal.trim().toLowerCase();
        if (g.contains("roas")
                || g.contains("价值")
                || "value".equals(g)
                || "value_roas".equals(g)
                || "vo_min_roas".equals(g)) {
            return SemanticGoal.VALUE_ROAS;
        }
        return SemanticGoal.CONVERSION;
    }

    /**
     * 根据 UI 优化目标与出价，填充 TikTok 广告组创建参数。
     * ROAS 出价写入 {@code roas_bid}；转化出价写入 {@code bid_price}。
     */
    public static void applyToAdGroupBuilder(
            TikTokAdGroup.TikTokAdGroupBuilder builder, String optimizationGoal, BigDecimal price) {
        SemanticGoal semantic = parseSemanticGoal(optimizationGoal);
        builder.bidType("BID_TYPE_CUSTOM");
        builder.billingEvent("OCPM");
        builder.optimizationGoal(semantic == SemanticGoal.VALUE_ROAS ? GOAL_VALUE : GOAL_CONVERT);
        if (semantic == SemanticGoal.VALUE_ROAS) {
            builder.roasBid(price);
            builder.deepBidType("VO_MIN_ROAS");
            builder.bidPrice(null);
        } else {
            builder.bidPrice(price);
            builder.roasBid(null);
            builder.deepBidType(null);
        }
    }
}
