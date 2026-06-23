package com.dada.eventmanagement.event.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.event.dto.EventStatusRequest;
import com.dada.eventmanagement.event.dto.EventRevenueRequest;
import com.dada.eventmanagement.event.dto.EventUpsertRequest;
import com.dada.eventmanagement.event.service.EventRevenueService;
import com.dada.eventmanagement.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;
    private final EventRevenueService eventRevenueService;

    public EventController(EventService eventService, EventRevenueService eventRevenueService) {
        this.eventService = eventService;
        this.eventRevenueService = eventRevenueService;
    }

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok("Events listed", eventService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.ok("Event detail", eventService.get(id));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody EventUpsertRequest request) {
        return ApiResponse.ok("Event created", eventService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody EventUpsertRequest request) {
        return ApiResponse.ok("Event updated", eventService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateStatus(@PathVariable Long id, @Valid @RequestBody EventStatusRequest request) {
        return ApiResponse.ok("Event status updated", eventService.updateStatus(id, request.status()));
    }

    @PatchMapping("/{id}/finalize-costs")
    public ApiResponse<?> finalizeCosts(@PathVariable Long id) {
        return ApiResponse.ok("Event costs finalized", eventService.finalizeCosts(id));
    }

    @PatchMapping("/{id}/finalize-consumption")
    public ApiResponse<?> finalizeConsumption(@PathVariable Long id) {
        return ApiResponse.ok("Event consumption finalized", eventService.finalizeConsumption(id));
    }

    @PatchMapping("/{id}/finalize-pricing")
    public ApiResponse<?> finalizePricing(@PathVariable Long id) {
        return ApiResponse.ok("Event pricing finalized", eventService.finalizePricing(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ApiResponse.ok("Event deleted");
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<?> summary(@PathVariable Long id) {
        return ApiResponse.ok("Event summary", eventService.summary(id));
    }

    @GetMapping("/{id}/revenues")
    public ApiResponse<?> revenues(@PathVariable Long id) {
        return ApiResponse.ok("Event revenues listed", eventRevenueService.list(id));
    }

    @PostMapping("/{id}/revenues")
    public ApiResponse<?> createRevenue(@PathVariable Long id, @Valid @RequestBody EventRevenueRequest request) {
        return ApiResponse.ok("Event revenue created", eventRevenueService.create(id, request));
    }
}
