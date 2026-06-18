package com.dada.eventmanagement.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EventInventoryUsageRequest(
        @NotNull(message = "Event is required") Long eventId,
        @NotNull(message = "Inventory item is required") Long inventoryItemId,
        @NotNull(message = "Usage date is required") LocalDate usageDate,
        @NotNull(message = "Actual guest count is required") @Min(value = 1, message = "Actual guest count must be greater than zero") Integer actualGuestCount,
        @NotNull(message = "Consumption quantity is required") @DecimalMin(value = "0.00", message = "Consumption quantity cannot be negative") BigDecimal consumptionQuantity,
        @NotNull(message = "Waste quantity is required") @DecimalMin(value = "0.00", message = "Waste quantity cannot be negative") BigDecimal wasteQuantity,
        BigDecimal unitCost,
        Long accountId,
        Long paymentMethodId,
        @NotNull(message = "Financial category is required") Long categoryId,
        String notes
) {
}
