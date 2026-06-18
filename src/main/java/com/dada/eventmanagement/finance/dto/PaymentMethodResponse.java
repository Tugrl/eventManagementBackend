package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.PaymentMethodType;

public record PaymentMethodResponse(
        Long id,
        String name,
        PaymentMethodType methodType,
        Boolean isDefault
) {
}
