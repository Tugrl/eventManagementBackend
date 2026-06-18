package com.dada.eventmanagement.finance.repository;

import com.dada.eventmanagement.common.enums.FinancialCategoryType;
import com.dada.eventmanagement.finance.entity.FinancialCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory, Long> {
    List<FinancialCategory> findByCompanyIdAndIsActiveTrueOrderByNameAsc(Long companyId);
    List<FinancialCategory> findByCompanyIdAndCategoryTypeAndIsActiveTrueOrderByNameAsc(Long companyId, FinancialCategoryType categoryType);
    Optional<FinancialCategory> findByIdAndCompanyIdAndIsActiveTrue(Long id, Long companyId);
}
