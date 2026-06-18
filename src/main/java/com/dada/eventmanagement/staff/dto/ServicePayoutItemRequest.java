package com.dada.eventmanagement.staff.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ServicePayoutItemRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        BigDecimal points
) {
}
