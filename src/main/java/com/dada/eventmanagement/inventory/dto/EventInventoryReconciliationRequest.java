package com.dada.eventmanagement.inventory.dto;

import com.dada.eventmanagement.common.enums.InventoryReconciliationStatus;
import com.dada.eventmanagement.common.enums.InventoryUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EventInventoryReconciliationRequest(
        @NotNull InventoryUnit inputUnit,
        @NotNull @DecimalMin("0.0") BigDecimal actualConsumption,
        @NotNull @DecimalMin("0.0") BigDecimal manualWaste,
        @NotNull @Min(1) Integer actualGuestCount,
        @NotNull LocalDate usageDate,
        @NotNull InventoryReconciliationStatus status,
        String notes
) {
}
