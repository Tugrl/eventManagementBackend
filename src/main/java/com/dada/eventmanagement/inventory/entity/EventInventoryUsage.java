package com.dada.eventmanagement.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "event_inventory_usages")
public class EventInventoryUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private Long eventId;
    @Column(nullable = false)
    private Long inventoryItemId;
    @Column(nullable = false)
    private LocalDate usageDate;
    @Column(nullable = false)
    private Integer actualGuestCount;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal consumptionQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal wasteQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalConsumptionCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalWasteCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal consumptionPerPerson = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal wastePerPerson = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private Long inventoryMovementId;
    private Long financialTransactionId;
    private Long createdByUserId;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
