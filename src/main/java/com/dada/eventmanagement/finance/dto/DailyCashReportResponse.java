package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.DailyCashReportStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCashReportResponse(
        Long id,
        LocalDate reportDate,
        BigDecimal openingCash,
        BigDecimal managementCashIn,
        BigDecimal cashIncome,
        BigDecimal cardIncome,
        BigDecimal bankIncome,
        BigDecimal currentAccountIncome,
        BigDecimal cashExpense,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal expectedCash,
        BigDecimal actualCash,
        BigDecimal cashDifference,
        BigDecimal managementCashOut,
        String notes,
        DailyCashReportStatus status
) {
}
