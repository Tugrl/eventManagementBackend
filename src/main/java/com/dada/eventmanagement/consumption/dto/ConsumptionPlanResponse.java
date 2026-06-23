package com.dada.eventmanagement.consumption.dto;

import com.dada.eventmanagement.common.enums.InventoryUnit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsumptionPlanResponse(
        Long id,
        Long eventId,
        Long inventoryItemId,
        String productName,
        String category,
        BigDecimal estimatedConsumptionPerPerson,
        InventoryUnit inputUnit,
        InventoryUnit stockUnit,
        Integer expectedGuestCount,
        BigDecimal wastePercentage,
        BigDecimal unitCost,
        BigDecimal convertedUnitQuantity,
        BigDecimal requiredQuantity,
        BigDecimal totalCost,
        String notes,
        LocalDateTime createdAt
) {
}
