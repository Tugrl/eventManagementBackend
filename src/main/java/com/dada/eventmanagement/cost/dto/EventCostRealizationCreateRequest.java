package com.dada.eventmanagement.cost.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EventCostRealizationCreateRequest(
        @NotNull Long costCategoryId,
        @NotBlank String name,
        @NotNull @DecimalMin("0.0") BigDecimal actualUnitCost,
        @NotNull @DecimalMin("0.0") BigDecimal actualQuantity,
        String notes
) {
}
