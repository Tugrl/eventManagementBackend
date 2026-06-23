package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.FinancialTransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FinanceDocumentSettlementResponse(
        Long id,
        Long documentId,
        Long financialTransactionId,
        FinancialTransactionStatus financialTransactionStatus,
        LocalDate settlementDate,
        BigDecimal amount,
        Long accountId,
        Long paymentMethodId,
        String notes,
        LocalDateTime createdAt
) {
}
