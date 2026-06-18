package com.dada.eventmanagement.finance.dto;

import java.math.BigDecimal;

public record EventProfitSummaryResponse(
        Long eventId,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netProfit
) {
}
