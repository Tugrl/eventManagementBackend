package com.dada.eventmanagement.cost.repository;

import com.dada.eventmanagement.cost.entity.CostCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCategoryRepository extends JpaRepository<CostCategory, Long> {
    List<CostCategory> findByCompanyIdAndIsActiveTrueOrderByNameAsc(Long companyId);
    Optional<CostCategory> findByIdAndCompanyId(Long id, Long companyId);
    Optional<CostCategory> findByCompanyIdAndNameIgnoreCase(Long companyId, String name);
}
