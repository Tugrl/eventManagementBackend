package com.dada.eventmanagement.inventory.dto;

import com.dada.eventmanagement.common.enums.InventoryMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryMovementRequest(
        @NotNull(message = "Inventory item is required") Long inventoryItemId,
        Long eventId,
        @NotNull(message = "Movement type is required") InventoryMovementType movementType,
        @NotNull(message = "Movement date is required") LocalDate movementDate,
        @NotNull(message = "Quantity is required") @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero") BigDecimal quantity,
        BigDecimal unitCost,
        String notes
) {
}
