package com.dada.eventmanagement.staff.dto;

import com.dada.eventmanagement.common.enums.CostPaymentStatus;
import com.dada.eventmanagement.common.enums.ServicePayoutClosingStatus;
import com.dada.eventmanagement.common.enums.ServicePayoutSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ServicePayoutResponse(
        Long id,
        LocalDate payoutDate,
        Long eventId,
        ServicePayoutSource payoutSource,
        ServicePayoutClosingStatus closingStatus,
        CostPaymentStatus paymentStatus,
        BigDecimal totalServiceAmount,
        BigDecimal totalPoints,
        BigDecimal amountPerPoint,
        Long financialTransactionId,
        Long accountId,
        Long paymentMethodId,
        Long categoryId,
        LocalDate paymentDate,
        String notes,
        List<ServicePayoutItemResponse> items
) {
}
