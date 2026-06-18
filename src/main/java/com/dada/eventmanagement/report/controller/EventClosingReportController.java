package com.dada.eventmanagement.report.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.report.dto.ClosingReportRequest;
import com.dada.eventmanagement.report.service.EventClosingReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class EventClosingReportController {
    private final EventClosingReportService service;

    public EventClosingReportController(EventClosingReportService service) {
        this.service = service;
    }

    @PostMapping("/api/events/{eventId}/closing-report")
    public ApiResponse<?> create(@PathVariable Long eventId, @Valid @RequestBody ClosingReportRequest request) {
        return ApiResponse.ok("Closing report created", service.create(eventId, request));
    }

    @GetMapping("/api/events/{eventId}/closing-report")
    public ApiResponse<?> get(@PathVariable Long eventId) {
        return ApiResponse.ok("Closing report detail", service.get(eventId));
    }

    @PutMapping("/api/events/{eventId}/closing-report")
    public ApiResponse<?> update(@PathVariable Long eventId, @Valid @RequestBody ClosingReportRequest request) {
        return ApiResponse.ok("Closing report updated", service.update(eventId, request));
    }
}
