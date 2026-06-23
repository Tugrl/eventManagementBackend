package com.dada.eventmanagement.cost.dto;

import com.dada.eventmanagement.common.enums.CostPaymentStatus;
import com.dada.eventmanagement.common.enums.CostVerificationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EventCostRealizationResponse(
        Long id,
        Long eventId,
        Long eventCostId,
        Long inventoryItemId,
        Long inventoryReconciliationId,
        Long costCategoryId,
        String name,
        BigDecimal estimatedUnitCost,
        BigDecimal estimatedQuantity,
        BigDecimal estimatedTotalCost,
        BigDecimal actualUnitCost,
        BigDecimal actualQuantity,
        BigDecimal actualTotalCost,
        CostVerificationStatus verificationStatus,
        CostPaymentStatus paymentStatus,
        Long accountId,
        Long paymentMethodId,
        Long financialCategoryId,
        Long contactId,
        LocalDate transactionDate,
        Long financialTransactionId,
        String notes,
        Boolean finalized
) {
}
