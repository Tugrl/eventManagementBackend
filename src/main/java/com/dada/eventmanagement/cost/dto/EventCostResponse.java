package com.dada.eventmanagement.cost.dto;

import com.dada.eventmanagement.common.enums.CalculationType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventCostResponse(
        Long id,
        Long eventId,
        Long costCategoryId,
        String name,
        String description,
        CalculationType calculationType,
        BigDecimal unitCost,
        BigDecimal quantity,
        BigDecimal totalCost,
        Boolean isEstimated,
        Long financialTransactionId,
        LocalDateTime createdAt
) {
}
