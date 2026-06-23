package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.FinanceDocumentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceDocumentRequest(
        @NotNull(message = "Document type is required") FinanceDocumentType documentType,
        @NotNull(message = "Category is required") Long categoryId,
        Long contactId,
        Long eventId,
        @NotNull(message = "Issue date is required") LocalDate issueDate,
        LocalDate dueDate,
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
        String description
) {
}
