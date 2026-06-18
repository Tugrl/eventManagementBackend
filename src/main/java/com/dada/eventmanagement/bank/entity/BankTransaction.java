package com.dada.eventmanagement.bank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_transactions")
public class BankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private LocalDate transactionDate;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    private String senderName;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String iban;
    private String referenceCode;
    private Long matchedReservationId;
    @Column(nullable = false)
    private Boolean isMatched = false;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
