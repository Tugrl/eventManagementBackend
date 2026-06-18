package com.dada.eventmanagement.auth.controller;

import com.dada.eventmanagement.auth.dto.RegisterRequest;
import com.dada.eventmanagement.auth.dto.UserUpdateRequest;
import com.dada.eventmanagement.auth.service.AuthService;
import com.dada.eventmanagement.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok("Users listed", authService.listUsers());
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("User created", authService.register(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok("User updated", authService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deactivate(@PathVariable Long id) {
        authService.deactivateUser(id);
        return ApiResponse.ok("User deactivated");
    }
}
