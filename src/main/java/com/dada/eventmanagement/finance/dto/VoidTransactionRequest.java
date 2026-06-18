package com.dada.eventmanagement.finance.dto;

import jakarta.validation.constraints.NotBlank;

public record VoidTransactionRequest(
        @NotBlank(message = "Void reason is required") String reason
) {
}
