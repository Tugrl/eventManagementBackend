package com.dada.eventmanagement.reservation.dto;

import com.dada.eventmanagement.common.enums.DepositStatus;
import com.dada.eventmanagement.common.enums.ReservationChannel;
import com.dada.eventmanagement.common.enums.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long eventId,
        String eventTitle,
        java.time.LocalDate eventDate,
        String reservationCode,
        String customerName,
        String customerPhone,
        String customerEmail,
        Integer guestCount,
        ReservationChannel reservationChannel,
        String tableNumber,
        BigDecimal depositAmount,
        BigDecimal finalTicketPrice,
        BigDecimal totalTicketAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        DepositStatus depositStatus,
        ReservationStatus reservationStatus,
        String notes,
        LocalDateTime createdAt
) {
}
