package com.dada.eventmanagement.consumption.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConsumptionPlanRequest(
        @NotNull(message = "Inventory item is required") Long inventoryItemId,
        @NotBlank(message = "Category is required") String category,
        @NotNull(message = "Estimated consumption is required") @DecimalMin(value = "0.0", inclusive = false) BigDecimal estimatedConsumptionPerPerson,
        Integer expectedGuestCount,
        @NotNull(message = "Waste percentage is required") @DecimalMin(value = "0.0", inclusive = true) BigDecimal wastePercentage,
        String notes
) {
}
