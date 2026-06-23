package com.dada.eventmanagement.staff.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public record EventStaffAssignmentItemRequest(
        @NotNull Long employeeId,
        BigDecimal plannedDailyCost,
        BigDecimal servicePoint,
        String notes
) {
}
