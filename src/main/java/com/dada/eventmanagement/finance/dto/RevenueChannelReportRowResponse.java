package com.dada.eventmanagement.finance.dto;

import java.math.BigDecimal;

public record RevenueChannelReportRowResponse(
        Long revenueChannelId,
        String revenueChannelName,
        BigDecimal amount,
        Integer guestCount
) {
}
