package com.dada.eventmanagement.cost.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.cost.dto.CostCategoryRequest;
import com.dada.eventmanagement.cost.service.CostCategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cost-categories")
public class CostCategoryController {
    private final CostCategoryService service;

    public CostCategoryController(CostCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok("Cost categories listed", service.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> create(@Valid @RequestBody CostCategoryRequest request) {
        return ApiResponse.ok("Cost category created", service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody CostCategoryRequest request) {
        return ApiResponse.ok("Cost category updated", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Cost category deleted");
    }
}
