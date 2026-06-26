package com.dada.eventmanagement.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EventProfitReportRowResponse(
        Long eventId,
        String eventTitle,
        LocalDate eventDate,
        String eventStartTime,
        String eventEndTime,
        String venueName,
        Integer expectedGuestCount,
        Integer reservationCount,
        Integer pendingDepositCount,
        BigDecimal pendingDepositAmount,
        Integer actualGuestCount,
        Integer coverCount,
        String eventStatus,
        String closingStatus,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netProfit
) {
}
