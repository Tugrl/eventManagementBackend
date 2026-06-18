package com.dada.eventmanagement.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCashSummaryResponse(
        LocalDate reportDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal cashIncome,
        BigDecimal cardIncome,
        BigDecimal bankIncome,
        BigDecimal currentAccountIncome,
        BigDecimal cashExpense,
        BigDecimal netTotal,
        Boolean closed
) {
}
