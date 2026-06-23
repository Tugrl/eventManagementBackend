package com.dada.eventmanagement.inventory.dto;

import com.dada.eventmanagement.common.enums.InventoryReconciliationStatus;
import com.dada.eventmanagement.common.enums.InventoryUnit;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EventInventoryReconciliationResponse(
        Long id,
        Long eventId,
        Long inventoryItemId,
        String itemName,
        InventoryUnit stockUnit,
        InventoryUnit inputUnit,
        BigDecimal plannedQuantity,
        BigDecimal actualConsumptionInput,
        BigDecimal manualWasteInput,
        BigDecimal actualConsumptionStock,
        BigDecimal totalWasteStock,
        BigDecimal automaticWasteMl,
        BigDecimal deductedQuantity,
        BigDecimal previouslyProcessedQuantity,
        BigDecimal unitCost,
        BigDecimal totalCost,
        Integer actualGuestCount,
        LocalDate usageDate,
        InventoryReconciliationStatus status,
        Boolean finalized,
        Long inventoryMovementId,
        String notes
) {
}
