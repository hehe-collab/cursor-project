package com.drama.dto;

import com.drama.entity.PromotionDetailsSummary;
import java.util.List;
import lombok.Data;

@Data
public class PromotionDetailsResponseDTO {

    private List<PromotionDetailsSummary> list;
    /** 当前筛选条件下的汇总行（由各日明细聚合后再算衍生指标） */
    private PromotionDetailsSummary summary;
    private Long total;
}
