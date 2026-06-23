package com.dada.eventmanagement.common.enums;

public enum FinanceDocumentType {
    INCOME,
    EXPENSE;

    public FinancialTransactionType transactionType() {
        return this == INCOME ? FinancialTransactionType.INCOME : FinancialTransactionType.EXPENSE;
    }
}
