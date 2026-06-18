package com.dada.eventmanagement.inventory.dto;

import com.dada.eventmanagement.common.enums.InventoryMovementType;
import com.dada.eventmanagement.common.enums.InventoryUnit;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryMovementResponse(
        Long id,
        Long inventoryItemId,
        String itemName,
        InventoryUnit unit,
        Long eventId,
        InventoryMovementType movementType,
        LocalDate movementDate,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal totalAmount,
        String notes
) {
}
