package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.RevenueChannelType;

public record RevenueChannelResponse(
        Long id,
        String name,
        RevenueChannelType channelType,
        Integer sortOrder,
        Boolean isDefault
) {
}
