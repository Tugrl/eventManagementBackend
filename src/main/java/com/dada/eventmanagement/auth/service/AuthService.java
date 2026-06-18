package com.dada.eventmanagement.auth.service;

import com.dada.eventmanagement.auth.dto.*;
import com.dada.eventmanagement.auth.entity.User;
import com.dada.eventmanagement.auth.repository.UserRepository;
import com.dada.eventmanagement.auth.security.JwtService;
import com.dada.eventmanagement.auth.security.UserPrincipal;
import com.dada.eventmanagement.common.enums.Role;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.exception.UnauthorizedException;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuthUserResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailAndIsActiveTrue(normalizedEmail).isPresent()) {
            throw new BadRequestException("Email already exists");
        }
        User user = new User();
        user.setCompanyId(1L);
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setIsActive(true);
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );
        User user = userRepository.findByEmailAndIsActiveTrue(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getCompanyId(),
                user.getFullName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getIsActive()
        );
        return new LoginResponse(jwtService.generateToken(principal), toResponse(user));
    }

    public AuthUserResponse me() {
        UserPrincipal principal = SecurityUtils.currentUser();
        return new AuthUserResponse(
                principal.getId(),
                principal.getFullName(),
                principal.getEmail(),
                principal.getRole(),
                principal.getCompanyId()
        );
    }

    public OperationsReportPreferencesResponse operationsReportPreferences() {
        User user = findCompanyUser(SecurityUtils.currentUser().getId());
        if (user.getOperationsReportPreferencesJson() == null || user.getOperationsReportPreferencesJson().isBlank()) {
            return defaultOperationsReportPreferences();
        }
        try {
            return objectMapper.readValue(user.getOperationsReportPreferencesJson(), OperationsReportPreferencesResponse.class);
        } catch (JsonProcessingException ex) {
            return defaultOperationsReportPreferences();
        }
    }

    @Transactional
    public OperationsReportPreferencesResponse updateOperationsReportPreferences(OperationsReportPreferencesRequest request) {
        User user = findCompanyUser(SecurityUtils.currentUser().getId());
        OperationsReportPreferencesResponse response = new OperationsReportPreferencesResponse(
                sanitizePreferenceList(request.daily()),
                sanitizePreferenceList(request.events()),
                sanitizePreferenceList(request.totals())
        );
        try {
            user.setOperationsReportPreferencesJson(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Preferences could not be saved");
        }
        userRepository.save(user);
        return response;
    }


    public List<UserManagementResponse> listUsers() {
        Long companyId = SecurityUtils.currentCompanyId();
        return userRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::toManagementResponse)
                .toList();
    }

    @Transactional
    public UserManagementResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findCompanyUser(id);
        UserPrincipal current = SecurityUtils.currentUser();
        if (current.getId().equals(id) && Boolean.FALSE.equals(request.isActive())) {
            throw new BadRequestException("Current user cannot be deactivated");
        }
        user.setRole(request.role());
        user.setIsActive(request.isActive());
        return toManagementResponse(userRepository.save(user));
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = findCompanyUser(id);
        UserPrincipal current = SecurityUtils.currentUser();
        if (current.getId().equals(id)) {
            throw new BadRequestException("Current user cannot be deactivated");
        }
        user.setIsActive(false);
        userRepository.save(user);
    }

    private User findCompanyUser(Long id) {
        Long companyId = SecurityUtils.currentCompanyId();
        return userRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserManagementResponse toManagementResponse(User user) {
        return new UserManagementResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCompanyId(),
                user.getIsActive()
        );
    }
    private AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCompanyId()
        );
    }

    private OperationsReportPreferencesResponse defaultOperationsReportPreferences() {
        return new OperationsReportPreferencesResponse(
                List.of("daily_summary", "daily_rows", "contact_moves", "daily_extras"),
                List.of("event_summary", "event_rows"),
                List.of("totals_summary", "totals_rows", "category_distribution")
        );
    }

    private List<String> sanitizePreferenceList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(v -> v != null && !v.isBlank()).distinct().toList();
    }
}


