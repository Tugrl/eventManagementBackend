package com.dada.eventmanagement.cost.dto;

import com.dada.eventmanagement.common.enums.CostPaymentStatus;
import com.dada.eventmanagement.common.enums.CostVerificationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EventCostRealizationRequest(
        @NotNull CostVerificationStatus verificationStatus,
        @NotNull CostPaymentStatus paymentStatus,
        @NotNull @DecimalMin("0.0") BigDecimal actualUnitCost,
        @NotNull @DecimalMin("0.0") BigDecimal actualQuantity,
        Long accountId,
        Long paymentMethodId,
        Long financialCategoryId,
        Long contactId,
        LocalDate transactionDate,
        String notes
) {
}
