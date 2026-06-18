package com.dada.eventmanagement.staff.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "service_payout_items")
public class ServicePayoutItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private Long payoutId;
    @Column(nullable = false)
    private Long employeeId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal points = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal payoutAmount = BigDecimal.ZERO;
}
