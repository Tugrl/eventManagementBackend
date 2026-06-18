package com.dada.eventmanagement.consumption.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsumptionPlanResponse(
        Long id,
        Long eventId,
        Long inventoryItemId,
        String productName,
        String category,
        BigDecimal estimatedConsumptionPerPerson,
        Integer expectedGuestCount,
        BigDecimal wastePercentage,
        BigDecimal unitCost,
        BigDecimal requiredQuantity,
        BigDecimal totalCost,
        String notes,
        LocalDateTime createdAt
) {
}
