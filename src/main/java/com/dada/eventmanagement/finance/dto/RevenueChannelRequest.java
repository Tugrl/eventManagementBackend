package com.dada.eventmanagement.finance.dto;

import com.dada.eventmanagement.common.enums.RevenueChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RevenueChannelRequest(
        @NotBlank(message = "Revenue channel name is required") String name,
        @NotNull(message = "Revenue channel type is required") RevenueChannelType channelType,
        Integer sortOrder
) {
}
