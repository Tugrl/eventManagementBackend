package com.dada.eventmanagement.event.dto;

import com.dada.eventmanagement.common.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public record EventStatusRequest(
        @NotNull(message = "Event status is required") EventStatus status
) {
}
