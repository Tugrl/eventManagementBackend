package com.dada.eventmanagement.inventory.entity;

import com.dada.eventmanagement.common.enums.InventoryReconciliationStatus;
import com.dada.eventmanagement.common.enums.InventoryUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "event_inventory_reconciliations")
public class EventInventoryReconciliation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private Long eventId;
    @Column(nullable = false)
    private Long inventoryItemId;
    private Long consumptionPlanId;
    @Column(nullable = false)
    private String itemName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryUnit stockUnit;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryUnit inputUnit;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal plannedQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal actualConsumptionInput = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal manualWasteInput = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal actualConsumptionStock = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalWasteStock = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal automaticWasteMl = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal deductedQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal previouslyProcessedQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;
    @Column(nullable = false)
    private Integer actualGuestCount = 1;
    @Column(nullable = false)
    private LocalDate usageDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryReconciliationStatus status = InventoryReconciliationStatus.PENDING;
    @Column(nullable = false)
    private Boolean finalized = false;
    private Long inventoryMovementId;
    @Column(columnDefinition = "TEXT")
    private String notes;
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
