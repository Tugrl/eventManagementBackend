package com.dada.eventmanagement.inventory.dto;

import com.dada.eventmanagement.common.enums.InventoryUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InventoryItemRequest(
        @NotBlank(message = "Name is required") String name,
        String category,
        @NotNull(message = "Unit is required") InventoryUnit unit,
        @NotNull(message = "Unit cost is required") @DecimalMin(value = "0.00", message = "Unit cost cannot be negative") BigDecimal unitCost,
        @NotNull(message = "Current quantity is required") @DecimalMin(value = "0.00", message = "Current quantity cannot be negative") BigDecimal currentQuantity,
        @NotNull(message = "Minimum quantity is required") @DecimalMin(value = "0.00", message = "Minimum quantity cannot be negative") BigDecimal minimumQuantity,
        BigDecimal bottleVolumeMl
) {
}
