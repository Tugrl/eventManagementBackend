package com.dada.eventmanagement.cost.dto;

import jakarta.validation.constraints.NotBlank;

public record CostCategoryRequest(
        @NotBlank(message = "Category name is required") String name,
        String description
) {
}
