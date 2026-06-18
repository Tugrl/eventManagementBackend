package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.FinancialTransactionStatus;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FinancialTransactionResponse(
        Long id,
        Long eventId,
        Long accountId,
        String accountName,
        Long paymentMethodId,
        String paymentMethodName,
        Long categoryId,
        String categoryName,
        Long revenueChannelId,
        String revenueChannelName,
        Long contactId,
        FinancialTransactionType transactionType,
        LocalDate transactionDate,
        BigDecimal amount,
        Integer guestCount,
        String description,
        FinancialTransactionStatus status,
        LocalDateTime createdAt
) {
}
