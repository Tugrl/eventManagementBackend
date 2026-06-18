package com.dada.eventmanagement.event.entity;

import com.dada.eventmanagement.common.enums.EventStatus;
import com.dada.eventmanagement.common.enums.EventRevenueModel;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate eventDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Column(nullable = false)
    private String venueName;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private Integer expectedGuestCount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal targetProfitMargin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventRevenueModel revenueModel = EventRevenueModel.CLOSED_ORGANIZATION;

    private Long primaryContactId;

    @Column(precision = 19, scale = 2)
    private BigDecimal agreedRevenue;

    @Column(precision = 19, scale = 2)
    private BigDecimal ticketPrice;

    private Integer targetTicketCount;

    @Column(nullable = false)
    private Integer complimentaryGuestCount = 0;

    @Column(precision = 19, scale = 2)
    private BigDecimal sponsorRevenueTarget;

    @Column(precision = 19, scale = 2)
    private BigDecimal targetRevenueAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal targetProfitAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal suggestedTicketPrice;

    @Column(precision = 19, scale = 2)
    private BigDecimal finalTicketPrice;

    @Column(nullable = false)
    private Boolean depositRequired;

    @Column(precision = 19, scale = 2)
    private BigDecimal minimumDepositAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.PLANNING;

    @Column(nullable = false)
    private Boolean costFinalized = false;

    @Column(nullable = false)
    private Boolean consumptionFinalized = false;

    @Column(nullable = false)
    private Boolean pricingFinalized = false;

    @Column(nullable = false)
    private Boolean isDeleted = false;

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
