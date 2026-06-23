package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.FinanceDocumentStatus;
import com.dada.eventmanagement.common.enums.FinanceDocumentType;
import com.dada.eventmanagement.common.enums.FinancialScope;
import com.dada.eventmanagement.common.enums.OperationContext;
import com.dada.eventmanagement.common.enums.OperationGroup;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FinanceDocumentResponse(
        Long id,
        FinanceDocumentType documentType,
        FinancialScope documentScope,
        Long categoryId,
        Long contactId,
        Long eventId,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal settledAmount,
        BigDecimal remainingAmount,
        String description,
        FinanceDocumentStatus status,
        OperationGroup operationGroup,
        OperationContext operationContext,
        LocalDate lastSettlementDate,
        LocalDateTime createdAt,
        List<FinanceDocumentSettlementResponse> settlements
) {
}
