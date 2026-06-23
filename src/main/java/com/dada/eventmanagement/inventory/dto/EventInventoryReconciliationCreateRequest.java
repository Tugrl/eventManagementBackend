package com.dada.eventmanagement.inventory.dto;

import jakarta.validation.constraints.NotNull;

public record EventInventoryReconciliationCreateRequest(
        @NotNull Long inventoryItemId
) {
}
