package com.dada.eventmanagement.pricing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ticket_price_suggestions")
public class TicketPriceSuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private Long eventId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalEstimatedCost;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal targetProfitMargin;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal targetRevenue;
    @Column(nullable = false)
    private Integer expectedGuestCount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal suggestedTicketPrice;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
