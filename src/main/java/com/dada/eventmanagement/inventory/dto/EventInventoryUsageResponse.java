package com.dada.eventmanagement.inventory.dto;

import com.dada.eventmanagement.common.enums.InventoryUnit;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EventInventoryUsageResponse(
        Long id,
        Long eventId,
        Long inventoryItemId,
        String itemName,
        String category,
        InventoryUnit unit,
        LocalDate usageDate,
        Integer actualGuestCount,
        BigDecimal consumptionQuantity,
        BigDecimal wasteQuantity,
        BigDecimal unitCost,
        BigDecimal totalConsumptionCost,
        BigDecimal totalWasteCost,
        BigDecimal consumptionPerPerson,
        BigDecimal wastePerPerson,
        Long financialTransactionId,
        String notes
) {
}
