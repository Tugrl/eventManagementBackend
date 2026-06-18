package com.dada.eventmanagement.common.enums;

import java.math.BigDecimal;

public enum ContactMovementType {
    RECEIVABLE,
    COLLECTION,
    PAYABLE,
    PAYMENT,
    ADJUSTMENT;

    public BigDecimal balanceDelta(BigDecimal amount) {
        return switch (this) {
            case RECEIVABLE, PAYMENT -> amount;
            case COLLECTION, PAYABLE -> amount.negate();
            case ADJUSTMENT -> amount;
        };
    }
}
