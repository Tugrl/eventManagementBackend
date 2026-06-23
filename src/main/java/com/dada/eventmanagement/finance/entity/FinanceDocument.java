package com.dada.eventmanagement.finance.entity;

import com.dada.eventmanagement.common.enums.FinanceDocumentStatus;
import com.dada.eventmanagement.common.enums.FinanceDocumentType;
import com.dada.eventmanagement.common.enums.FinancialScope;
import com.dada.eventmanagement.common.enums.OperationContext;
import com.dada.eventmanagement.common.enums.OperationGroup;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "finance_documents")
public class FinanceDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinanceDocumentType documentType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialScope documentScope = FinancialScope.COMPANY;
    @Column(nullable = false)
    private Long categoryId;
    private Long contactId;
    private Long eventId;
    @Column(nullable = false)
    private LocalDate issueDate;
    private LocalDate dueDate;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinanceDocumentStatus status = FinanceDocumentStatus.OPEN;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationGroup operationGroup = OperationGroup.OTHER;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationContext operationContext = OperationContext.COMPANY;
    private Long createdByUserId;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
