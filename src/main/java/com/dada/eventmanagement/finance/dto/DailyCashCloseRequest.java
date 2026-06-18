package com.dada.eventmanagement.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCashCloseRequest(
        @NotNull(message = "Report date is required") LocalDate reportDate,
        @NotNull(message = "Opening cash is required") BigDecimal openingCash,
        BigDecimal managementCashIn,
        @NotNull(message = "Actual cash is required") BigDecimal actualCash,
        BigDecimal managementCashOut,
        String notes
) {
}
