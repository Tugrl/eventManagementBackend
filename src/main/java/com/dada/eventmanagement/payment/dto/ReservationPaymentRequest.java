package com.dada.eventmanagement.payment.dto;

import com.dada.eventmanagement.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationPaymentRequest(
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
        @NotNull(message = "Payment date is required") LocalDate paymentDate,
        @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,
        String bankReference,
        String description
) {
}
