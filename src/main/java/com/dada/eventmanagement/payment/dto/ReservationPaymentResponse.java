package com.dada.eventmanagement.payment.dto;

import com.dada.eventmanagement.common.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationPaymentResponse(
        Long id,
        Long reservationId,
        Long eventId,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod paymentMethod,
        String bankReference,
        String description,
        Long financialTransactionId,
        LocalDateTime createdAt
) {
}
