package com.dada.eventmanagement.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "finance_document_settlements")
public class FinanceDocumentSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private Long documentId;
    private Long financialTransactionId;
    @Column(nullable = false)
    private LocalDate settlementDate;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(nullable = false)
    private Long accountId;
    private Long paymentMethodId;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private Long createdByUserId;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
