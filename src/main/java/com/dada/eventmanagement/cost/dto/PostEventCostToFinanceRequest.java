package com.dada.eventmanagement.cost.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PostEventCostToFinanceRequest(
        Long accountId,
        Long paymentMethodId,
        @NotNull(message = "Financial category is required") Long categoryId,
        @NotNull(message = "Transaction date is required") LocalDate transactionDate,
        String description
) {
}
