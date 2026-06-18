package com.dada.eventmanagement.staff.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "service_payouts")
public class ServicePayout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private LocalDate payoutDate;
    private Long eventId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalServiceAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPoints = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPerPoint = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private Long financialTransactionId;
    private Long createdByUserId;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
