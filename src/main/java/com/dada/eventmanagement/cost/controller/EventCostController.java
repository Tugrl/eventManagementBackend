package com.dada.eventmanagement.cost.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.cost.dto.EventCostRequest;
import com.dada.eventmanagement.cost.dto.PostEventCostToFinanceRequest;
import com.dada.eventmanagement.cost.service.EventCostService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class EventCostController {
    private final EventCostService service;

    public EventCostController(EventCostService service) {
        this.service = service;
    }

    @GetMapping("/api/events/{eventId}/costs")
    public ApiResponse<?> list(@PathVariable Long eventId) {
        return ApiResponse.ok("Event costs listed", service.list(eventId));
    }

    @PostMapping("/api/events/{eventId}/costs")
    public ApiResponse<?> create(@PathVariable Long eventId, @Valid @RequestBody EventCostRequest request) {
        return ApiResponse.ok("Event cost created", service.create(eventId, request));
    }

    @PutMapping("/api/event-costs/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody EventCostRequest request) {
        return ApiResponse.ok("Event cost updated", service.update(id, request));
    }

    @DeleteMapping("/api/event-costs/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Event cost deleted");
    }

    @PostMapping("/api/event-costs/{id}/post-to-finance")
    public ApiResponse<?> postToFinance(@PathVariable Long id, @Valid @RequestBody PostEventCostToFinanceRequest request) {
        return ApiResponse.ok("Event cost posted to finance", service.postToFinance(id, request));
    }

    @GetMapping("/api/events/{eventId}/cost-summary")
    public ApiResponse<?> summary(@PathVariable Long eventId) {
        return ApiResponse.ok("Event cost summary", service.summary(eventId));
    }
}
