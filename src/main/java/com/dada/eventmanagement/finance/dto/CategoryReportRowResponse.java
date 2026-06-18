package com.dada.eventmanagement.finance.dto;

import java.math.BigDecimal;

public record CategoryReportRowResponse(
        Long categoryId,
        String categoryName,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netTotal
) {
}
