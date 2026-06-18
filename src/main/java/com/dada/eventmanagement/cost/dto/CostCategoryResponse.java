package com.dada.eventmanagement.cost.dto;

public record CostCategoryResponse(
        Long id,
        String name,
        String description,
        Boolean isDefault,
        Boolean isActive
) {
}
