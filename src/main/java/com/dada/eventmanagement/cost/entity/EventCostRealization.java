package com.dada.eventmanagement.cost.entity;

import com.dada.eventmanagement.common.enums.CostPaymentStatus;
import com.dada.eventmanagement.common.enums.CostVerificationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "event_cost_realizations")
public class EventCostRealization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private Long eventId;
    private Long eventCostId;
    private Long inventoryItemId;
    private Long inventoryReconciliationId;
    @Column(nullable = false)
    private Long costCategoryId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedUnitCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedQuantity = BigDecimal.ONE;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedTotalCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal actualUnitCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal actualQuantity = BigDecimal.ONE;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal actualTotalCost = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CostVerificationStatus verificationStatus = CostVerificationStatus.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CostPaymentStatus paymentStatus = CostPaymentStatus.UNPAID;
    private Long accountId;
    private Long paymentMethodId;
    private Long financialCategoryId;
    private Long contactId;
    private LocalDate transactionDate;
    private Long financialTransactionId;
    private Long contactMovementId;
    private Long financeDocumentId;
    private Long settlementId;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(nullable = false)
    private Boolean finalized = false;
    private Long verifiedByUserId;
    private LocalDateTime verifiedAt;
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
