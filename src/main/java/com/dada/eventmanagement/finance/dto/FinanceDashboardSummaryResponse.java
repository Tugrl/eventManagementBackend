package com.dada.eventmanagement.finance.dto;

import java.math.BigDecimal;

public record FinanceDashboardSummaryResponse(
        BigDecimal paidIncome,
        BigDecimal paidExpense,
        BigDecimal openReceivables,
        BigDecimal openPayables,
        BigDecimal cashBalance,
        BigDecimal bankBalance,
        BigDecimal cardBalance
) {
}
