package com.dada.eventmanagement.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FinalTicketPriceRequest(
        @NotNull(message = "Final ticket price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Final ticket price must be positive")
        BigDecimal finalTicketPrice
) {
}
