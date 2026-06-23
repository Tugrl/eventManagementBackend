package com.dada.eventmanagement.cost.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.cost.dto.EventCostRealizationCreateRequest;
import com.dada.eventmanagement.cost.dto.EventCostRealizationRequest;
import com.dada.eventmanagement.cost.service.EventCostRealizationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/closing-costs")
public class EventCostRealizationController {
    private final EventCostRealizationService service;

    public EventCostRealizationController(EventCostRealizationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<?> list(@PathVariable Long eventId) {
        return ApiResponse.ok("Closing costs listed", service.list(eventId));
    }

    @PostMapping
    public ApiResponse<?> create(@PathVariable Long eventId, @Valid @RequestBody EventCostRealizationCreateRequest request) {
        return ApiResponse.ok("Closing cost created", service.create(eventId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long eventId, @PathVariable Long id, @Valid @RequestBody EventCostRealizationRequest request) {
        return ApiResponse.ok("Closing cost updated", service.update(eventId, id, request));
    }

    @PostMapping("/finalize")
    public ApiResponse<?> finalizeCosts(@PathVariable Long eventId) {
        return ApiResponse.ok("Closing costs finalized", service.finalizeCosts(eventId));
    }
}
