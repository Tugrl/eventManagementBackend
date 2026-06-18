package com.dada.eventmanagement.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EventProfitReportRowResponse(
        Long eventId,
        String eventTitle,
        LocalDate eventDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netProfit
) {
}
