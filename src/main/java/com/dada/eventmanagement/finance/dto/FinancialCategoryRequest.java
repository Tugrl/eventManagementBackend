package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.FinancialCategoryType;
import com.dada.eventmanagement.common.enums.FinancialScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FinancialCategoryRequest(
        Long parentId,
        @NotBlank(message = "Category name is required") String name,
        @NotNull(message = "Category type is required") FinancialCategoryType categoryType,
        @NotNull(message = "Scope is required") FinancialScope scope,
        String description
) {
}
