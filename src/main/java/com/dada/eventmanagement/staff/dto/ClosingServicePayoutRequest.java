package com.dada.eventmanagement.staff.dto;

import com.dada.eventmanagement.common.enums.CostPaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClosingServicePayoutRequest(
        @NotNull @DecimalMin("0.00") BigDecimal totalServiceAmount,
        @NotNull LocalDate payoutDate,
        @NotNull CostPaymentStatus paymentStatus,
        Long accountId,
        Long paymentMethodId,
        Long categoryId,
        LocalDate paymentDate,
        String notes
) {
}

