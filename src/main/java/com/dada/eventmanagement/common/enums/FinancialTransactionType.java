package com.dada.eventmanagement.common.enums;

public enum FinancialTransactionType {
    INCOME,
    EXPENSE;

    public FinancialCategoryType categoryType() {
        return this == INCOME ? FinancialCategoryType.INCOME : FinancialCategoryType.EXPENSE;
    }

    public FinancialTransactionType reverse() {
        return this == INCOME ? EXPENSE : INCOME;
    }
}
