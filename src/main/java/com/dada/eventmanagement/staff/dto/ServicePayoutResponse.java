package com.dada.eventmanagement.staff.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ServicePayoutResponse(
        Long id,
        LocalDate payoutDate,
        Long eventId,
        BigDecimal totalServiceAmount,
        BigDecimal totalPoints,
        BigDecimal amountPerPoint,
        Long financialTransactionId,
        String notes,
        List<ServicePayoutItemResponse> items
) {
}
