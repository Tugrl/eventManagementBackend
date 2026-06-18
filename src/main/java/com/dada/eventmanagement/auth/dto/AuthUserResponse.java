package com.dada.eventmanagement.auth.dto;

import com.dada.eventmanagement.common.enums.Role;

public record AuthUserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        Long companyId
) {
}
