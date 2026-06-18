package com.dada.eventmanagement.cost.dto;

import java.math.BigDecimal;
import java.util.List;

public record CostSummaryResponse(
        Long eventId,
        BigDecimal totalEstimatedCost,
        BigDecimal fixedCostTotal,
        BigDecimal perPersonCostTotal,
        BigDecimal staffCostTotal,
        BigDecimal unitCostTotal,
        BigDecimal manualCostTotal,
        List<CategoryBreakdown> categoryBreakdown
) {
    public record CategoryBreakdown(String categoryName, BigDecimal totalCost) {
    }
}
