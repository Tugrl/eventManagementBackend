package com.dada.eventmanagement.inventory.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.inventory.dto.EventInventoryReconciliationCreateRequest;
import com.dada.eventmanagement.inventory.dto.EventInventoryReconciliationRequest;
import com.dada.eventmanagement.inventory.service.EventInventoryReconciliationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/closing-inventory")
public class EventInventoryReconciliationController {
    private final EventInventoryReconciliationService service;

    public EventInventoryReconciliationController(EventInventoryReconciliationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<?> list(@PathVariable Long eventId) {
        return ApiResponse.ok("Closing inventory listed", service.list(eventId));
    }

    @PostMapping
    public ApiResponse<?> create(@PathVariable Long eventId, @Valid @RequestBody EventInventoryReconciliationCreateRequest request) {
        return ApiResponse.ok("Closing inventory item created", service.create(eventId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long eventId, @PathVariable Long id, @Valid @RequestBody EventInventoryReconciliationRequest request) {
        return ApiResponse.ok("Closing inventory item updated", service.update(eventId, id, request));
    }

    @PostMapping("/finalize")
    public ApiResponse<?> finalizeInventory(@PathVariable Long eventId) {
        return ApiResponse.ok("Closing inventory finalized", service.finalizeInventory(eventId));
    }
}
