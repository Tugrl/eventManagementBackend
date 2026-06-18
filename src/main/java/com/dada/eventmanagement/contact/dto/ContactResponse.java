package com.dada.eventmanagement.contact.dto;

import com.dada.eventmanagement.common.enums.ContactType;
import java.math.BigDecimal;

public record ContactResponse(
        Long id,
        String name,
        ContactType contactType,
        String phone,
        String email,
        String taxNumber,
        String notes,
        BigDecimal openingBalance,
        BigDecimal currentBalance
) {
}
