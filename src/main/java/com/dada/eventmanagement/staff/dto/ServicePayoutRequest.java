package com.dada.eventmanagement.staff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ServicePayoutRequest(
        @NotNull(message = "Payout date is required") LocalDate payoutDate,
        Long eventId,
        Long accountId,
        Long paymentMethodId,
        @NotNull(message = "Financial category is required") Long categoryId,
        @NotNull(message = "Total service amount is required") @DecimalMin(value = "0.01", message = "Total service amount must be greater than zero") BigDecimal totalServiceAmount,
        String notes,
        @NotEmpty(message = "At least one employee is required") List<@Valid ServicePayoutItemRequest> items
) {
}
