package com.dada.eventmanagement.consumption.entity;

import com.dada.eventmanagement.common.enums.InventoryUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "event_consumption_plans")
public class EventConsumptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private Long eventId;
    private Long inventoryItemId;
    @Column(nullable = false)
    private String productName;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal estimatedConsumptionPerPerson;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryUnit inputUnit;
    @Column(nullable = false)
    private Integer expectedGuestCount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal wastePercentage = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal convertedUnitQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal requiredQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;
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
