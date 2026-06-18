package com.dada.eventmanagement.auth.controller;

import com.dada.eventmanagement.auth.dto.LoginRequest;
import com.dada.eventmanagement.auth.dto.LoginResponse;
import com.dada.eventmanagement.auth.dto.OperationsReportPreferencesRequest;
import com.dada.eventmanagement.auth.dto.RegisterRequest;
import com.dada.eventmanagement.auth.security.AuthCookieService;
import com.dada.eventmanagement.auth.service.AuthService;
import com.dada.eventmanagement.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Register successful", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse login = authService.login(request);
        authCookieService.addAuthCookie(response, login.token());
        return ApiResponse.ok("Login successful", new LoginResponse(null, login.user()));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(HttpServletResponse response) {
        authCookieService.clearAuthCookie(response);
        return ApiResponse.ok("Logout successful", null);
    }

    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.ok("User profile", authService.me());
    }

    @GetMapping("/me/operations-report-preferences")
    public ApiResponse<?> operationsReportPreferences() {
        return ApiResponse.ok("Operations report preferences", authService.operationsReportPreferences());
    }

    @PutMapping("/me/operations-report-preferences")
    public ApiResponse<?> updateOperationsReportPreferences(@RequestBody OperationsReportPreferencesRequest request) {
        return ApiResponse.ok("Operations report preferences updated", authService.updateOperationsReportPreferences(request));
    }
}
