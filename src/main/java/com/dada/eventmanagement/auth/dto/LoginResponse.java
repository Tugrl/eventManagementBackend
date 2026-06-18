package com.dada.eventmanagement.auth.dto;

public record LoginResponse(
        String token,
        AuthUserResponse user
) {
}
