package com.dada.eventmanagement.contact.dto;

import com.dada.eventmanagement.common.enums.ContactType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContactResponse(
        Long id,
        String name,
        ContactType contactType,
        String phone,
        String email,
        String taxNumber,
        String notes,
        BigDecimal openingBalance,
        BigDecimal currentBalance,
        BigDecimal openReceivableAmount,
        BigDecimal openPayableAmount,
        LocalDate lastMovementDate
) {
}
