package com.dada.eventmanagement.cost.dto;

import com.dada.eventmanagement.common.enums.CalculationType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record EventCostRequest(
        @NotNull(message = "Cost category is required") Long costCategoryId,
        @NotBlank(message = "Cost name is required") String name,
        String description,
        @NotNull(message = "Calculation type is required") CalculationType calculationType,
        @NotNull(message = "Unit cost is required") @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitCost,
        @NotNull(message = "Quantity is required") @DecimalMin(value = "0.0", inclusive = true) BigDecimal quantity,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal totalCost,
        @NotNull(message = "Estimated flag is required") Boolean isEstimated
) {
}
