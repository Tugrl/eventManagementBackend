package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.enums.OperationContext;
import com.dada.eventmanagement.common.enums.OperationSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialTransactionRequest(
        Long eventId,
        Long accountId,
        Long paymentMethodId,
        @NotNull(message = "Category is required") Long categoryId,
        Long revenueChannelId,
        Long contactId,
        @NotNull(message = "Transaction type is required") FinancialTransactionType transactionType,
        @NotNull(message = "Transaction date is required") LocalDate transactionDate,
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
        Integer guestCount,
        String description,
        OperationSource operationSource,
        OperationContext operationContext,
        Long referenceId
) {
}
