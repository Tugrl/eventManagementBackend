package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.FinancialCategoryType;
import com.dada.eventmanagement.common.enums.FinancialScope;

public record FinancialCategoryResponse(
        Long id,
        Long parentId,
        String name,
        FinancialCategoryType categoryType,
        FinancialScope scope,
        String description,
        Boolean isDefault
) {
}
