package com.dada.eventmanagement.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        Long totalEvents,
        Long upcomingEvents,
        Long completedEvents,
        Long totalReservations,
        Long totalPaidDeposits,
        BigDecimal totalEstimatedRevenue,
        BigDecimal totalEstimatedCost,
        BigDecimal totalEstimatedProfit,
        List<NearestEventDto> nearestEvents
) {
    public record NearestEventDto(Long id, String title, LocalDate eventDate, String venueName) {
    }
}
