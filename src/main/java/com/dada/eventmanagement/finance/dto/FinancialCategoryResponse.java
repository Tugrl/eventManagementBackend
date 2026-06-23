package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.FinancialCategoryType;
import com.dada.eventmanagement.common.enums.FinancialScope;
import com.dada.eventmanagement.common.enums.OperationGroup;

public record FinancialCategoryResponse(
        Long id,
        Long parentId,
        String name,
        FinancialCategoryType categoryType,
        FinancialScope scope,
        OperationGroup operationGroup,
        String description,
        Boolean isDefault
) {
}
