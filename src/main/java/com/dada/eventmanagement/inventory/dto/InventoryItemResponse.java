package com.dada.eventmanagement.inventory.dto;

import com.dada.eventmanagement.common.enums.InventoryUnit;
import java.math.BigDecimal;

public record InventoryItemResponse(
        Long id,
        String name,
        String category,
        InventoryUnit unit,
        BigDecimal unitCost,
        BigDecimal currentQuantity,
        BigDecimal minimumQuantity,
        BigDecimal bottleVolumeMl,
        boolean belowMinimum
) {
}
