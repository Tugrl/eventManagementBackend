package com.dada.eventmanagement.auth.dto;

import com.dada.eventmanagement.common.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UserUpdateRequest(
        @NotNull(message = "Role is required") Role role,
        @NotNull(message = "Active flag is required") Boolean isActive
) {
}
