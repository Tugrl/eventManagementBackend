package com.dada.eventmanagement.finance.repository;

import com.dada.eventmanagement.common.enums.PaymentMethodType;
import com.dada.eventmanagement.finance.entity.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByCompanyIdAndIsActiveTrueOrderByNameAsc(Long companyId);
    Optional<PaymentMethod> findByIdAndCompanyIdAndIsActiveTrue(Long id, Long companyId);
    Optional<PaymentMethod> findFirstByCompanyIdAndMethodTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(Long companyId, PaymentMethodType methodType);
}
