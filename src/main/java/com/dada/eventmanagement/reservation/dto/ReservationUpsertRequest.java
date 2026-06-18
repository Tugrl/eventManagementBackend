package com.dada.eventmanagement.reservation.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ReservationUpsertRequest(
        @NotBlank(message = "Customer name is required") String customerName,
        @NotBlank(message = "Customer phone is required") String customerPhone,
        @Email(message = "Customer email is invalid") String customerEmail,
        @NotNull(message = "Guest count is required") @Positive Integer guestCount,
        String tableNumber,
        @NotNull(message = "Deposit amount is required") @DecimalMin(value = "0.0", inclusive = true) BigDecimal depositAmount,
        String notes
) {
}
