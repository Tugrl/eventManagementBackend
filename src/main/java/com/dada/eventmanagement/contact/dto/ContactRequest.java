package com.dada.eventmanagement.contact.dto;

import com.dada.eventmanagement.common.enums.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ContactRequest(
        @NotBlank(message = "Contact name is required") String name,
        @NotNull(message = "Contact type is required") ContactType contactType,
        String phone,
        String email,
        String taxNumber,
        String notes,
        BigDecimal openingBalance
) {
}
