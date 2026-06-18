package com.dada.eventmanagement.contact.entity;

import com.dada.eventmanagement.common.enums.ContactMovementType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "contact_movements")
public class ContactMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private Long contactId;
    private Long financialTransactionId;
    @Column(nullable = false)
    private LocalDate movementDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactMovementType movementType;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceDelta = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT")
    private String description;
    private Long createdByUserId;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
