package com.dada.eventmanagement.finance.entity;

import com.dada.eventmanagement.common.enums.FinancialTransactionStatus;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_transactions")
public class FinancialTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    private Long eventId;
    @Column(nullable = false)
    private Long accountId;
    private Long paymentMethodId;
    @Column(nullable = false)
    private Long categoryId;
    private Long revenueChannelId;
    private Long contactId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialTransactionType transactionType;
    @Column(nullable = false)
    private LocalDate transactionDate;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
    private Integer guestCount;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialTransactionStatus status = FinancialTransactionStatus.ACTIVE;
    @Column(columnDefinition = "TEXT")
    private String voidReason;
    private Long createdByUserId;
    private Long voidedByUserId;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    private LocalDateTime voidedAt;

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
