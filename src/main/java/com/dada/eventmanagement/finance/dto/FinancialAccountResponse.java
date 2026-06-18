package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.AccountType;
import java.math.BigDecimal;

public record FinancialAccountResponse(
        Long id,
        String name,
        AccountType accountType,
        BigDecimal openingBalance,
        BigDecimal currentBalance,
        Boolean isDefault
) {
}
