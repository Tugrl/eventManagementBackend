package com.dada.eventmanagement.contact.dto;

import com.dada.eventmanagement.common.enums.ContactMovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContactMovementResponse(
        Long id,
        Long contactId,
        Long financialTransactionId,
        Long documentId,
        Long settlementId,
        LocalDate movementDate,
        ContactMovementType movementType,
        BigDecimal amount,
        BigDecimal balanceDelta,
        BigDecimal remainingDocumentAmount,
        String description
) {
}
