package com.dada.eventmanagement.consumption.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.consumption.dto.ConsumptionPlanRequest;
import com.dada.eventmanagement.consumption.service.ConsumptionPlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class ConsumptionPlanController {
    private final ConsumptionPlanService service;

    public ConsumptionPlanController(ConsumptionPlanService service) {
        this.service = service;
    }

    @GetMapping("/api/events/{eventId}/consumption-plans")
    public ApiResponse<?> list(@PathVariable Long eventId) {
        return ApiResponse.ok("Consumption plans listed", service.list(eventId));
    }

    @PostMapping("/api/events/{eventId}/consumption-plans")
    public ApiResponse<?> create(@PathVariable Long eventId, @Valid @RequestBody ConsumptionPlanRequest request) {
        return ApiResponse.ok("Consumption plan created", service.create(eventId, request));
    }

    @PutMapping("/api/consumption-plans/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody ConsumptionPlanRequest request) {
        return ApiResponse.ok("Consumption plan updated", service.update(id, request));
    }

    @DeleteMapping("/api/consumption-plans/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Consumption plan deleted");
    }
}
