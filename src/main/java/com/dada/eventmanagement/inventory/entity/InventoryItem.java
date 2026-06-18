package com.dada.eventmanagement.inventory.entity;

import com.dada.eventmanagement.common.enums.InventoryUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private String name;
    private String category;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryUnit unit;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal minimumQuantity = BigDecimal.ZERO;
    @Column(precision = 19, scale = 4)
    private BigDecimal bottleVolumeMl;
    @Column(nullable = false)
    private Boolean isActive = true;
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
