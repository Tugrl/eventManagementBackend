package com.dada.eventmanagement.staff.dto;

import java.math.BigDecimal;

public record ServicePayoutItemResponse(
        Long employeeId,
        String employeeName,
        BigDecimal points,
        BigDecimal payoutAmount
) {
}
