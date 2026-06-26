package com.dada.eventmanagement.staff.dto;

import java.math.BigDecimal;

public record ServicePayoutItemResponse(
        Long employeeId,
        String employeeName,
        BigDecimal points,
        BigDecimal payoutAmount,
        Long contactId,
        String contactName,
        Long contactMovementId,
        Long financeDocumentId,
        Boolean documentCreated,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String documentStatus,
        Long paymentSettlementId,
        Long paymentTransactionId
) {
}
