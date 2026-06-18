package com.dada.eventmanagement.inventory.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.inventory.dto.EventInventoryUsageRequest;
import com.dada.eventmanagement.inventory.dto.InventoryItemRequest;
import com.dada.eventmanagement.inventory.dto.InventoryMovementRequest;
import com.dada.eventmanagement.inventory.service.InventoryService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/items")
    public ApiResponse<?> items() {
        return ApiResponse.ok("Inventory items listed", service.items());
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> createItem(@Valid @RequestBody InventoryItemRequest request) {
        return ApiResponse.ok("Inventory item created", service.createItem(request));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> updateItem(@PathVariable Long id, @Valid @RequestBody InventoryItemRequest request) {
        return ApiResponse.ok("Inventory item updated", service.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteItem(@PathVariable Long id) {
        service.deleteItem(id);
        return ApiResponse.ok("Inventory item deleted");
    }

    @GetMapping("/movements")
    public ApiResponse<?> movements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Inventory movements listed", service.movements(startDate, endDate));
    }

    @PostMapping("/movements")
    public ApiResponse<?> createMovement(@Valid @RequestBody InventoryMovementRequest request) {
        return ApiResponse.ok("Inventory movement created", service.createMovement(request));
    }

    @PostMapping("/event-usages")
    public ApiResponse<?> createUsage(@Valid @RequestBody EventInventoryUsageRequest request) {
        return ApiResponse.ok("Event inventory usage created", service.createUsage(request));
    }

    @GetMapping("/events/{eventId}/usages")
    public ApiResponse<?> eventUsages(@PathVariable Long eventId) {
        return ApiResponse.ok("Event inventory usages listed", service.eventUsages(eventId));
    }

    @GetMapping("/reports/usages")
    public ApiResponse<?> usageReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Inventory usage report calculated", service.usageReport(startDate, endDate));
    }
}
