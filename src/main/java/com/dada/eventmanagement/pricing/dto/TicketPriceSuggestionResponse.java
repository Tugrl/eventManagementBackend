package com.dada.eventmanagement.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketPriceSuggestionResponse(
        Long id,
        Long eventId,
        BigDecimal totalEstimatedCost,
        BigDecimal targetProfitMargin,
        BigDecimal targetRevenue,
        Integer expectedGuestCount,
        BigDecimal suggestedTicketPrice,
        LocalDateTime createdAt
) {
}
