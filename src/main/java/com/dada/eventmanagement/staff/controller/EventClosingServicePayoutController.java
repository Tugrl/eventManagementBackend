package com.dada.eventmanagement.staff.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.staff.dto.ClosingServicePayoutPaymentRequest;
import com.dada.eventmanagement.staff.dto.ClosingServicePayoutRequest;
import com.dada.eventmanagement.staff.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/closing-service-payout")
public class EventClosingServicePayoutController {
    private final StaffService service;

    public EventClosingServicePayoutController(StaffService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<?> get(@PathVariable Long eventId) {
        return ApiResponse.ok("Closing service payout loaded", service.closingServicePayout(eventId));
    }

    @PutMapping
    public ApiResponse<?> save(@PathVariable Long eventId, @Valid @RequestBody ClosingServicePayoutRequest request) {
        return ApiResponse.ok("Closing service payout saved", service.saveClosingServicePayout(eventId, request));
    }

    @PostMapping("/finalize")
    public ApiResponse<?> finalizePayout(@PathVariable Long eventId) {
        return ApiResponse.ok("Closing service payout finalized", service.finalizeClosingServicePayout(eventId));
    }

    @PostMapping("/pay")
    public ApiResponse<?> pay(@PathVariable Long eventId, @Valid @RequestBody ClosingServicePayoutPaymentRequest request) {
        return ApiResponse.ok("Closing service payout paid", service.payClosingServicePayout(eventId, request));
    }
}
