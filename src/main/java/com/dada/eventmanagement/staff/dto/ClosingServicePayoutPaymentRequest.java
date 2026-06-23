package com.dada.eventmanagement.staff.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ClosingServicePayoutPaymentRequest(
        @NotNull Long accountId,
        Long paymentMethodId,
        @NotNull Long categoryId,
        @NotNull LocalDate paymentDate
) {
}
