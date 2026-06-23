package com.dada.eventmanagement.staff.controller;

import com.dada.eventmanagement.common.enums.EmployeeType;
import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.staff.dto.EmployeeRequest;
import com.dada.eventmanagement.staff.dto.EventStaffAssignmentRequest;
import com.dada.eventmanagement.staff.dto.ServicePayoutRequest;
import com.dada.eventmanagement.staff.service.StaffService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
public class StaffController {
    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @GetMapping("/employees")
    public ApiResponse<?> employees(@RequestParam(required = false) EmployeeType type) {
        return ApiResponse.ok("Employees listed", service.employees(type));
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.ok("Employee created", service.createEmployee(request));
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.ok("Employee updated", service.updateEmployee(id, request));
    }

    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteEmployee(@PathVariable Long id) {
        service.deleteEmployee(id);
        return ApiResponse.ok("Employee deleted");
    }

    @GetMapping("/service-payouts")
    public ApiResponse<?> payouts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Service payouts listed", service.payouts(startDate, endDate));
    }

    @PostMapping("/service-payouts")
    public ApiResponse<?> createPayout(@Valid @RequestBody ServicePayoutRequest request) {
        return ApiResponse.ok("Service payout created", service.createPayout(request));
    }

    @GetMapping("/events/{eventId}/assignments")
    public ApiResponse<?> eventAssignments(@PathVariable Long eventId) {
        return ApiResponse.ok("Event staff assignments listed", service.eventAssignments(eventId));
    }

    @PutMapping("/events/{eventId}/assignments")
    public ApiResponse<?> saveEventAssignments(@PathVariable Long eventId, @Valid @RequestBody EventStaffAssignmentRequest request) {
        return ApiResponse.ok("Event staff assignments saved", service.saveEventAssignments(eventId, request));
    }

    @PutMapping("/service-payouts/{id}")
    public ApiResponse<?> updatePayout(@PathVariable Long id, @Valid @RequestBody ServicePayoutRequest request) {
        return ApiResponse.ok("Service payout updated", service.updatePayout(id, request));
    }

    @DeleteMapping("/service-payouts/{id}")
    public ApiResponse<?> deletePayout(@PathVariable Long id) {
        service.deletePayout(id);
        return ApiResponse.ok("Service payout deleted");
    }
}
