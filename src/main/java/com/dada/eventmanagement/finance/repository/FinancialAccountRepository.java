package com.dada.eventmanagement.finance.repository;

import com.dada.eventmanagement.common.enums.AccountType;
import com.dada.eventmanagement.finance.entity.FinancialAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {
    List<FinancialAccount> findByCompanyIdAndIsActiveTrueOrderByNameAsc(Long companyId);
    Optional<FinancialAccount> findByIdAndCompanyIdAndIsActiveTrue(Long id, Long companyId);
    Optional<FinancialAccount> findFirstByCompanyIdAndAccountTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(Long companyId, AccountType accountType);
    List<FinancialAccount> findByCompanyIdAndIsDefaultTrueAndIsActiveTrue(Long companyId);
}
