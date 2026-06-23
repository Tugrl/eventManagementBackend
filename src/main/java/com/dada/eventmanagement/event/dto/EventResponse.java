package com.dada.eventmanagement.event.dto;

import com.dada.eventmanagement.common.enums.EventRevenueModel;
import com.dada.eventmanagement.common.enums.EventStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String venueName,
        Integer maxCapacity,
        Integer expectedGuestCount,
        BigDecimal targetProfitMargin,
        EventRevenueModel revenueModel,
        Long primaryContactId,
        BigDecimal agreedRevenue,
        BigDecimal ticketPrice,
        Integer targetTicketCount,
        Integer complimentaryGuestCount,
        BigDecimal sponsorRevenueTarget,
        BigDecimal targetRevenueAmount,
        BigDecimal targetProfitAmount,
        BigDecimal suggestedTicketPrice,
        BigDecimal finalTicketPrice,
        Boolean depositRequired,
        BigDecimal minimumDepositAmount,
        EventStatus status,
        Boolean teamConfigured,
        Integer teamAssignedCount,
        Boolean costFinalized,
        Boolean consumptionFinalized,
        Boolean pricingFinalized,
        Integer setupCompletionRate,
        Boolean reservationReady,
        LocalDateTime createdAt
) {
}
