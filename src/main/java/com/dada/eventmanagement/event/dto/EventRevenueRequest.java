package com.dada.eventmanagement.event.dto;

import com.dada.eventmanagement.common.enums.EventRevenueType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EventRevenueRequest(
        @NotNull(message = "Revenue type is required") EventRevenueType revenueType,
        @NotNull(message = "Transaction date is required") LocalDate transactionDate,
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
        Long accountId,
        Long paymentMethodId,
        @NotNull(message = "Category is required") Long categoryId,
        Long revenueChannelId,
        Long contactId,
        Integer guestCount,
        String description
) {
}
