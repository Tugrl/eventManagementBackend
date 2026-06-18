package com.dada.eventmanagement.event.dto;

import com.dada.eventmanagement.common.enums.EventRevenueModel;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventUpsertRequest(
        @NotBlank(message = "Event title is required") String title,
        String description,
        @NotNull(message = "Event date is required") LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        @NotBlank(message = "Venue name is required") String venueName,
        @NotNull(message = "Max capacity is required") @Positive Integer maxCapacity,
        @NotNull(message = "Expected guest count is required") @Positive Integer expectedGuestCount,
        @NotNull(message = "Target profit margin is required") @DecimalMin(value = "0.0", inclusive = true) BigDecimal targetProfitMargin,
        @NotNull(message = "Revenue model is required") EventRevenueModel revenueModel,
        Long primaryContactId,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal agreedRevenue,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal ticketPrice,
        @Min(0) Integer targetTicketCount,
        @Min(0) Integer complimentaryGuestCount,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal sponsorRevenueTarget,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal targetRevenueAmount,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal targetProfitAmount,
        @NotNull(message = "Deposit required flag is required") Boolean depositRequired,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal minimumDepositAmount
) {
}
