package com.dada.eventmanagement.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceDocumentSettlementRequest(
        @NotNull(message = "Settlement date is required") LocalDate settlementDate,
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
        @NotNull(message = "Account is required") Long accountId,
        Long paymentMethodId,
        String notes
) {
}
