package com.dada.eventmanagement.staff.entity;

import com.dada.eventmanagement.common.enums.EmployeeType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long companyId;
    @Column(nullable = false)
    private String fullName;
    private Long contactId;
    private String roleName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeType employeeType;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal defaultDailyRate = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal servicePoint = BigDecimal.ZERO;
    private String phone;
    @Column(columnDefinition = "TEXT")
    private String notes;
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
