package com.dada.eventmanagement.contact.dto;

import com.dada.eventmanagement.common.enums.ContactMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContactMovementRequest(
        @NotNull(message = "Movement date is required") LocalDate movementDate,
        @NotNull(message = "Movement type is required") ContactMovementType movementType,
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
        Long financialTransactionId,
        Long accountId,
        Long paymentMethodId,
        Long categoryId,
        Long revenueChannelId,
        Long eventId,
        String description
) {
}
