package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FinancialAccountRequest(
        @NotBlank String name,
        @NotNull AccountType accountType,
        @NotNull @DecimalMin("0.00") BigDecimal openingBalance,
        Boolean isDefault
) {
}
