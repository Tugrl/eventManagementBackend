package com.dada.eventmanagement.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceOverviewReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netTotal,
        BigDecimal cashDifference
) {
}
