package com.dada.eventmanagement.event.dto;

import com.dada.eventmanagement.common.enums.EventStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EventSummaryResponse(
        Long eventId,
        String eventTitle,
        LocalDate eventDate,
        String venueName,
        EventStatus status,
        Integer maxCapacity,
        Integer expectedGuestCount,
        BigDecimal finalTicketPrice,
        BigDecimal suggestedTicketPrice,
        Boolean teamConfigured,
        Integer teamAssignedCount,
        Integer fixedTeamCount,
        Integer extraTeamCount,
        Boolean costFinalized,
        Boolean consumptionFinalized,
        Boolean pricingFinalized,
        Integer setupCompletionRate,
        Boolean reservationReady,
        Long totalReservationCount,
        Long activeReservationCount,
        Long confirmedGuestCount,
        Long pendingDepositCount,
        Long paidDepositCount,
        BigDecimal totalDepositAmount,
        BigDecimal estimatedTicketRevenue,
        BigDecimal estimatedTotalCost,
        BigDecimal estimatedProfit,
        BigDecimal estimatedProfitMargin,
        BigDecimal totalConsumptionPlanCost,
        List<CategoryCostBreakdown> categoryBreakdown
) {
    public record CategoryCostBreakdown(String categoryName, BigDecimal totalCost) {
    }
}
